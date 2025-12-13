package com.koyeresolutions.ksexpire.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.utils.Constants
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object para la entidad Item
 * Contiene todas las consultas necesarias para la funcionalidad de la app
 */
@Dao
interface ItemDao {

    // ==================== CONSULTAS BÁSICAS ====================

    /**
     * Obtener todos los ítems activos
     */
    @Query("SELECT * FROM items WHERE isActive = 1 ORDER BY updatedAt DESC")
    fun getAllActiveItems(): Flow<List<Item>>

    /**
     * Obtener todos los ítems (incluyendo inactivos) para backup
     */
    @Query("SELECT * FROM items ORDER BY createdAt DESC")
    fun getAllItems(): Flow<List<Item>>

    /**
     * Obtener ítem por ID
     */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): Item?

    /**
     * Obtener ítem por ID como LiveData (para observar cambios)
     */
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemByIdLiveData(id: Long): LiveData<Item?>

    // ==================== CONSULTAS POR TIPO ====================

    /**
     * Obtener todas las suscripciones activas
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_SUBSCRIPTION} 
        AND isActive = 1 
        ORDER BY expiryDate ASC
    """)
    fun getActiveSubscriptions(): Flow<List<Item>>

    /**
     * Obtener todas las garantías activas
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_WARRANTY} 
        AND isActive = 1 
        ORDER BY expiryDate ASC
    """)
    fun getActiveWarranties(): Flow<List<Item>>

    /**
     * Obtener ítems por tipo (genérico)
     */
    @Query("SELECT * FROM items WHERE type = :type AND isActive = 1 ORDER BY expiryDate ASC")
    fun getItemsByType(type: Int): Flow<List<Item>>

    // ==================== BÚSQUEDA ====================

    /**
     * Buscar ítems por nombre (case-insensitive)
     */
    @Query("""
        SELECT * FROM items 
        WHERE name LIKE '%' || :query || '%' 
        AND isActive = 1 
        ORDER BY 
            CASE WHEN name LIKE :query || '%' THEN 1 ELSE 2 END,
            name ASC
    """)
    fun searchItems(query: String): Flow<List<Item>>

    /**
     * Buscar ítems por nombre y tipo
     */
    @Query("""
        SELECT * FROM items 
        WHERE name LIKE '%' || :query || '%' 
        AND type = :type 
        AND isActive = 1 
        ORDER BY name ASC
    """)
    fun searchItemsByType(query: String, type: Int): Flow<List<Item>>

    // ==================== CONSULTAS PARA DASHBOARD ====================

    /**
     * Obtener suscripciones activas para cálculo de gasto mensual
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_SUBSCRIPTION} 
        AND isActive = 1 
        AND price IS NOT NULL
    """)
    suspend fun getSubscriptionsForMonthlyCalculation(): List<Item>

    /**
     * Contar total de ítems activos
     */
    @Query("SELECT COUNT(*) FROM items WHERE isActive = 1")
    fun getActiveItemsCount(): LiveData<Int>

    /**
     * Contar suscripciones activas
     */
    @Query("""
        SELECT COUNT(*) FROM items 
        WHERE type = ${Constants.ITEM_TYPE_SUBSCRIPTION} 
        AND isActive = 1
    """)
    fun getActiveSubscriptionsCount(): LiveData<Int>

    /**
     * Contar garantías activas
     */
    @Query("""
        SELECT COUNT(*) FROM items 
        WHERE type = ${Constants.ITEM_TYPE_WARRANTY} 
        AND isActive = 1
    """)
    fun getActiveWarrantiesCount(): LiveData<Int>

    // ==================== CONSULTAS PARA NOTIFICACIONES ====================

    /**
     * Obtener suscripciones que vencen en los próximos N días
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_SUBSCRIPTION} 
        AND isActive = 1 
        AND expiryDate BETWEEN :startTime AND :endTime
        ORDER BY expiryDate ASC
    """)
    suspend fun getSubscriptionsExpiringBetween(startTime: Long, endTime: Long): List<Item>

    /**
     * Obtener garantías que vencen en los próximos N días
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_WARRANTY} 
        AND isActive = 1 
        AND expiryDate BETWEEN :startTime AND :endTime
        ORDER BY expiryDate ASC
    """)
    suspend fun getWarrantiesExpiringBetween(startTime: Long, endTime: Long): List<Item>

    /**
     * Obtener ítems vencidos (para limpieza o notificaciones)
     */
    @Query("""
        SELECT * FROM items 
        WHERE isActive = 1 
        AND expiryDate < :currentTime
        ORDER BY expiryDate DESC
    """)
    suspend fun getExpiredItems(currentTime: Long): List<Item>

    // ==================== CONSULTAS PARA ESTADÍSTICAS ====================

    /**
     * Obtener garantías por estado de vigencia
     */
    @Query("""
        SELECT * FROM items 
        WHERE type = ${Constants.ITEM_TYPE_WARRANTY} 
        AND isActive = 1 
        ORDER BY 
            CASE 
                WHEN expiryDate < :currentTime THEN 0
                WHEN expiryDate < :warningTime THEN 1
                ELSE 2
            END,
            expiryDate ASC
    """)
    fun getWarrantiesByStatus(currentTime: Long, warningTime: Long): Flow<List<Item>>

    /**
     * Obtener ítems creados recientemente (últimos 7 días)
     */
    @Query("""
        SELECT * FROM items 
        WHERE createdAt >= :weekAgo 
        ORDER BY createdAt DESC
    """)
    suspend fun getRecentItems(weekAgo: Long): List<Item>

    // ==================== OPERACIONES CRUD ====================

    /**
     * Insertar nuevo ítem
     * @return ID del ítem insertado
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: Item): Long

    /**
     * Insertar múltiples ítems (para restore de backup)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<Item>): List<Long>

    /**
     * Actualizar ítem existente
     */
    @Update
    suspend fun updateItem(item: Item)

    /**
     * Actualizar múltiples ítems
     */
    @Update
    suspend fun updateItems(items: List<Item>)

    /**
     * Eliminar ítem (eliminación física)
     */
    @Delete
    suspend fun deleteItem(item: Item)

    /**
     * Eliminar ítem por ID
     */
    @Query("DELETE FROM items WHERE id = :id")
    suspend fun deleteItemById(id: Long)

    /**
     * Marcar ítem como inactivo (eliminación lógica)
     */
    @Query("UPDATE items SET isActive = 0, updatedAt = :timestamp WHERE id = :id")
    suspend fun deactivateItem(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Reactivar ítem
     */
    @Query("UPDATE items SET isActive = 1, updatedAt = :timestamp WHERE id = :id")
    suspend fun reactivateItem(id: Long, timestamp: Long = System.currentTimeMillis())

    // ==================== OPERACIONES DE MANTENIMIENTO ====================

    /**
     * Limpiar ítems inactivos antiguos (más de 30 días)
     */
    @Query("""
        DELETE FROM items 
        WHERE isActive = 0 
        AND updatedAt < :cutoffTime
    """)
    suspend fun cleanupOldInactiveItems(cutoffTime: Long)

    /**
     * Actualizar timestamp de última modificación
     */
    @Query("UPDATE items SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: Long, timestamp: Long = System.currentTimeMillis())

    /**
     * Contar total de ítems en la base de datos
     */
    @Query("SELECT COUNT(*) FROM items")
    suspend fun getTotalItemsCount(): Int

    /**
     * Obtener el tamaño aproximado de la base de datos
     */
    @Query("SELECT COUNT(*) * 1024 FROM items") // Estimación aproximada
    suspend fun getEstimatedDatabaseSize(): Long

    // ==================== CONSULTAS PARA BACKUP/RESTORE ====================

    /**
     * Obtener todos los ítems para backup (sin Flow para operación única)
     */
    @Query("SELECT * FROM items ORDER BY createdAt ASC")
    suspend fun getAllItemsForBackup(): List<Item>

    /**
     * Limpiar toda la tabla (para restore completo)
     */
    @Query("DELETE FROM items")
    suspend fun clearAllItems()

    /**
     * Verificar si existe un ítem con el mismo nombre y fechas (para evitar duplicados en restore)
     */
    @Query("""
        SELECT COUNT(*) FROM items 
        WHERE name = :name 
        AND purchaseDate = :purchaseDate 
        AND expiryDate = :expiryDate
    """)
    suspend fun checkDuplicateItem(name: String, purchaseDate: Long, expiryDate: Long): Int
}