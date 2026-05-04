package com.koyeresolutions.ksexpire.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.koyeresolutions.ksexpire.KSExpireApplication
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.utils.Constants
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel para búsqueda con funcionalidad avanzada
 * Implementa búsqueda en tiempo real con filtros y estadísticas
 */
@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ItemRepository = (application as KSExpireApplication).repository

    // Query de búsqueda
    private val _searchQuery = MutableStateFlow("")
    val currentQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filtro actual
    private val _searchFilter = MutableStateFlow(SearchFilter.ALL)
    val currentFilter: StateFlow<SearchFilter> = _searchFilter.asStateFlow()

    // Filtro de categoría
    private val _categoryFilter = MutableStateFlow<String?>(null)
    val categoryFilter: StateFlow<String?> = _categoryFilter.asStateFlow()

    // Categorías usadas
    val usedCategories = repository.getUsedCategories()

    // Flag para evitar re-aplicar argumentos de navegación en rotación
    var hasProcessedNavigationArgs = false

    // Resultados de búsqueda
    private val _searchResults = MutableLiveData<List<Item>>()
    val searchResults: LiveData<List<Item>> = _searchResults

    // Estado de carga
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // Estadísticas de búsqueda
    private val _searchStats = MutableStateFlow(SearchStats())
    val searchStats: StateFlow<SearchStats> = _searchStats.asStateFlow()

    init {
        setupSearch()
        loadInitialStats()
    }

    /**
     * Configurar búsqueda reactiva
     */
    private fun setupSearch() {
        combine(
            _searchQuery.debounce(300),
            _searchFilter,
            _categoryFilter
        ) { query, filter, category ->
            Triple(query, filter, category)
        }.onEach { (query, filter, category) ->
            performSearch(query, filter, category)
        }.launchIn(viewModelScope)
    }

    /**
     * Cargar estadísticas iniciales
     */
    private fun loadInitialStats() {
        viewModelScope.launch {
            try {
                repository.getAllActiveItems().collect { items ->
                    val stats = SearchStats(
                        totalItems = items.size,
                        subscriptionsCount = items.count { it.isSubscription() },
                        warrantiesCount = items.count { it.isWarranty() }
                    )
                    _searchStats.value = stats
                }
            } catch (e: Exception) {
                // Manejar error silenciosamente
            }
        }
    }

    /**
     * Realizar búsqueda
     */
    fun search(query: String) {
        _searchQuery.value = query.trim()
    }

    /**
     * Establecer filtro
     */
    fun setFilter(filter: SearchFilter) {
        _searchFilter.value = filter
    }

    /**
     * Establecer filtro de categoría
     */
    fun setCategoryFilter(category: String?) {
        _categoryFilter.value = category
    }

    /**
     * Limpiar búsqueda
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchFilter.value = SearchFilter.ALL
        _categoryFilter.value = null
    }

    fun refreshSearch() {
        val currentQuery = _searchQuery.value
        val currentFilter = _searchFilter.value
        val currentCategory = _categoryFilter.value
        performSearch(currentQuery, currentFilter, currentCategory)
    }

    /**
     * Realizar búsqueda con filtros combinados
     */
    private fun performSearch(query: String, filter: SearchFilter, category: String?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val typeFilter: Int? = when (filter) {
                    SearchFilter.SUBSCRIPTIONS -> Constants.ITEM_TYPE_SUBSCRIPTION
                    SearchFilter.WARRANTIES -> Constants.ITEM_TYPE_WARRANTY
                    SearchFilter.ALL -> null
                }

                val results = repository.searchItemsFiltered(query, typeFilter, category).first()
                _searchResults.value = results

            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Alternar estado de ítem
     */
    fun toggleItemStatus(item: Item) {
        viewModelScope.launch {
            try {
                val updatedItem = item.copy(
                    isActive = !item.isActive,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateItem(updatedItem)
                refreshSearch() // Refrescar resultados
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    /**
     * Eliminar ítem
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                refreshSearch() // Refrescar resultados
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    /**
     * Obtener sugerencias de búsqueda
     */
    fun getSearchSuggestions(): List<String> {
        // Implementar sugerencias basadas en nombres frecuentes
        return listOf(
            "Netflix", "Spotify", "Amazon Prime", "Disney+",
            "Samsung", "Apple", "Sony", "LG"
        )
    }

    /**
     * Filtros de búsqueda
     */
    enum class SearchFilter {
        ALL,
        SUBSCRIPTIONS,
        WARRANTIES
    }

    /**
     * Estadísticas de búsqueda
     */
    data class SearchStats(
        val totalItems: Int = 0,
        val subscriptionsCount: Int = 0,
        val warrantiesCount: Int = 0
    )
}