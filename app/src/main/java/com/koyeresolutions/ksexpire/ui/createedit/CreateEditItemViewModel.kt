package com.koyeresolutions.ksexpire.ui.createedit

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.utils.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para crear y editar ítems
 * Maneja validación, estado del formulario y operaciones CRUD
 */
class CreateEditItemViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository

    // Estados del formulario
    private val _uiState = MutableStateFlow(CreateEditUiState())
    val uiState: StateFlow<CreateEditUiState> = _uiState.asStateFlow()

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _validationErrors = MutableLiveData<List<String>>()
    val validationErrors: LiveData<List<String>> = _validationErrors

    private val _saveResult = MutableLiveData<SaveResult?>()
    val saveResult: LiveData<SaveResult?> = _saveResult

    // Ítem actual (null para crear nuevo, objeto para editar)
    private var currentItem: Item? = null
    private var isEditMode = false

    /**
     * Inicializar para crear nuevo ítem
     */
    fun initializeForCreate(preselectedType: String? = null) {
        isEditMode = false
        currentItem = null
        
        val itemType = when (preselectedType) {
            "subscription" -> Constants.ITEM_TYPE_SUBSCRIPTION
            "warranty" -> Constants.ITEM_TYPE_WARRANTY
            else -> Constants.ITEM_TYPE_SUBSCRIPTION // Por defecto suscripción
        }
        
        _uiState.value = CreateEditUiState(
            itemType = itemType,
            isEditMode = false
        )
    }

    /**
     * Inicializar para editar ítem existente
     */
    fun initializeForEdit(itemId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                val item = repository.getItemById(itemId)
                if (item != null) {
                    currentItem = item
                    isEditMode = true
                    
                    _uiState.value = CreateEditUiState(
                        itemType = item.type,
                        name = item.name,
                        price = item.price?.toString() ?: "",
                        purchaseDate = item.purchaseDate,
                        expiryDate = item.expiryDate,
                        billingFrequency = item.billingFrequency ?: Constants.FREQUENCY_MONTHLY,
                        imagePath = item.imagePath,
                        isActive = item.isActive,
                        isEditMode = true
                    )
                } else {
                    _saveResult.value = SaveResult.Error("Ítem no encontrado")
                }
            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Error al cargar ítem: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Actualizar tipo de ítem
     */
    fun updateItemType(type: Int) {
        _uiState.value = _uiState.value.copy(
            itemType = type,
            billingFrequency = if (type == Constants.ITEM_TYPE_SUBSCRIPTION) {
                Constants.FREQUENCY_MONTHLY
            } else {
                null
            }
        )
    }

    /**
     * Actualizar nombre
     */
    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
        clearValidationErrors()
    }

    /**
     * Actualizar precio
     */
    fun updatePrice(price: String) {
        _uiState.value = _uiState.value.copy(price = price)
        clearValidationErrors()
    }

    /**
     * Actualizar fecha de compra
     */
    fun updatePurchaseDate(timestamp: Long) {
        _uiState.value = _uiState.value.copy(purchaseDate = timestamp)
        clearValidationErrors()
    }

    /**
     * Actualizar fecha de vencimiento
     */
    fun updateExpiryDate(timestamp: Long) {
        _uiState.value = _uiState.value.copy(expiryDate = timestamp)
        clearValidationErrors()
    }

    /**
     * Actualizar frecuencia de facturación
     */
    fun updateBillingFrequency(frequency: String) {
        _uiState.value = _uiState.value.copy(billingFrequency = frequency)
    }

    /**
     * Actualizar ruta de imagen
     */
    fun updateImagePath(imagePath: String?) {
        _uiState.value = _uiState.value.copy(imagePath = imagePath)
    }

    /**
     * Alternar estado activo
     */
    fun toggleActiveStatus() {
        _uiState.value = _uiState.value.copy(isActive = !_uiState.value.isActive)
    }

    /**
     * Validar formulario
     */
    private fun validateForm(): List<String> {
        val errors = mutableListOf<String>()
        val state = _uiState.value

        // Validar nombre
        if (state.name.isBlank()) {
            errors.add("El nombre es obligatorio")
        }

        // Validar fechas
        if (state.purchaseDate <= 0) {
            errors.add("La fecha de compra es obligatoria")
        }

        if (state.expiryDate <= 0) {
            errors.add("La fecha de vencimiento es obligatoria")
        }

        if (state.expiryDate <= state.purchaseDate) {
            errors.add("La fecha de vencimiento debe ser posterior a la de compra")
        }

        // Validar precio (si se proporciona)
        if (state.price.isNotBlank()) {
            val priceValue = state.price.toDoubleOrNull()
            if (priceValue == null || priceValue < 0) {
                errors.add("El precio debe ser un número válido mayor o igual a 0")
            }
        }

        // Validar frecuencia para suscripciones
        if (state.itemType == Constants.ITEM_TYPE_SUBSCRIPTION && state.billingFrequency.isNullOrBlank()) {
            errors.add("La frecuencia de cobro es obligatoria para suscripciones")
        }

        return errors
    }

    /**
     * Guardar ítem
     */
    fun saveItem() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Validar formulario
                val errors = validateForm()
                if (errors.isNotEmpty()) {
                    _validationErrors.value = errors
                    _isLoading.value = false
                    return@launch
                }

                val state = _uiState.value
                val priceValue = if (state.price.isBlank()) null else state.price.toDouble()
                android.util.Log.d("CreateEditVM", "priceValue after conversion: $priceValue")

                // Crear o actualizar ítem
                val item = if (isEditMode && currentItem != null) {
                    // Actualizar ítem existente
                    currentItem!!.copy(
                        type = state.itemType,
                        name = state.name.trim(),
                        price = priceValue,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate,
                        billingFrequency = state.billingFrequency,
                        imagePath = state.imagePath,
                        isActive = state.isActive,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // Crear nuevo ítem
                    Item(
                        type = state.itemType,
                        name = state.name.trim(),
                        price = priceValue,
                        purchaseDate = state.purchaseDate,
                        expiryDate = state.expiryDate,
                        billingFrequency = state.billingFrequency,
                        imagePath = state.imagePath,
                        isActive = state.isActive
                    )
                }

                // Guardar en repositorio
                val result = if (isEditMode) {
                    repository.updateItem(item)
                } else {
                    repository.insertItem(item)
                }

                result.fold(
                    onSuccess = { 
                        _saveResult.value = SaveResult.Success
                    },
                    onFailure = { exception ->
                        _saveResult.value = SaveResult.Error(exception.message ?: "Error desconocido")
                    }
                )

            } catch (e: Exception) {
                _saveResult.value = SaveResult.Error("Error al guardar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Limpiar errores de validación
     */
    private fun clearValidationErrors() {
        _validationErrors.value = emptyList()
    }

    /**
     * Limpiar resultado de guardado
     */
    fun clearSaveResult() {
        _saveResult.value = null
    }

    /**
     * Verificar si el formulario tiene cambios
     */
    fun hasChanges(): Boolean {
        if (!isEditMode) return true // Nuevo ítem siempre tiene cambios
        
        val state = _uiState.value
        val original = currentItem ?: return true
        
        return state.name != original.name ||
                state.price != (original.price?.toString() ?: "") ||
                state.purchaseDate != original.purchaseDate ||
                state.expiryDate != original.expiryDate ||
                state.billingFrequency != original.billingFrequency ||
                state.imagePath != original.imagePath ||
                state.isActive != original.isActive
    }

    /**
     * Estado del UI para crear/editar
     */
    data class CreateEditUiState(
        val itemType: Int = Constants.ITEM_TYPE_SUBSCRIPTION,
        val name: String = "",
        val price: String = "",
        val purchaseDate: Long = System.currentTimeMillis(),
        val expiryDate: Long = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L), // 30 días por defecto
        val billingFrequency: String? = Constants.FREQUENCY_MONTHLY,
        val imagePath: String? = null,
        val isActive: Boolean = true,
        val isEditMode: Boolean = false
    )

    /**
     * Resultado de operación de guardado
     */
    sealed class SaveResult {
        object Success : SaveResult()
        data class Error(val message: String) : SaveResult()
    }
}