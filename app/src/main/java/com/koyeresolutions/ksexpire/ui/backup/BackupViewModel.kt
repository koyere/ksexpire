package com.koyeresolutions.ksexpire.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.backup.BackupManager
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para backup y restauración
 */
class BackupViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository
    private val backupManager = BackupManager(application)

    // Estados del UI
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _backupResult = MutableLiveData<BackupOperationResult?>()
    val backupResult: LiveData<BackupOperationResult?> = _backupResult

    private val _restoreResult = MutableLiveData<BackupOperationResult?>()
    val restoreResult: LiveData<BackupOperationResult?> = _restoreResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _backupInfo = MutableStateFlow(BackupInfo())
    val backupInfo: StateFlow<BackupInfo> = _backupInfo.asStateFlow()

    /**
     * Cargar información para backup
     */
    fun loadBackupInfo() {
        viewModelScope.launch {
            try {
                repository.getAllActiveItems().collect { items ->
                    val subscriptionsCount = items.count { it.isSubscription() }
                    val warrantiesCount = items.count { it.isWarranty() }
                    val imagesCount = items.count { !it.imagePath.isNullOrBlank() }
                    
                    // Estimar tamaño (aproximado)
                    val estimatedSize = (items.size * 1024L) + (imagesCount * 500 * 1024L) // ~500KB por imagen

                    _backupInfo.value = BackupInfo(
                        totalItems = items.size,
                        subscriptionsCount = subscriptionsCount,
                        warrantiesCount = warrantiesCount,
                        imagesCount = imagesCount,
                        estimatedSize = estimatedSize
                    )
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar información: ${e.message}"
            }
        }
    }

    /**
     * Crear backup
     */
    fun createBackup(outputUri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Obtener todos los ítems para backup
                val items = repository.getAllItemsForBackup()

                // Crear backup
                val result = backupManager.createBackup(items, outputUri)

                result.fold(
                    onSuccess = { backupResult ->
                        _backupResult.value = BackupOperationResult(
                            success = true,
                            itemsCount = backupResult.itemsBackedUp,
                            imagesCount = backupResult.imagesBackedUp,
                            fileSize = backupResult.backupSize,
                            message = backupResult.message
                        )
                    },
                    onFailure = { exception ->
                        _backupResult.value = BackupOperationResult(
                            success = false,
                            message = exception.message ?: "Error desconocido al crear backup"
                        )
                    }
                )

            } catch (e: Exception) {
                _backupResult.value = BackupOperationResult(
                    success = false,
                    message = "Error al crear backup: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Validar y restaurar backup
     */
    fun validateAndRestoreBackup(inputUri: Uri) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                // Primero validar el archivo
                val validationResult = backupManager.validateBackupFile(inputUri)

                validationResult.fold(
                    onSuccess = { validation ->
                        if (validation.isValid) {
                            // Archivo válido, proceder con restauración
                            performRestore(inputUri)
                        } else {
                            _errorMessage.value = "Archivo de backup inválido o corrupto"
                            _isLoading.value = false
                        }
                    },
                    onFailure = { exception ->
                        _errorMessage.value = "Error al validar backup: ${exception.message}"
                        _isLoading.value = false
                    }
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error al procesar backup: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    /**
     * Realizar restauración
     */
    private suspend fun performRestore(inputUri: Uri) {
        try {
            // Restaurar backup
            val result = backupManager.restoreBackup(inputUri)

            result.fold(
                onSuccess = { restoreResult ->
                    // Restaurar ítems en la base de datos
                    val repositoryResult = repository.restoreItemsFromBackup(
                        restoreResult.items,
                        clearExisting = true
                    )

                    repositoryResult.fold(
                        onSuccess = { restoredCount ->
                            _restoreResult.value = BackupOperationResult(
                                success = true,
                                itemsCount = restoredCount,
                                imagesCount = restoreResult.imagesRestored,
                                backupDate = restoreResult.backupDate,
                                message = restoreResult.message
                            )
                        },
                        onFailure = { exception ->
                            _restoreResult.value = BackupOperationResult(
                                success = false,
                                message = "Error al restaurar en base de datos: ${exception.message}"
                            )
                        }
                    )
                },
                onFailure = { exception ->
                    _restoreResult.value = BackupOperationResult(
                        success = false,
                        message = exception.message ?: "Error desconocido al restaurar backup"
                    )
                }
            )

        } catch (e: Exception) {
            _restoreResult.value = BackupOperationResult(
                success = false,
                message = "Error al restaurar backup: ${e.message}"
            )
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Generar nombre de archivo de backup
     */
    fun generateBackupFileName(): String {
        return backupManager.generateBackupFileName()
    }

    /**
     * Limpiar resultado de backup
     */
    fun clearBackupResult() {
        _backupResult.value = null
    }

    /**
     * Limpiar resultado de restauración
     */
    fun clearRestoreResult() {
        _restoreResult.value = null
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }

    /**
     * Información de backup
     */
    data class BackupInfo(
        val totalItems: Int = 0,
        val subscriptionsCount: Int = 0,
        val warrantiesCount: Int = 0,
        val imagesCount: Int = 0,
        val estimatedSize: Long = 0L
    )

    /**
     * Resultado de operación de backup/restore
     */
    data class BackupOperationResult(
        val success: Boolean,
        val itemsCount: Int = 0,
        val imagesCount: Int = 0,
        val fileSize: Long = 0L,
        val backupDate: Long = 0L,
        val message: String
    )
}