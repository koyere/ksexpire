package com.koyeresolutions.ksexpire.ui.about

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.play.core.review.ReviewManagerFactory
import com.koyeresolutions.ksexpire.utils.PreferencesManager
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla "Sobre el Desarrollador"
 * IMPLEMENTA GOOGLE PLAY IN-APP REVIEW API DEL PLANNING
 */
class AboutViewModel(application: Application) : AndroidViewModel(application) {

    private val preferencesManager = PreferencesManager(application)
    private val reviewManager = ReviewManagerFactory.create(application)

    // Estados del UI
    private val _reviewResult = MutableLiveData<Boolean?>()
    val reviewResult: LiveData<Boolean?> = _reviewResult

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    /**
     * Solicitar review in-app
     * IMPLEMENTA LA FUNCIONALIDAD CRÍTICA DEL PLANNING
     */
    fun requestInAppReview(activity: Activity) {
        viewModelScope.launch {
            try {
                // Verificar si ya se solicitó review antes
                if (preferencesManager.isReviewRequested()) {
                    // Si ya se pidió, ir directo a Play Store
                    _reviewResult.value = false
                    return@launch
                }

                // Solicitar ReviewInfo
                val request = reviewManager.requestReviewFlow()
                request.addOnCompleteListener { requestTask ->
                    if (requestTask.isSuccessful) {
                        // ReviewInfo obtenido exitosamente
                        val reviewInfo = requestTask.result
                        
                        // Lanzar el flujo de review
                        val flow = reviewManager.launchReviewFlow(activity, reviewInfo)
                        flow.addOnCompleteListener { flowTask ->
                            // El flujo se completó (sin importar si el usuario calificó o no)
                            // Marcar como solicitado para no volver a mostrar
                            preferencesManager.setReviewRequested(true)
                            _reviewResult.value = true
                        }
                    } else {
                        // Error al obtener ReviewInfo, ir a Play Store
                        _errorMessage.value = "No se pudo mostrar el diálogo de calificación"
                        _reviewResult.value = false
                    }
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "Error al solicitar calificación: ${e.message}"
                _reviewResult.value = false
            }
        }
    }

    /**
     * Verificar si se debe mostrar el trigger automático de review
     * SEGÚN EL PLANNING: Después del 3er ítem creado
     */
    fun checkAutoReviewTrigger(): Boolean {
        val itemsCreated = preferencesManager.getItemsCreatedCount()
        val reviewRequested = preferencesManager.isReviewRequested()
        
        // Mostrar review si:
        // 1. Ha creado exactamente 3 ítems
        // 2. No se ha solicitado review antes
        return itemsCreated >= 3 && !reviewRequested
    }

    /**
     * Incrementar contador de ítems creados
     * Para el trigger automático de review
     */
    fun incrementItemsCreated() {
        val currentCount = preferencesManager.getItemsCreatedCount()
        preferencesManager.setItemsCreatedCount(currentCount + 1)
    }

    /**
     * Limpiar resultado de review
     */
    fun clearReviewResult() {
        _reviewResult.value = null
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
    }
}