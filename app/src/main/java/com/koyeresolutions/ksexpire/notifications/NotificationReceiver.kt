package com.koyeresolutions.ksexpire.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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
        val itemId = intent.getLongExtra(NotificationManager.EXTRA_ITEM_ID, -1L)
        val notificationTypeOrdinal = intent.getIntExtra(NotificationManager.EXTRA_NOTIFICATION_TYPE, -1)
        val dueDate = intent.getLongExtra(NotificationManager.EXTRA_DUE_DATE, 0L)

        if (itemId == -1L || notificationTypeOrdinal == -1) return

        val notificationType = NotificationManager.NotificationType.values().getOrNull(notificationTypeOrdinal)
            ?: return

        // Usar goAsync() para mantener el BroadcastReceiver vivo mientras la coroutine trabaja
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                processNotification(context, itemId, notificationType, dueDate)
            } catch (e: Exception) {
                Log.e("NotificationReceiver", "Error al procesar notificación", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    /**
     * Procesar y mostrar notificación
     */
    private suspend fun processNotification(
        context: Context,
        itemId: Long,
        notificationType: NotificationManager.NotificationType,
        alarmDueDate: Long
    ) {
        val item = AppDatabase.getDatabase(context).itemDao().getItemById(itemId) ?: return
        if (!item.isActive) return

        val notificationManager = NotificationManager(context)
        val dueDate = notificationManager.dueDateFor(item, notificationType)

        // Ignorar alarmas de un vencimiento que el usuario ya cambió
        if (alarmDueDate != 0L &&
            NotificationManager.startOfDay(alarmDueDate) != NotificationManager.startOfDay(dueDate)
        ) return

        if (!notificationManager.wasSent(item, notificationType, dueDate)) {
            notificationManager.showNotification(item, notificationType, dueDate)
        }

        // Suscripción: dejar programado el recordatorio del siguiente ciclo.
        // La fecha guardada se avanza al abrir la app / en la revisión diaria, una vez pasado el cobro.
        if (item.isSubscription() && notificationType == NotificationManager.NotificationType.SUBSCRIPTION) {
            item.getNextBillingDate()?.let { next ->
                notificationManager.scheduleSubscriptionNotification(item.copy(expiryDate = next))
            }
        }
    }
}
