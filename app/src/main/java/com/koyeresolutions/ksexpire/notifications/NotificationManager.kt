package com.koyeresolutions.ksexpire.notifications

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.koyeresolutions.ksexpire.MainActivity
import com.koyeresolutions.ksexpire.R
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.CurrencyUtils
import com.koyeresolutions.ksexpire.utils.DateUtils

/**
 * Manager para notificaciones locales
 * IMPLEMENTA LA FUNCIONALIDAD DE NOTIFICACIONES DEL PLANNING
 */
class NotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val SUBSCRIPTION_NOTIFICATION_ID_BASE = 1000
        private const val WARRANTY_NOTIFICATION_ID_BASE = 2000
    }

    /**
     * Programar notificación para suscripción
     * Notifica 1 día antes del cobro (configurable)
     */
    fun scheduleSubscriptionNotification(item: Item, daysBefore: Int = Constants.DEFAULT_SUBSCRIPTION_REMINDER_DAYS) {
        if (!item.isSubscription() || !item.isActive) return

        val notificationTime = item.expiryDate - (daysBefore * 24 * 60 * 60 * 1000L)
        
        // No programar si ya pasó la fecha
        if (notificationTime <= System.currentTimeMillis()) return

        val intent = createNotificationIntent(item, NotificationType.SUBSCRIPTION)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(item, NotificationType.SUBSCRIPTION),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar alarma exacta
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }

    /**
     * Programar notificaciones para garantía
     * Notifica 30 días y 7 días antes del vencimiento
     */
    fun scheduleWarrantyNotifications(item: Item) {
        if (!item.isWarranty() || !item.isActive) return

        // Notificación 30 días antes
        scheduleWarrantyNotification(item, 30, NotificationType.WARRANTY_30_DAYS)
        
        // Notificación 7 días antes
        scheduleWarrantyNotification(item, 7, NotificationType.WARRANTY_7_DAYS)
    }

    /**
     * Programar notificación individual para garantía
     */
    private fun scheduleWarrantyNotification(item: Item, daysBefore: Int, type: NotificationType) {
        val notificationTime = item.expiryDate - (daysBefore * 24 * 60 * 60 * 1000L)
        
        // No programar si ya pasó la fecha
        if (notificationTime <= System.currentTimeMillis()) return

        val intent = createNotificationIntent(item, type)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(item, type),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Programar alarma exacta
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                notificationTime,
                pendingIntent
            )
        }
    }

    /**
     * Cancelar todas las notificaciones de un ítem
     */
    fun cancelItemNotifications(item: Item) {
        // Cancelar notificación de suscripción
        if (item.isSubscription()) {
            cancelNotification(item, NotificationType.SUBSCRIPTION)
        }
        
        // Cancelar notificaciones de garantía
        if (item.isWarranty()) {
            cancelNotification(item, NotificationType.WARRANTY_30_DAYS)
            cancelNotification(item, NotificationType.WARRANTY_7_DAYS)
        }
    }

    /**
     * Cancelar notificación específica
     */
    private fun cancelNotification(item: Item, type: NotificationType) {
        val intent = createNotificationIntent(item, type)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getNotificationId(item, type),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    /**
     * Mostrar notificación inmediata
     */
    fun showNotification(item: Item, type: NotificationType) {
        val notification = when (type) {
            NotificationType.SUBSCRIPTION -> createSubscriptionNotification(item)
            NotificationType.WARRANTY_30_DAYS -> createWarrantyNotification(item, 30)
            NotificationType.WARRANTY_7_DAYS -> createWarrantyNotification(item, 7)
        }

        notificationManager.notify(
            getNotificationId(item, type),
            notification.build()
        )
    }

    /**
     * Crear notificación para suscripción
     */
    private fun createSubscriptionNotification(item: Item): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_subscription_title, item.name)
        val text = if (item.price != null) {
            context.getString(
                R.string.notification_subscription_text,
                CurrencyUtils.formatPrice(context, item.price)
            )
        } else {
            "Se renovará mañana"
        }

        return NotificationCompat.Builder(context, NotificationChannels.SUBSCRIPTIONS_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_subscription)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    /**
     * Crear notificación para garantía
     */
    private fun createWarrantyNotification(item: Item, daysBefore: Int): NotificationCompat.Builder {
        val title = context.getString(R.string.notification_warranty_title, item.name)
        val text = context.getString(R.string.notification_warranty_text, daysBefore)

        return NotificationCompat.Builder(context, NotificationChannels.WARRANTIES_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_warranty)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(createMainActivityIntent())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
    }

    /**
     * Crear intent para notificación
     */
    private fun createNotificationIntent(item: Item, type: NotificationType): Intent {
        return Intent(context, NotificationReceiver::class.java).apply {
            putExtra("item_id", item.id)
            putExtra("notification_type", type.ordinal)
            action = "com.koyeresolutions.ksexpire.NOTIFICATION_ACTION"
        }
    }

    /**
     * Crear intent para abrir la app principal
     */
    private fun createMainActivityIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    /**
     * Generar ID único para notificación
     */
    private fun getNotificationId(item: Item, type: NotificationType): Int {
        return when (type) {
            NotificationType.SUBSCRIPTION -> SUBSCRIPTION_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.WARRANTY_30_DAYS -> WARRANTY_NOTIFICATION_ID_BASE + item.id.toInt()
            NotificationType.WARRANTY_7_DAYS -> WARRANTY_NOTIFICATION_ID_BASE + item.id.toInt() + 10000
        }
    }

    /**
     * Reprogramar todas las notificaciones activas
     * Usado después de reinicio del sistema
     */
    suspend fun rescheduleAllNotifications(items: List<Item>) {
        items.filter { it.isActive }.forEach { item ->
            when {
                item.isSubscription() -> scheduleSubscriptionNotification(item)
                item.isWarranty() -> scheduleWarrantyNotifications(item)
            }
        }
    }

    /**
     * Tipos de notificación
     */
    enum class NotificationType {
        SUBSCRIPTION,
        WARRANTY_30_DAYS,
        WARRANTY_7_DAYS
    }
}