package com.koyeresolutions.ksexpire.services

import android.content.Context
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.notifications.NotificationManager

/**
 * Servicio para gestionar notificaciones de ítems
 * Actúa como capa de abstracción entre Repository y NotificationManager
 */
class NotificationService(private val context: Context) {

    private val notificationManager = NotificationManager(context)

    /**
     * Programar notificaciones para un ítem nuevo o actualizado
     */
    fun scheduleItemNotifications(item: Item) {
        if (!item.isActive) {
            // Si el ítem está inactivo, cancelar notificaciones
            cancelItemNotifications(item)
            return
        }

        when {
            item.isSubscription() -> {
                notificationManager.scheduleSubscriptionNotification(item)
            }
            item.isWarranty() -> {
                notificationManager.scheduleWarrantyNotifications(item)
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
        // Cancelar notificaciones anteriores
        cancelItemNotifications(oldItem)
        
        // Programar nuevas notificaciones
        scheduleItemNotifications(newItem)
    }

    /**
     * Verificar y programar notificaciones inmediatas para ítems que vencen pronto
     */
    suspend fun checkImmediateNotifications(items: List<Item>) {
        val now = System.currentTimeMillis()
        
        items.filter { it.isActive }.forEach { item ->
            val daysUntilExpiry = item.getDaysUntilExpiry()
            
            when {
                // Suscripciones que vencen mañana
                item.isSubscription() && daysUntilExpiry == 1 -> {
                    notificationManager.showNotification(
                        item, 
                        NotificationManager.NotificationType.SUBSCRIPTION
                    )
                }
                
                // Garantías que vencen en 30 días
                item.isWarranty() && daysUntilExpiry == 30 -> {
                    notificationManager.showNotification(
                        item, 
                        NotificationManager.NotificationType.WARRANTY_30_DAYS
                    )
                }
                
                // Garantías que vencen en 7 días
                item.isWarranty() && daysUntilExpiry == 7 -> {
                    notificationManager.showNotification(
                        item, 
                        NotificationManager.NotificationType.WARRANTY_7_DAYS
                    )
                }
            }
        }
    }

    /**
     * Obtener estadísticas de notificaciones programadas
     */
    suspend fun getNotificationStats(items: List<Item>): NotificationStats {
        val activeItems = items.filter { it.isActive }
        val subscriptionsWithNotifications = activeItems.count { it.isSubscription() }
        val warrantiesWithNotifications = activeItems.count { it.isWarranty() }
        
        val upcomingNotifications = activeItems.count { item ->
            val days = item.getDaysUntilExpiry()
            when {
                item.isSubscription() -> days <= 1
                item.isWarranty() -> days <= 30
                else -> false
            }
        }
        
        return NotificationStats(
            totalScheduled = subscriptionsWithNotifications + (warrantiesWithNotifications * 2),
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