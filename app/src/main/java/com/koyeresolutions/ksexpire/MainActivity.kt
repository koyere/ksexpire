package com.koyeresolutions.ksexpire

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.koyeresolutions.ksexpire.databinding.ActivityMainBinding
import com.koyeresolutions.ksexpire.notifications.NotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Actividad principal que contiene la navegación por tabs
 * Dashboard, Búsqueda y Acerca de
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Launcher para solicitar permiso de notificaciones (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permiso concedido, verificar alarmas exactas
            checkExactAlarmPermission()
        }
        // Si no se concede, la app funciona pero sin notificaciones
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupNavigation()
        requestNotificationPermissions()
        rescheduleNotificationsOnStartup()
    }

    /**
     * Configurar navegación con Bottom Navigation
     */
    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)
    }

    /**
     * Solicitar permisos de notificación necesarios
     */
    private fun requestNotificationPermissions() {
        // Android 13+ requiere permiso POST_NOTIFICATIONS en tiempo de ejecución
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Ya tiene permiso, verificar alarmas exactas
                    checkExactAlarmPermission()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Mostrar explicación antes de pedir permiso
                    MaterialAlertDialogBuilder(this)
                        .setTitle(R.string.notification_permission_title)
                        .setMessage(R.string.notification_permission_rationale)
                        .setPositiveButton(R.string.action_ok) { _, _ ->
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show()
                }
                else -> {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 y anteriores no necesitan permiso POST_NOTIFICATIONS
            checkExactAlarmPermission()
        }
    }

    /**
     * Verificar permiso de alarmas exactas (Android 12+)
     */
    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.exact_alarm_permission_title)
                    .setMessage(R.string.exact_alarm_permission_rationale)
                    .setPositiveButton(R.string.action_ok) { _, _ ->
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = Uri.parse("package:$packageName")
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton(R.string.dialog_cancel, null)
                    .show()
            }
        }
    }

    /**
     * Reprogramar notificaciones al abrir la app
     * Actúa como red de seguridad para notificaciones perdidas
     */
    private fun rescheduleNotificationsOnStartup() {
        val app = application as KSExpireApplication
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val items = app.database.itemDao().getAllItemsForBackup()
                val notificationManager = NotificationManager(this@MainActivity)
                notificationManager.rescheduleAllNotifications(items)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}