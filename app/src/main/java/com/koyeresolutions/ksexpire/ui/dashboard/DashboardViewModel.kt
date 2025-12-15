package com.koyeresolutions.ksexpire.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel para el Dashboard principal
 * IMPLEMENTA LA LÓGICA DE GASTO MENSUAL NORMALIZADO DEL PLANNING
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository

    // Estados del UI
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val _monthlyExpense = MutableLiveData<Double>()
    val monthlyExpense: LiveData<Double> = _monthlyExpense

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    // Datos observables
    val subscriptions = repository.getActiveSubscriptions()
    val warranties = repository.getActiveWarranties()
    val dashboardItems = repository.getDashboardItems()

    init {
        loadDashboardData()
        // Observar cambios en suscripciones para recalcular gasto mensual
        viewModelScope.launch {
            subscriptions.collect {
                loadDashboardData()
            }
        }
    }

    /**
     * Cargar datos del dashboard
     */
    private fun loadDashboardData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Calcular gasto mensual normalizado
                val monthlyExpense = repository.calculateMonthlyExpense()
                _monthlyExpense.value = monthlyExpense

                // Actualizar estado del UI
                _uiState.value = _uiState.value.copy(
                    monthlyExpense = monthlyExpense,
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar datos: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Refrescar datos del dashboard
     */
    fun refreshData() {
        loadDashboardData()
    }

    /**
     * Formatear gasto mensual para mostrar en UI
     */
    fun getFormattedMonthlyExpense(): String {
        val expense = _monthlyExpense.value ?: 0.0
        return CurrencyUtils.formatMonthlyExpense(getApplication(), expense)
    }

    /**
     * Verificar si hay datos para mostrar
     */
    fun hasData(): Boolean {
        return _uiState.value.let { state ->
            state.subscriptionsCount > 0 || state.warrantiesCount > 0
        }
    }

    /**
     * Manejar eliminación de ítem
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                loadDashboardData() // Refrescar después de eliminar
            } catch (e: Exception) {
                _errorMessage.value = "Error al eliminar ítem: ${e.message}"
            }
        }
    }

    /**
     * Alternar estado activo/inactivo de ítem
     */
    fun toggleItemStatus(item: Item) {
        viewModelScope.launch {
            try {
                val updatedItem = item.copy(
                    isActive = !item.isActive,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateItem(updatedItem)
                loadDashboardData()
            } catch (e: Exception) {
                _errorMessage.value = "Error al actualizar ítem: ${e.message}"
            }
        }
    }

    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _errorMessage.value = null
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Estado del UI del Dashboard
     */
    data class DashboardUiState(
        val monthlyExpense: Double = 0.0,
        val subscriptionsCount: Int = 0,
        val warrantiesCount: Int = 0,
        val isLoading: Boolean = true,
        val error: String? = null,
        val isEmpty: Boolean = false
    )
}