package com.koyeresolutions.ksexpire.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.koyeresolutions.ksexpire.data.database.AppDatabase

/**
 * Receptor de notificaciones programadas
 * Maneja las alarmas y muestra las notificaciones correspondientes
 */
class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getLongExtra("item_id", -1L)
        val notificationTypeOrdinal = intent.getIntExtra("notification_type", -1)
        
        if (itemId == -1L || notificationTypeOrdinal == -1) return

        val notificationType = NotificationManager.NotificationType.values().getOrNull(notificationTypeOrdinal)
            ?: return

        // Procesar notificación en corrutina
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            processNotification(context, itemId, notificationType)
        }
    }

    /**
     * Procesar y mostrar notificación
     */
    private suspend fun processNotification(
        context: Context,
        itemId: Long,
        notificationType: NotificationManager.NotificationType
    ) {
        try {
            val database = AppDatabase.getDatabase(context)
            val item = database.itemDao().getItemById(itemId)
            
            // Verificar que el ítem existe y está activo
            if (item != null && item.isActive) {
                val notificationManager = NotificationManager(context)
                notificationManager.showNotification(item, notificationType)
                
                // Reprogramar próxima notificación si es suscripción
                if (item.isSubscription() && notificationType == NotificationManager.NotificationType.SUBSCRIPTION) {
                    // Actualizar fecha de próximo cobro
                    val nextBillingDate = item.getNextBillingDate()
                    if (nextBillingDate != null) {
                        val updatedItem = item.copy(
                            expiryDate = nextBillingDate,
                            updatedAt = System.currentTimeMillis()
                        )
                        database.itemDao().updateItem(updatedItem)
                        
                        // Programar siguiente notificación
                        notificationManager.scheduleSubscriptionNotification(updatedItem)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}