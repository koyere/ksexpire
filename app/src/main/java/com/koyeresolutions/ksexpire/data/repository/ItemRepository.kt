package com.koyeresolutions.ksexpire.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.koyeresolutions.ksexpire.data.database.ItemDao
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.services.NotificationService
import com.koyeresolutions.ksexpire.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

/**
 * Repository que actúa como capa de abstracción entre ViewModels y la base de datos
 * Implementa lógica de negocio y transformaciones de datos
 * INTEGRA SISTEMA DE NOTIFICACIONES
 */
class ItemRepository(
    private val itemDao: ItemDao,
    private val context: Context
) {
    
    private val notificationService = NotificationService(context)

    // ==================== OPERACIONES BÁSICAS ====================

    /**
     * Obtener todos los ítems activos
     */
    fun getAllActiveItems(): Flow<List<Item>> = itemDao.getAllActiveItems()

    /**
     * Obtener ítem por ID
     */
    suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)

    /**
     * Obtener ítem por ID como LiveData
     */
    fun getItemByIdLiveData(id: Long): LiveData<Item?> = itemDao.getItemByIdLiveData(id)

    // ==================== OPERACIONES POR TIPO ====================

    /**
     * Obtener suscripciones activas ordenadas por próximo vencimiento
     */
    fun getActiveSubscriptions(): Flow<List<Item>> = itemDao.getActiveSubscriptions()

    /**
     * Obtener garantías activas ordenadas por próximo vencimiento
     */
    fun getActiveWarranties(): Flow<List<Item>> = itemDao.getActiveWarranties()

    /**
     * Obtener garantías categorizadas por estado de vigencia
     */
    fun getWarrantiesByStatus(): Flow<WarrantyStatusGroup> {
        val currentTime = System.currentTimeMillis()
        val warningTime = currentTime + (30 * 24 * 60 * 60 * 1000L) // 30 días
        
        return itemDao.getWarrantiesByStatus(currentTime, warningTime).map { warranties ->
            WarrantyStatusGroup(
                expired = warranties.filter { it.isExpired() },
                expiringSoon = warranties.filter { it.isExpiringSoon() && !it.isExpired() },
                valid = warranties.filter { !it.isExpiringSoon() && !it.isExpired() }
            )
        }
    }

    // ==================== BÚSQUEDA ====================

    /**
     * Buscar ítems por nombre
     */
    fun searchItems(query: String): Flow<List<Item>> {
        return if (query.isBlank()) {
            getAllActiveItems()
        } else {
            itemDao.searchItems(query.trim())
        }
    }

    /**
     * Buscar ítems por nombre y tipo
     */
    fun searchItemsByType(query: String, type: Int): Flow<List<Item>> {
        return if (query.isBlank()) {
            itemDao.getItemsByType(type)
        } else {
            itemDao.searchItemsByType(query.trim(), type)
        }
    }

    // ==================== DASHBOARD Y ESTADÍSTICAS ====================

    /**
     * Calcular gasto mensual normalizado de suscripciones activas
     * IMPLEMENTA LA LÓGICA CRÍTICA DEL PLANNING
     */
    suspend fun calculateMonthlyExpense(): Double {
        val subscriptions = itemDao.getSubscriptionsForMonthlyCalculation()
        return subscriptions.sumOf { it.getNormalizedMonthlyPrice() }
    }

    /**
     * Obtener estadísticas del dashboard
     */
    suspend fun getDashboardStats(): DashboardStats {
        val monthlyExpense = calculateMonthlyExpense()
        val subscriptionsCount = itemDao.getActiveSubscriptionsCount()
        val warrantiesCount = itemDao.getActiveWarrantiesCount()
        val totalItemsCount = itemDao.getActiveItemsCount()
        
        return DashboardStats(
            monthlyExpense = monthlyExpense,
            subscriptionsCount = subscriptionsCount,
            warrantiesCount = warrantiesCount,
            totalItemsCount = totalItemsCount
        )
    }

    /**
     * Obtener ítems para el dashboard (limitados para rendimiento)
     */
    fun getDashboardItems(): Flow<DashboardItems> {
        return getAllActiveItems().map { items ->
            val subscriptions = items.filter { it.isSubscription() }.take(5) // Mostrar solo 5 más recientes
            val warranties = items.filter { it.isWarranty() }.take(5)
            
            DashboardItems(
                subscriptions = subscriptions,
                warranties = warranties,
                hasMoreSubscriptions = items.count { it.isSubscription() } > 5,
                hasMoreWarranties = items.count { it.isWarranty() } > 5
            )
        }
    }

    // ==================== NOTIFICACIONES ====================

    /**
     * Obtener suscripciones que requieren notificación
     */
    suspend fun getSubscriptionsForNotification(daysAhead: Int = Constants.DEFAULT_SUBSCRIPTION_REMINDER_DAYS): List<Item> {
        val now = System.currentTimeMillis()
        val targetTime = now + (daysAhead * 24 * 60 * 60 * 1000L)
        
        return itemDao.getSubscriptionsExpiringBetween(now, targetTime)
    }

    /**
     * Obtener garantías que requieren notificación
     */
    suspend fun getWarrantiesForNotification(daysAhead: Int): List<Item> {
        val now = System.currentTimeMillis()
        val targetTime = now + (daysAhead * 24 * 60 * 60 * 1000L)
        
        return itemDao.getWarrantiesExpiringBetween(now, targetTime)
    }

    /**
     * Obtener todos los ítems que requieren notificación
     */
    suspend fun getItemsForNotification(): NotificationItems {
        val subscriptions1Day = getSubscriptionsForNotification(1)
        val warranties30Days = getWarrantiesForNotification(30)
        val warranties7Days = getWarrantiesForNotification(7)
        
        return NotificationItems(
            subscriptions1Day = subscriptions1Day,
            warranties30Days = warranties30Days,
            warranties7Days = warranties7Days
        )
    }

    // ==================== OPERACIONES CRUD ====================

    /**
     * Insertar nuevo ítem con validación y notificaciones
     */
    suspend fun insertItem(item: Item): Result<Long> {
        return try {
            val errors = item.validate()
            if (errors.isNotEmpty()) {
                Result.failure(ValidationException(errors))
            } else {
                val updatedItem = item.copy(
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                val id = itemDao.insertItem(updatedItem)
                
                // Programar notificaciones para el nuevo ítem
                val itemWithId = updatedItem.copy(id = id)
                notificationService.scheduleItemNotifications(itemWithId)
                
                Result.success(id)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualizar ítem existente con validación y notificaciones
     */
    suspend fun updateItem(item: Item): Result<Unit> {
        return try {
            val errors = item.validate()
            if (errors.isNotEmpty()) {
                Result.failure(ValidationException(errors))
            } else {
                // Obtener ítem anterior para comparar cambios
                val oldItem = itemDao.getItemById(item.id)
                
                val updatedItem = item.copy(updatedAt = System.currentTimeMillis())
                itemDao.updateItem(updatedItem)
                
                // Reprogramar notificaciones si es necesario
                if (oldItem != null) {
                    notificationService.rescheduleItemNotifications(oldItem, updatedItem)
                } else {
                    notificationService.scheduleItemNotifications(updatedItem)
                }
                
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Eliminar ítem (eliminación lógica por defecto) y cancelar notificaciones
     */
    suspend fun deleteItem(item: Item, permanent: Boolean = false): Result<Unit> {
        return try {
            // Cancelar notificaciones antes de eliminar
            notificationService.cancelItemNotifications(item)
            
            if (permanent) {
                itemDao.deleteItem(item)
            } else {
                itemDao.deactivateItem(item.id)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reactivar ítem eliminado lógicamente
     */
    suspend fun reactivateItem(itemId: Long): Result<Unit> {
        return try {
            itemDao.reactivateItem(itemId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== BACKUP Y RESTORE ====================

    /**
     * Obtener todos los ítems para backup
     */
    suspend fun getAllItemsForBackup(): List<Item> = itemDao.getAllItemsForBackup()

    /**
     * Restaurar ítems desde backup
     */
    suspend fun restoreItemsFromBackup(items: List<Item>, clearExisting: Boolean = false): Result<Int> {
        return try {
            if (clearExisting) {
                itemDao.clearAllItems()
            }
            
            val validItems = items.filter { it.validate().isEmpty() }
            val insertedIds = itemDao.insertItems(validItems)
            
            Result.success(insertedIds.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MANTENIMIENTO ====================

    /**
     * Limpiar datos antiguos y optimizar base de datos
     */
    suspend fun performMaintenance(): MaintenanceResult {
        return try {
            val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
            
            // Limpiar ítems inactivos antiguos
            itemDao.cleanupOldInactiveItems(thirtyDaysAgo)
            
            // Obtener estadísticas después de limpieza
            val totalItems = itemDao.getTotalItemsCount()
            val estimatedSize = itemDao.getEstimatedDatabaseSize()
            
            MaintenanceResult(
                success = true,
                itemsRemaining = totalItems,
                estimatedDatabaseSize = estimatedSize,
                message = "Mantenimiento completado exitosamente"
            )
        } catch (e: Exception) {
            MaintenanceResult(
                success = false,
                itemsRemaining = 0,
                estimatedDatabaseSize = 0,
                message = "Error durante el mantenimiento: ${e.message}"
            )
        }
    }

    // ==================== CLASES DE DATOS ====================

    /**
     * Agrupación de garantías por estado
     */
    data class WarrantyStatusGroup(
        val expired: List<Item>,
        val expiringSoon: List<Item>,
        val valid: List<Item>
    )

    /**
     * Estadísticas del dashboard
     */
    data class DashboardStats(
        val monthlyExpense: Double,
        val subscriptionsCount: LiveData<Int>,
        val warrantiesCount: LiveData<Int>,
        val totalItemsCount: LiveData<Int>
    )

    /**
     * Ítems para mostrar en el dashboard
     */
    data class DashboardItems(
        val subscriptions: List<Item>,
        val warranties: List<Item>,
        val hasMoreSubscriptions: Boolean,
        val hasMoreWarranties: Boolean
    )

    /**
     * Ítems que requieren notificación
     */
    data class NotificationItems(
        val subscriptions1Day: List<Item>,
        val warranties30Days: List<Item>,
        val warranties7Days: List<Item>
    )

    /**
     * Resultado de operaciones de mantenimiento
     */
    data class MaintenanceResult(
        val success: Boolean,
        val itemsRemaining: Int,
        val estimatedDatabaseSize: Long,
        val message: String
    )

    /**
     * Excepción para errores de validación
     */
    class ValidationException(val errors: List<String>) : Exception(errors.joinToString(", "))
}