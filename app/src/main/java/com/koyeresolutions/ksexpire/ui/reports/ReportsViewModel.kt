package com.koyeresolutions.ksexpire.ui.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel para la pantalla de reportes
 */
class ReportsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository

    private val _reportData = MutableLiveData<ReportData>()
    val reportData: LiveData<ReportData> = _reportData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    init {
        loadReportData()
    }

    fun loadReportData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val monthlyExpense = repository.calculateMonthlyExpense()
                val annualProjection = monthlyExpense * 12
                val expenseByCategory = repository.getExpenseByCategory()
                val topSubscriptions = repository.getTopExpensiveSubscriptions()
                val allActive = repository.getAllActiveItems().first()
                val subsCount = allActive.count { it.isSubscription() }
                val warrCount = allActive.count { it.isWarranty() }

                _reportData.value = ReportData(
                    monthlyExpense = monthlyExpense,
                    annualProjection = annualProjection,
                    expenseByCategory = expenseByCategory,
                    topSubscriptions = topSubscriptions,
                    subscriptionsCount = subsCount,
                    warrantiesCount = warrCount
                )
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    data class ReportData(
        val monthlyExpense: Double,
        val annualProjection: Double,
        val expenseByCategory: Map<String, Double>,
        val topSubscriptions: List<Item>,
        val subscriptionsCount: Int,
        val warrantiesCount: Int
    )
}
