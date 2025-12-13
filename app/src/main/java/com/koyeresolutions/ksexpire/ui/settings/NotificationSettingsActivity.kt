package com.koyeresolutions.ksexpire.ui.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.koyeresolutions.ksexpire.databinding.ActivityNotificationSettingsBinding
import com.koyeresolutions.ksexpire.services.NotificationService
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import kotlinx.coroutines.launch

/**
 * Actividad para configurar notificaciones y preferencias
 * IMPLEMENTA CONFIGURACIÓN DE MONEDA DEL PLANNING
 */
class NotificationSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationSettingsBinding
    private lateinit var preferences: SharedPreferences
    private lateinit var notificationService: NotificationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityNotificationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupToolbar()
        initializeServices()
        setupUI()
        loadCurrentSettings()
    }

    /**
     * Configurar toolbar
     */
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = "Configuración"
    }

    /**
     * Inicializar servicios
     */
    private fun initializeServices() {
        preferences = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        notificationService = NotificationService(this)
    }

    /**
     * Configurar elementos de UI
     */
    private fun setupUI() {
        // Configurar spinner de moneda
        setupCurrencySpinner()
        
        // Configurar switches de notificaciones
        setupNotificationSwitches()
        
        // Botón guardar
        binding.buttonSave.setOnClickListener {
            saveSettings()
        }
    }

    /**
     * Configurar spinner de moneda
     */
    private fun setupCurrencySpinner() {
        val currencies = CurrencyUtils.getSupportedCurrencies()
        val adapter = android.widget.ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            currencies.map { it.name }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCurrency.adapter = adapter
    }

    /**
     * Configurar switches de notificaciones
     */
    private fun setupNotificationSwitches() {
        // Los switches ya están configurados en el layout
        // Solo necesitamos manejar los listeners en saveSettings()
    }

    /**
     * Cargar configuración actual
     */
    private fun loadCurrentSettings() {
        // Cargar configuración de moneda
        val currentSymbol = CurrencyUtils.getCurrencySymbol(this)
        val currencies = CurrencyUtils.getSupportedCurrencies()
        val currentIndex = currencies.indexOfFirst { it.symbol == currentSymbol }
        if (currentIndex >= 0) {
            binding.spinnerCurrency.setSelection(currentIndex)
        }

        // Cargar configuración de notificaciones
        binding.switchSubscriptionNotifications.isChecked = 
            preferences.getBoolean("notifications_subscriptions_enabled", true)
        
        binding.switchWarrantyNotifications30.isChecked = 
            preferences.getBoolean("notifications_warranty_30_enabled", true)
            
        binding.switchWarrantyNotifications7.isChecked = 
            preferences.getBoolean("notifications_warranty_7_enabled", true)

        // Mostrar estadísticas de notificaciones
        loadNotificationStats()
    }

    /**
     * Cargar estadísticas de notificaciones
     */
    private fun loadNotificationStats() {
        lifecycleScope.launch {
            try {
                // Aquí podrías cargar estadísticas reales
                // Por ahora mostramos información estática
                binding.textNotificationStats.text = "Configuración de recordatorios para tus ítems"
            } catch (e: Exception) {
                binding.textNotificationStats.text = "Error al cargar estadísticas"
            }
        }
    }

    /**
     * Guardar configuración
     */
    private fun saveSettings() {
        val editor = preferences.edit()

        // Guardar configuración de moneda
        val selectedCurrency = CurrencyUtils.getSupportedCurrencies()[binding.spinnerCurrency.selectedItemPosition]
        if (selectedCurrency.code == "AUTO") {
            CurrencyUtils.resetToAutoCurrency(this)
        } else {
            CurrencyUtils.setCurrencySymbol(this, selectedCurrency.symbol)
        }

        // Guardar configuración de notificaciones
        editor.putBoolean("notifications_subscriptions_enabled", binding.switchSubscriptionNotifications.isChecked)
        editor.putBoolean("notifications_warranty_30_enabled", binding.switchWarrantyNotifications30.isChecked)
        editor.putBoolean("notifications_warranty_7_enabled", binding.switchWarrantyNotifications7.isChecked)

        editor.apply()

        // Mostrar confirmación y cerrar
        android.widget.Toast.makeText(this, "Configuración guardada", android.widget.Toast.LENGTH_SHORT).show()
        finish()
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