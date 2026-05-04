package com.koyeresolutions.ksexpire

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.work.Configuration
import androidx.work.WorkManager
import com.koyeresolutions.ksexpire.data.database.AppDatabase
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.notifications.NotificationChannels
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.FileUtils
import com.koyeresolutions.ksexpire.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Clase Application principal de KS Expire
 * Inicializa componentes globales y configuraciones necesarias
 */
class KSExpireApplication : Application() {

    // Base de datos y repositorio - Singleton
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { ItemRepository(database.itemDao(), this) }

    override fun onCreate() {
        super.onCreate()
        
        // Aplicar tema guardado antes de cualquier UI
        applyTheme()
        
        // Inicializar componentes críticos
        initializeNotificationChannels()
        initializeFileDirectories()
        initializeWorkManager()
    }

    /**
     * Aplicar tema según preferencia del usuario
     */
    private fun applyTheme() {
        val preferencesManager = PreferencesManager(this)
        val mode = when (preferencesManager.getThemeMode()) {
            Constants.THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            Constants.THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    /**
     * Crear canales de notificación requeridos para Android 8.0+
     */
    private fun initializeNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)
            
            // Canal para suscripciones
            val subscriptionChannel = NotificationChannel(
                NotificationChannels.SUBSCRIPTIONS_CHANNEL_ID,
                getString(R.string.notification_channel_subscriptions),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de próximos cobros de suscripciones"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Canal para garantías
            val warrantyChannel = NotificationChannel(
                NotificationChannels.WARRANTIES_CHANNEL_ID,
                getString(R.string.notification_channel_warranties),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorios de vencimiento de garantías"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Canal para pruebas gratuitas (prioridad alta)
            val freeTrialChannel = NotificationChannel(
                NotificationChannels.FREE_TRIAL_CHANNEL_ID,
                getString(R.string.notification_channel_free_trial),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de vencimiento de pruebas gratuitas"
                enableVibration(true)
                setShowBadge(true)
            }
            
            notificationManager.createNotificationChannels(
                listOf(subscriptionChannel, warrantyChannel, freeTrialChannel)
            )
        }
    }

    /**
     * Crear directorios necesarios para almacenamiento de archivos
     */
    private fun initializeFileDirectories() {
        CoroutineScope(Dispatchers.IO).launch {
            FileUtils.createAppDirectories(this@KSExpireApplication)
        }
    }

    /**
     * Configurar WorkManager para tareas en segundo plano
     */
    private fun initializeWorkManager() {
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
        
        WorkManager.initialize(this, config)
    }
}