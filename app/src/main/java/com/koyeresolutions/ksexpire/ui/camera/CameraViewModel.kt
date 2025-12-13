package com.koyeresolutions.ksexpire.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel para la cámara
 * Maneja el procesamiento de imágenes en segundo plano
 */
class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val _isProcessing = MutableLiveData<Boolean>()
    val isProcessing: LiveData<Boolean> = _isProcessing

    private val _processingResult = MutableLiveData<ProcessingResult>()
    val processingResult: LiveData<ProcessingResult> = _processingResult

    /**
     * Procesar imagen capturada en segundo plano
     */
    fun processImage(tempImagePath: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                val result = withContext(Dispatchers.IO) {
                    processImageInternal(tempImagePath)
                }
                
                _processingResult.value = result
                
            } catch (e: Exception) {
                _processingResult.value = ProcessingResult.Error("Error al procesar imagen: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Procesamiento interno de imagen
     */
    private suspend fun processImageInternal(tempImagePath: String): ProcessingResult {
        return try {
            // Cargar imagen
            val bitmap = android.graphics.BitmapFactory.decodeFile(tempImagePath)
                ?: return ProcessingResult.Error("No se pudo cargar la imagen")

            // Corregir orientación
            val correctedBitmap = FileUtils.correctImageOrientation(bitmap, tempImagePath)

            // Redimensionar
            val resizedBitmap = FileUtils.resizeBitmap(
                correctedBitmap,
                com.koyeresolutions.ksexpire.utils.Constants.MAX_IMAGE_WIDTH,
                com.koyeresolutions.ksexpire.utils.Constants.MAX_IMAGE_HEIGHT
            )

            // Guardar imagen comprimida
            val savedPath = FileUtils.saveCompressedImage(resizedBitmap, getApplication())
                ?: return ProcessingResult.Error("No se pudo guardar la imagen")

            // Limpiar archivo temporal
            java.io.File(tempImagePath).delete()

            ProcessingResult.Success(savedPath)

        } catch (e: Exception) {
            ProcessingResult.Error("Error en procesamiento: ${e.message}")
        }
    }

    /**
     * Limpiar resultado
     */
    fun clearResult() {
        _processingResult.value = null
    }

    /**
     * Resultado del procesamiento de imagen
     */
    sealed class ProcessingResult {
        data class Success(val imagePath: String) : ProcessingResult()
        data class Error(val message: String) : ProcessingResult()
    }
}