package com.koyeresolutions.ksexpire.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.databinding.FragmentDashboardBinding
import com.koyeresolutions.ksexpire.ui.backup.BackupActivity
import com.koyeresolutions.ksexpire.ui.createedit.CreateEditItemActivity
import com.koyeresolutions.ksexpire.ui.dashboard.adapters.SubscriptionAdapter
import com.koyeresolutions.ksexpire.ui.dashboard.adapters.WarrantyAdapter
import com.koyeresolutions.ksexpire.ui.settings.NotificationSettingsActivity
import kotlinx.coroutines.launch

/**
 * Fragment principal del Dashboard
 * Muestra gasto mensual, suscripciones y garantías
 */
class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DashboardViewModel by viewModels()
    
    private lateinit var subscriptionAdapter: SubscriptionAdapter
    private lateinit var warrantyAdapter: WarrantyAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMenu()
        setupUI()
        setupRecyclerViews()
        setupObservers()
        setupClickListeners()
    }

    /**
     * Configurar menú del dashboard
     */
    private fun setupMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_dashboard, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_backup -> {
                        openBackupActivity()
                        true
                    }
                    R.id.action_settings -> {
                        openNotificationSettings()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Configurar SwipeRefreshLayout
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshData()
        }
        
        // Configurar colores del SwipeRefresh
        binding.swipeRefresh.setColorSchemeResources(
            R.color.md_theme_light_primary,
            R.color.md_theme_light_secondary,
            R.color.md_theme_light_tertiary
        )
    }

    /**
     * Configurar RecyclerViews
     */
    private fun setupRecyclerViews() {
        // Adapter para suscripciones
        subscriptionAdapter = SubscriptionAdapter(
            onItemClick = { item ->
                openEditItem(item.id)
            },
            onToggleStatus = { item ->
                viewModel.toggleItemStatus(item)
            },
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )

        // Adapter para garantías
        warrantyAdapter = WarrantyAdapter(
            onItemClick = { item ->
                openEditItem(item.id)
            },
            onToggleStatus = { item ->
                viewModel.toggleItemStatus(item)
            },
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )

        // Configurar RecyclerViews
        binding.recyclerSubscriptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subscriptionAdapter
            setHasFixedSize(true)
        }

        binding.recyclerWarranties.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = warrantyAdapter
            setHasFixedSize(true)
        }
    }

    /**
     * Configurar observadores de datos
     */
    private fun setupObservers() {
        // Observar gasto mensual
        viewModel.monthlyExpense.observe(viewLifecycleOwner) { expense ->
            binding.textMonthlyExpense.text = viewModel.getFormattedMonthlyExpense()
        }

        // Observar estado de carga
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.swipeRefresh.isRefreshing = isLoading
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Observar errores
        viewModel.errorMessage.observe(viewLifecycleOwner) { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }

        // Observar suscripciones
        viewModel.subscriptions.observe(viewLifecycleOwner) { subscriptions ->
            subscriptionAdapter.submitList(subscriptions)
            updateSubscriptionsVisibility(subscriptions.isEmpty())
        }

        // Observar garantías
        viewModel.warranties.observe(viewLifecycleOwner) { warranties ->
            warrantyAdapter.submitList(warranties)
            updateWarrantiesVisibility(warranties.isEmpty())
        }

        // Observar estado del UI
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                updateEmptyState(state.isEmpty)
            }
        }
    }

    /**
     * Configurar listeners de clicks
     */
    private fun setupClickListeners() {
        // FAB para agregar nuevo ítem
        binding.fabAdd.setOnClickListener {
            openCreateItem()
        }

        // Botón "Ver todas" para suscripciones
        binding.buttonViewAllSubscriptions.setOnClickListener {
            // TODO: Navegar a lista completa de suscripciones
        }

        // Botón "Ver todas" para garantías
        binding.buttonViewAllWarranties.setOnClickListener {
            // TODO: Navegar a lista completa de garantías
        }

        // Botón de agregar primera suscripción
        binding.buttonAddFirstSubscription.setOnClickListener {
            openCreateItem(preselectedType = "subscription")
        }

        // Botón de agregar primera garantía
        binding.buttonAddFirstWarranty.setOnClickListener {
            openCreateItem(preselectedType = "warranty")
        }
    }

    /**
     * Actualizar visibilidad de sección de suscripciones
     */
    private fun updateSubscriptionsVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerSubscriptions.visibility = View.GONE
            binding.layoutEmptySubscriptions.visibility = View.VISIBLE
            binding.buttonViewAllSubscriptions.visibility = View.GONE
        } else {
            binding.recyclerSubscriptions.visibility = View.VISIBLE
            binding.layoutEmptySubscriptions.visibility = View.GONE
            binding.buttonViewAllSubscriptions.visibility = View.VISIBLE
        }
    }

    /**
     * Actualizar visibilidad de sección de garantías
     */
    private fun updateWarrantiesVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            binding.recyclerWarranties.visibility = View.GONE
            binding.layoutEmptyWarranties.visibility = View.VISIBLE
            binding.buttonViewAllWarranties.visibility = View.GONE
        } else {
            binding.recyclerWarranties.visibility = View.VISIBLE
            binding.layoutEmptyWarranties.visibility = View.GONE
            binding.buttonViewAllWarranties.visibility = View.VISIBLE
        }
    }

    /**
     * Actualizar estado vacío general
     */
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.layoutContent.visibility = View.GONE
            binding.layoutEmpty.visibility = View.VISIBLE
        } else {
            binding.layoutContent.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
        }
    }

    /**
     * Abrir actividad para crear nuevo ítem
     */
    private fun openCreateItem(preselectedType: String? = null) {
        val intent = Intent(requireContext(), CreateEditItemActivity::class.java)
        preselectedType?.let {
            intent.putExtra("preselected_type", it)
        }
        startActivity(intent)
    }

    /**
     * Abrir actividad de backup
     */
    private fun openBackupActivity() {
        val intent = Intent(requireContext(), BackupActivity::class.java)
        startActivity(intent)
    }

    /**
     * Abrir configuración de notificaciones
     */
    private fun openNotificationSettings() {
        val intent = Intent(requireContext(), NotificationSettingsActivity::class.java)
        startActivity(intent)
    }

    /**
     * Abrir actividad para editar ítem existente
     */
    private fun openEditItem(itemId: Long) {
        val intent = Intent(requireContext(), CreateEditItemActivity::class.java)
        intent.putExtra("item_id", itemId)
        startActivity(intent)
    }

    /**
     * Mostrar confirmación de eliminación
     */
    private fun showDeleteConfirmation(item: com.koyeresolutions.ksexpire.data.entities.Item) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar ítem")
            .setMessage("¿Estás seguro de eliminar \"${item.name}\"?")
            .setPositiveButton("Eliminar") { _, _ ->
                viewModel.deleteItem(item)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /**
     * Mostrar mensaje de error
     */
    private fun showError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setAction("Reintentar") {
                viewModel.refreshData()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        // Refrescar datos cuando se regresa al fragment
        viewModel.refreshData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}