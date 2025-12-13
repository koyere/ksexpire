package com.koyeresolutions.ksexpire.ui.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.ActivityCameraBinding
import com.koyeresolutions.ksexpire.utils.FileUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Actividad de cámara con CameraX
 * Captura fotos de recibos con compresión automática
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUIRED_PERMISSIONS = Manifest.permission.CAMERA
    }

    // Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            showPermissionDenied()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkPermissionsAndStartCamera()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Botón de captura
        binding.buttonCapture.setOnClickListener {
            takePhoto()
        }
        
        // Botón de cerrar
        binding.buttonClose.setOnClickListener {
            finish()
        }
        
        // Botón de flash
        binding.buttonFlash.setOnClickListener {
            toggleFlash()
        }
    }
    /**
     * Verificar permisos y iniciar cámara
     */
    private fun checkPermissionsAndStartCamera() {
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    /**
     * Verificar si todos los permisos están concedidos
     */
    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(
        this, REQUIRED_PERMISSIONS
    ) == PackageManager.PERMISSION_GRANTED

    /**
     * Iniciar cámara con CameraX
     */
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                // Obtener el proveedor de cámara
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

                // Preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                    }

                // ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setJpegQuality(85) // Calidad alta para recibos
                    .build()

                // Seleccionar cámara trasera por defecto
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Desvincular casos de uso antes de vincular nuevos
                cameraProvider.unbindAll()

                // Vincular casos de uso a la cámara
                val camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

                // Configurar controles de cámara
                setupCameraControls(camera)

            } catch (exc: Exception) {
                Log.e(TAG, "Error al iniciar cámara", exc)
                showError("Error al iniciar la cámara")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /**
     * Configurar controles de cámara (flash, etc.)
     */
    private fun setupCameraControls(camera: Camera) {
        val cameraControl = camera.cameraControl
        val cameraInfo = camera.cameraInfo

        // Verificar si tiene flash
        if (cameraInfo.hasFlashUnit()) {
            binding.buttonFlash.visibility = View.VISIBLE
        } else {
            binding.buttonFlash.visibility = View.GONE
        }
    }

    /**
     * Capturar foto
     */
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Mostrar indicador de captura
        binding.buttonCapture.isEnabled = false
        binding.progressCapture.visibility = View.VISIBLE

        // Configurar opciones de salida
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            createTempImageFile()
        ).build()

        // Capturar imagen
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error al capturar foto: ${exception.message}", exception)
                    showError("Error al capturar la foto")
                    resetCaptureButton()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    processAndSaveImage(output.savedUri?.path)
                }
            }
        )
    }
    /**
     * Procesar y guardar imagen capturada
     */
    private fun processAndSaveImage(tempImagePath: String?) {
        if (tempImagePath == null) {
            showError("Error al procesar la imagen")
            resetCaptureButton()
            return
        }

        try {
            // Cargar imagen temporal
            val bitmap = BitmapFactory.decodeFile(tempImagePath)
            if (bitmap == null) {
                showError("Error al cargar la imagen")
                resetCaptureButton()
                return
            }

            // Corregir orientación si es necesario
            val correctedBitmap = FileUtils.correctImageOrientation(bitmap, tempImagePath)

            // Redimensionar para optimizar almacenamiento
            val resizedBitmap = FileUtils.resizeBitmap(
                correctedBitmap,
                com.koyeresolutions.ksexpire.utils.Constants.MAX_IMAGE_WIDTH,
                com.koyeresolutions.ksexpire.utils.Constants.MAX_IMAGE_HEIGHT
            )

            // Guardar imagen comprimida
            val savedImagePath = FileUtils.saveCompressedImage(resizedBitmap, this)

            if (savedImagePath != null) {
                // Limpiar archivo temporal
                java.io.File(tempImagePath).delete()

                // Retornar resultado
                val resultIntent = Intent().apply {
                    putExtra("image_path", savedImagePath)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            } else {
                showError("Error al guardar la imagen")
                resetCaptureButton()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar imagen", e)
            showError("Error al procesar la imagen")
            resetCaptureButton()
        }
    }

    /**
     * Crear archivo temporal para captura
     */
    private fun createTempImageFile(): java.io.File {
        val tempDir = java.io.File(cacheDir, "temp_camera")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        return java.io.File(tempDir, "temp_${System.currentTimeMillis()}.jpg")
    }

    /**
     * Alternar flash
     */
    private fun toggleFlash() {
        val imageCapture = imageCapture ?: return
        
        val currentFlashMode = imageCapture.flashMode
        val newFlashMode = if (currentFlashMode == ImageCapture.FLASH_MODE_OFF) {
            ImageCapture.FLASH_MODE_ON
        } else {
            ImageCapture.FLASH_MODE_OFF
        }
        
        imageCapture.flashMode = newFlashMode
        updateFlashButton(newFlashMode)
    }

    /**
     * Actualizar botón de flash
     */
    private fun updateFlashButton(flashMode: Int) {
        val iconRes = if (flashMode == ImageCapture.FLASH_MODE_ON) {
            R.drawable.ic_flash_on
        } else {
            R.drawable.ic_flash_off
        }
        binding.buttonFlash.setImageResource(iconRes)
    }

    /**
     * Resetear botón de captura
     */
    private fun resetCaptureButton() {
        binding.buttonCapture.isEnabled = true
        binding.progressCapture.visibility = View.GONE
    }

    /**
     * Mostrar error de permisos denegados
     */
    private fun showPermissionDenied() {
        Snackbar.make(
            binding.root,
            getString(R.string.camera_permission_denied),
            Snackbar.LENGTH_LONG
        ).setAction("Configuración") {
            // Abrir configuración de la app
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = android.net.Uri.parse("package:$packageName")
            startActivity(intent)
        }.show()
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}