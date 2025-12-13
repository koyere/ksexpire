package com.koyeresolutions.ksexpire

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.work.Configuration
import androidx.work.WorkManager
import com.koyeresolutions.ksexpire.data.database.AppDatabase
import com.koyeresolutions.ksexpire.data.repository.ItemRepository
import com.koyeresolutions.ksexpire.notifications.NotificationChannels
import com.koyeresolutions.ksexpire.utils.FileUtils
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
        
        // Inicializar componentes críticos
        initializeNotificationChannels()
        initializeFileDirectories()
        initializeWorkManager()
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
            
            notificationManager.createNotificationChannels(
                listOf(subscriptionChannel, warrantyChannel)
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