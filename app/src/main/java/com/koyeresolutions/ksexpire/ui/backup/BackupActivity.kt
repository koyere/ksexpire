package com.koyeresolutions.ksexpire.ui.backup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.ActivityBackupBinding
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.DateUtils
import kotlinx.coroutines.launch

/**
 * Actividad para backup y restauración
 * IMPLEMENTA FUNCIONALIDAD COMPLETA DE BACKUP DEL PLANNING
 */
class BackupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBackupBinding
    private val viewModel: BackupViewModel by viewModels()

    // Launchers para selección de archivos
    private val createBackupLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument(Constants.BACKUP_MIME_TYPE)
    ) { uri ->
        uri?.let { viewModel.createBackup(it) }
    }

    private val selectBackupLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.validateAndRestoreBackup(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityBackupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        setupUI()
        setupObservers()
        loadBackupInfo()
    }

    /**
     * Configurar toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Backup y Restauración"
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Botón crear backup
        binding.buttonCreateBackup.setOnClickListener {
            createBackup()
        }
        
        // Botón restaurar backup
        binding.buttonRestoreBackup.setOnClickListener {
            selectBackupFile()
        }
        
        // Información adicional
        binding.textBackupInfo.text = """
            • Los backups incluyen todos tus datos e imágenes
            • Se guardan en formato ZIP comprimido
            • Puedes guardarlos en Google Drive, USB, etc.
            • La restauración reemplaza todos los datos actuales
        """.trimIndent()
    }

    /**
     * Configurar observadores
     */
    private fun setupObservers() {
        // Estado de carga
        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.buttonCreateBackup.isEnabled = !isLoading
            binding.buttonRestoreBackup.isEnabled = !isLoading
        }

        // Resultado de backup
        viewModel.backupResult.observe(this) { result ->
            result?.let {
                showBackupResult(it)
                viewModel.clearBackupResult()
            }
        }

        // Resultado de restauración
        viewModel.restoreResult.observe(this) { result ->
            result?.let {
                showRestoreResult(it)
                viewModel.clearRestoreResult()
            }
        }

        // Errores
        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Información de backup
        lifecycleScope.launch {
            viewModel.backupInfo.collect { info ->
                updateBackupInfo(info)
            }
        }
    }

    /**
     * Cargar información de backup
     */
    private fun loadBackupInfo() {
        viewModel.loadBackupInfo()
    }

    /**
     * Crear backup
     */
    private fun createBackup() {
        val fileName = viewModel.generateBackupFileName()
        createBackupLauncher.launch(fileName)
    }

    /**
     * Seleccionar archivo de backup
     */
    private fun selectBackupFile() {
        selectBackupLauncher.launch(Constants.BACKUP_MIME_TYPE)
    }

    /**
     * Actualizar información de backup
     */
    private fun updateBackupInfo(info: BackupViewModel.BackupInfo) {
        binding.apply {
            textTotalItems.text = "Total de ítems: ${info.totalItems}"
            textSubscriptionsCount.text = "Suscripciones: ${info.subscriptionsCount}"
            textWarrantiesCount.text = "Garantías: ${info.warrantiesCount}"
            textImagesCount.text = "Imágenes: ${info.imagesCount}"
            
            val sizeText = if (info.estimatedSize > 0) {
                "Tamaño estimado: ${formatFileSize(info.estimatedSize)}"
            } else {
                "Calculando tamaño..."
            }
            textEstimatedSize.text = sizeText
        }
    }

    /**
     * Mostrar resultado de backup
     */
    private fun showBackupResult(result: BackupViewModel.BackupOperationResult) {
        if (result.success) {
            val message = """
                Backup creado exitosamente
                
                • ${result.itemsCount} ítems guardados
                • ${result.imagesCount} imágenes incluidas
                • Tamaño: ${formatFileSize(result.fileSize)}
            """.trimIndent()
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Backup Completado")
                .setMessage(message)
                .setPositiveButton("Aceptar", null)
                .setIcon(R.drawable.ic_check_circle)
                .show()
        } else {
            showError(result.message)
        }
    }

    /**
     * Mostrar resultado de restauración
     */
    private fun showRestoreResult(result: BackupViewModel.BackupOperationResult) {
        if (result.success) {
            val backupDate = DateUtils.formatDateTime(result.backupDate)
            val message = """
                Backup restaurado exitosamente
                
                • ${result.itemsCount} ítems restaurados
                • ${result.imagesCount} imágenes restauradas
                • Fecha del backup: $backupDate
                
                La aplicación se reiniciará para aplicar los cambios.
            """.trimIndent()
            
            MaterialAlertDialogBuilder(this)
                .setTitle("Restauración Completada")
                .setMessage(message)
                .setPositiveButton("Reiniciar") { _, _ ->
                    restartApp()
                }
                .setCancelable(false)
                .setIcon(R.drawable.ic_check_circle)
                .show()
        } else {
            showError(result.message)
        }
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, "Error: $message", Snackbar.LENGTH_LONG).show()
    }

    /**
     * Formatear tamaño de archivo
     */
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> "${bytes / (1024 * 1024)} MB"
        }
    }

    /**
     * Reiniciar aplicación
     */
    private fun restartApp() {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finishAffinity()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}