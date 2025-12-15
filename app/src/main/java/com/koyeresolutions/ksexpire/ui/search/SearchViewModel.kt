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
        // Combinar query y filtro para búsqueda reactiva
        combine(
            _searchQuery.debounce(300), // Debounce para evitar búsquedas excesivas
            _searchFilter
        ) { query, filter ->
            Pair(query, filter)
        }.onEach { (query, filter) ->
            performSearch(query, filter)
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
     * Limpiar búsqueda
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchFilter.value = SearchFilter.ALL
    }

    /**
     * Refrescar búsqueda actual
     */
    fun refreshSearch() {
        val currentQuery = _searchQuery.value
        val currentFilter = _searchFilter.value
        performSearch(currentQuery, currentFilter)
    }

    /**
     * Realizar búsqueda interna
     */
    private fun performSearch(query: String, filter: SearchFilter) {
        viewModelScope.launch {
            try {
                _isLoading.value = true

                val results = when {
                    // Si hay filtro específico pero no query, mostrar todos de ese tipo
                    query.isBlank() && filter == SearchFilter.SUBSCRIPTIONS -> {
                        repository.getActiveSubscriptions().first()
                    }
                    query.isBlank() && filter == SearchFilter.WARRANTIES -> {
                        repository.getActiveWarranties().first()
                    }
                    query.isBlank() && filter == SearchFilter.ALL -> {
                        // Mostrar todos los items activos cuando se selecciona "Todos"
                        repository.getAllActiveItems().first()
                    }
                    // Búsqueda con query
                    filter == SearchFilter.ALL -> {
                        repository.searchItems(query).first()
                    }
                    filter == SearchFilter.SUBSCRIPTIONS -> {
                        repository.searchItemsByType(query, Constants.ITEM_TYPE_SUBSCRIPTION).first()
                    }
                    filter == SearchFilter.WARRANTIES -> {
                        repository.searchItemsByType(query, Constants.ITEM_TYPE_WARRANTY).first()
                    }
                    else -> emptyList()
                }

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