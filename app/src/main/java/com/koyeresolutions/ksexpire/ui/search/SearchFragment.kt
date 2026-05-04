package com.koyeresolutions.ksexpire.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.FragmentSearchBinding
import com.koyeresolutions.ksexpire.ui.detail.ItemDetailActivity
import com.koyeresolutions.ksexpire.ui.imageviewer.ImageViewerActivity
import com.koyeresolutions.ksexpire.ui.search.adapters.SearchResultsAdapter
import com.koyeresolutions.ksexpire.utils.Constants
import kotlinx.coroutines.launch

/**
 * Fragment de búsqueda con funcionalidad avanzada
 * IMPLEMENTA BÚSQUEDA RÁPIDA DEL PLANNING
 */
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var searchAdapter: SearchResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        setupRecyclerView()
        setupObservers()
        setupSearchView()
        setupFilters()
        
        // Procesar argumentos de navegación (filtro desde Dashboard)
        processNavigationArguments()
    }
    
    /**
     * Procesar argumentos de navegación para aplicar filtros
     * Usa flag en ViewModel para evitar re-aplicación en rotación
     */
    private fun processNavigationArguments() {
        if (viewModel.hasProcessedNavigationArgs) return
        
        val filterType = arguments?.getString("filter_type")
        
        if (!filterType.isNullOrBlank()) {
            viewModel.hasProcessedNavigationArgs = true
            when (filterType) {
                "subscriptions" -> {
                    viewModel.setFilter(SearchViewModel.SearchFilter.SUBSCRIPTIONS)
                    viewModel.search("")
                }
                "warranties" -> {
                    viewModel.setFilter(SearchViewModel.SearchFilter.WARRANTIES)
                    viewModel.search("")
                }
            }
        }
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Cargar todos los items al iniciar (estado por defecto)
        viewModel.search("")
    }

    /**
     * Configurar RecyclerView
     */
    private fun setupRecyclerView() {
        searchAdapter = SearchResultsAdapter(
            onItemClick = { item ->
                openEditItem(item.id)
            },
            onImageClick = { item ->
                if (!item.imagePath.isNullOrBlank()) {
                    openImageViewer(item.imagePath, item.name)
                }
            },
            onToggleStatus = { item ->
                viewModel.toggleItemStatus(item)
            },
            onDeleteClick = { item ->
                viewModel.deleteItem(item)
            }
        )

        binding.recyclerResults.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = searchAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * Configurar observadores
     */
    private fun setupObservers() {
        // Observar resultados de búsqueda
        viewModel.searchResults.observe(viewLifecycleOwner) { results ->
            searchAdapter.submitList(results)
            updateResultsVisibility(results.isEmpty())
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar Flows con lifecycle awareness
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.currentQuery.collect { query ->
                        updateSearchInfo(query)
                    }
                }
                launch {
                    viewModel.currentFilter.collect { filter ->
                        updateFilterChips(filter)
                    }
                }
                launch {
                    viewModel.searchStats.collect { stats ->
                        updateSearchStats(stats)
                    }
                }
                launch {
                    viewModel.usedCategories.collect { categories ->
                        updateCategoryChips(categories)
                    }
                }
            }
        }
    }

    /**
     * Configurar SearchView
     */
    private fun setupSearchView() {
        binding.searchView.apply {
            setIconifiedByDefault(false)
            queryHint = getString(R.string.search_hint)
            
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { viewModel.search(it) }
                    clearFocus()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let { viewModel.search(it) }
                    return true
                }
            })

            // Configurar botón de limpiar
            setOnCloseListener {
                viewModel.clearSearch()
                false
            }
        }
    }

    /**
     * Configurar filtros por tipo
     */
    private fun setupFilters() {
        binding.chipGroupFilters.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when {
                checkedIds.contains(R.id.chip_subscriptions) -> SearchViewModel.SearchFilter.SUBSCRIPTIONS
                checkedIds.contains(R.id.chip_warranties) -> SearchViewModel.SearchFilter.WARRANTIES
                else -> SearchViewModel.SearchFilter.ALL
            }
            viewModel.setFilter(filter)
        }

        // Configurar chips individuales
        binding.chipAll.setOnClickListener { 
            viewModel.setFilter(SearchViewModel.SearchFilter.ALL)
        }
        
        binding.chipSubscriptions.setOnClickListener { 
            viewModel.setFilter(SearchViewModel.SearchFilter.SUBSCRIPTIONS)
        }
        
        binding.chipWarranties.setOnClickListener { 
            viewModel.setFilter(SearchViewModel.SearchFilter.WARRANTIES)
        }
    }

    /**
     * Actualizar visibilidad de resultados
     */
    private fun updateResultsVisibility(isEmpty: Boolean) {
        // Siempre mostrar resultados si hay items, o "sin resultados" si está vacío
        // El estado inicial vacío solo se muestra antes de cualquier interacción
        if (isEmpty) {
            showNoResultsState()
        } else {
            showResultsState()
        }
    }

    /**
     * Mostrar estado vacío (sin búsqueda)
     */
    private fun showEmptyState() {
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.layoutNoResults.visibility = View.GONE
        binding.recyclerResults.visibility = View.GONE
        binding.layoutSearchInfo.visibility = View.GONE
    }

    /**
     * Mostrar estado sin resultados
     */
    private fun showNoResultsState() {
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutNoResults.visibility = View.VISIBLE
        binding.recyclerResults.visibility = View.GONE
        binding.layoutSearchInfo.visibility = View.VISIBLE
    }

    /**
     * Mostrar estado con resultados
     */
    private fun showResultsState() {
        binding.layoutEmpty.visibility = View.GONE
        binding.layoutNoResults.visibility = View.GONE
        binding.recyclerResults.visibility = View.VISIBLE
        binding.layoutSearchInfo.visibility = View.VISIBLE
    }

    /**
     * Actualizar información de búsqueda
     */
    private fun updateSearchInfo(query: String) {
        if (query.isBlank()) {
            binding.textSearchInfo.text = ""
        } else {
            val resultCount = searchAdapter.itemCount
            binding.textSearchInfo.text = getString(R.string.search_results_count, resultCount)
        }
    }

    /**
     * Actualizar chips de filtro
     */
    private fun updateFilterChips(filter: SearchViewModel.SearchFilter) {
        binding.chipGroupFilters.clearCheck()
        
        when (filter) {
            SearchViewModel.SearchFilter.ALL -> binding.chipAll.isChecked = true
            SearchViewModel.SearchFilter.SUBSCRIPTIONS -> binding.chipSubscriptions.isChecked = true
            SearchViewModel.SearchFilter.WARRANTIES -> binding.chipWarranties.isChecked = true
        }
    }

    /**
     * Actualizar estadísticas de búsqueda
     */
    private fun updateSearchStats(stats: SearchViewModel.SearchStats) {
        binding.textTotalItems.text = getString(R.string.search_total_items, stats.totalItems)
        binding.textSubscriptionsCount.text = getString(R.string.search_subscriptions_count, stats.subscriptionsCount)
        binding.textWarrantiesCount.text = getString(R.string.search_warranties_count, stats.warrantiesCount)
    }

    /**
     * Actualizar chips de categoría dinámicamente
     */
    private fun updateCategoryChips(categories: List<String>) {
        val chipGroup = binding.chipGroupCategories
        chipGroup.removeAllViews()
        
        if (categories.isEmpty()) {
            binding.scrollCategoryFilters.visibility = View.GONE
            return
        }
        
        binding.scrollCategoryFilters.visibility = View.VISIBLE
        
        // Chip "Todas"
        val allChip = com.google.android.material.chip.Chip(requireContext()).apply {
            text = "Todas"
            isCheckable = true
            isChecked = true
            setOnClickListener { viewModel.setCategoryFilter(null) }
        }
        chipGroup.addView(allChip)
        
        // Chips por categoría usada
        categories.forEach { categoryName ->
            val predefined = Constants.PREDEFINED_CATEGORIES.find { it.name == categoryName }
            val chip = com.google.android.material.chip.Chip(requireContext()).apply {
                text = if (predefined != null) "${predefined.emoji} $categoryName" else categoryName
                isCheckable = true
                setOnClickListener { viewModel.setCategoryFilter(categoryName) }
            }
            chipGroup.addView(chip)
        }
    }

    /**
     * Abrir actividad de detalle del ítem
     */
    private fun openEditItem(itemId: Long) {
        val intent = Intent(requireContext(), ItemDetailActivity::class.java)
        intent.putExtra(ItemDetailActivity.EXTRA_ITEM_ID, itemId)
        startActivity(intent)
    }

    /**
     * Abrir visor de imagen
     */
    private fun openImageViewer(imagePath: String, itemName: String) {
        val intent = Intent(requireContext(), ImageViewerActivity::class.java)
        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_PATH, imagePath)
        intent.putExtra(ImageViewerActivity.EXTRA_ITEM_NAME, itemName)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refrescar resultados cuando se regresa al fragment
        viewModel.refreshSearch()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}