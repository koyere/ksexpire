package com.koyeresolutions.ksexpire.services

import android.content.Context
import com.koyeresolutions.ksexpire.data.database.AppDatabase
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.notifications.NotificationManager
import com.koyeresolutions.ksexpire.utils.Constants

/**
 * Servicio para gestionar notificaciones de ítems
 * Actúa como capa de abstracción entre Repository y NotificationManager
 * Respeta las preferencias del usuario para cada tipo de notificación
 */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManager(context)

    private val preferences by lazy {
        context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Verificar si las notificaciones de suscripciones están habilitadas
     */
    private fun isSubscriptionNotificationsEnabled(): Boolean {
        return preferences.getBoolean("notifications_subscriptions_enabled", true)
    }

    /**
     * Verificar si las notificaciones de garantía a 30 días están habilitadas
     */
    private fun isWarranty30NotificationsEnabled(): Boolean {
        return preferences.getBoolean("notifications_warranty_30_enabled", true)
    }

    /**
     * Verificar si las notificaciones de garantía a 7 días están habilitadas
     */
    private fun isWarranty7NotificationsEnabled(): Boolean {
        return preferences.getBoolean("notifications_warranty_7_enabled", true)
    }

    /**
     * Programar notificaciones para un ítem nuevo o actualizado
     * Respeta las preferencias del usuario (cancela los tipos deshabilitados)
     */
    fun scheduleItemNotifications(item: Item) {
        if (!item.isActive) {
            cancelItemNotifications(item)
            return
        }

        // Prueba gratuita: avisos propios, además de los del tipo de ítem
        if (item.isFreeTrial && item.freeTrialEndDate != null) {
            notificationManager.scheduleFreeTrialNotifications(item)
        } else {
            notificationManager.cancelFreeTrialNotifications(item)
        }

        when {
            item.isSubscription() -> {
                if (isSubscriptionNotificationsEnabled()) {
                    notificationManager.scheduleSubscriptionNotification(item)
                } else {
                    notificationManager.cancelSubscriptionNotification(item)
                }
            }
            item.isWarranty() -> {
                // Cancelar primero para eliminar el aviso que se haya deshabilitado
                notificationManager.cancelWarrantyNotifications(item)
                notificationManager.scheduleWarrantyNotifications(
                    item,
                    schedule30Days = isWarranty30NotificationsEnabled(),
                    schedule7Days = isWarranty7NotificationsEnabled()
                )
            }
        }
    }

    /**
     * Cancelar notificaciones de un ítem
     */
    fun cancelItemNotifications(item: Item) {
        notificationManager.cancelItemNotifications(item)
    }

    /**
     * Reprogramar notificaciones después de cambios
     */
    fun rescheduleItemNotifications(oldItem: Item, newItem: Item) {
        cancelItemNotifications(oldItem)
        scheduleItemNotifications(newItem)
    }

    /**
     * Reprogramar todas las notificaciones a partir de la base de datos.
     * Se ejecuta al abrir la app, tras reinicio/actualización y una vez al día.
     * - Avanza al siguiente ciclo las suscripciones cuya fecha de cobro ya pasó
     * - Programa alarmas futuras y muestra los recordatorios que se hayan perdido
     */
    suspend fun rescheduleAllNotifications() {
        val dao = AppDatabase.getDatabase(context).itemDao()
        val startOfToday = NotificationManager.startOfDay(System.currentTimeMillis())

        dao.getAllItemsForBackup().filter { it.isActive }.forEach { item ->
            val current = if (item.isSubscription() && item.expiryDate < startOfToday) {
                advanceSubscription(item, startOfToday)?.also { dao.updateItem(it) } ?: item
            } else {
                item
            }
            scheduleItemNotifications(current)
        }

        notificationManager.pruneSentRecords()
    }

    /**
     * Avanzar la fecha de cobro de una suscripción hasta hoy o después
     */
    private fun advanceSubscription(item: Item, startOfToday: Long): Item? {
        var updated = item
        while (updated.expiryDate < startOfToday) {
            val next = updated.getNextBillingDate() ?: return null
            updated = updated.copy(expiryDate = next)
        }
        return updated.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * Obtener estadísticas de notificaciones programadas
     */
    suspend fun getNotificationStats(items: List<Item>): NotificationStats {
        val activeItems = items.filter { it.isActive }
        val subscriptionsWithNotifications = if (isSubscriptionNotificationsEnabled()) {
            activeItems.count { it.isSubscription() }
        } else 0
        val warrantiesWithNotifications = activeItems.count { it.isWarranty() }
        val warrantyNotificationsPerItem = (if (isWarranty30NotificationsEnabled()) 1 else 0) +
                (if (isWarranty7NotificationsEnabled()) 1 else 0)

        val upcomingNotifications = activeItems.count { item ->
            val days = item.getDaysUntilExpiry()
            when {
                item.isSubscription() && isSubscriptionNotificationsEnabled() -> days <= 1
                item.isWarranty() && (isWarranty30NotificationsEnabled() || isWarranty7NotificationsEnabled()) -> days <= 30
                else -> false
            }
        }

        return NotificationStats(
            totalScheduled = subscriptionsWithNotifications + (warrantiesWithNotifications * warrantyNotificationsPerItem),
            subscriptionsScheduled = subscriptionsWithNotifications,
            warrantiesScheduled = warrantiesWithNotifications,
            upcomingNotifications = upcomingNotifications
        )
    }

    /**
     * Estadísticas de notificaciones
     */
    data class NotificationStats(
        val totalScheduled: Int,
        val subscriptionsScheduled: Int,
        val warrantiesScheduled: Int,
        val upcomingNotifications: Int
    )
}
