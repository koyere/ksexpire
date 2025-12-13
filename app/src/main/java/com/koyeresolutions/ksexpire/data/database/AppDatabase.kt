package com.koyeresolutions.ksexpire.data.database

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.utils.Constants

/**
 * Base de datos principal de KS Expire
 * Implementa Room Database con configuraciones de seguridad y rendimiento
 */
@Database(
    entities = [Item::class],
    version = Constants.DATABASE_VERSION,
    exportSchema = true // Para generar esquemas de migración
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    /**
     * DAO para acceso a datos de ítems
     */
    abstract fun itemDao(): ItemDao

    companion object {
        // Singleton instance
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtener instancia singleton de la base de datos
         * Thread-safe con double-checked locking
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2) // Preparado para futuras migraciones
                    .fallbackToDestructiveMigration() // Solo para desarrollo
                    .build()
                
                INSTANCE = instance
                instance
            }
        }

        /**
         * Callback para inicialización de la base de datos
         */
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                
                // Crear índices adicionales para optimizar consultas
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_items_type_active 
                    ON items(type, isActive)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_items_expiry_date 
                    ON items(expiryDate)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_items_name 
                    ON items(name)
                """)
                
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_items_created_at 
                    ON items(createdAt)
                """)
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                
                // Configuraciones de rendimiento
                db.execSQL("PRAGMA foreign_keys=ON")
                db.execSQL("PRAGMA journal_mode=WAL") // Write-Ahead Logging para mejor concurrencia
                db.execSQL("PRAGMA synchronous=NORMAL") // Balance entre seguridad y rendimiento
                db.execSQL("PRAGMA cache_size=10000") // Cache de 10MB aproximadamente
                db.execSQL("PRAGMA temp_store=MEMORY") // Tablas temporales en memoria
            }
        }

        /**
         * Migración de versión 1 a 2 (ejemplo para futuras actualizaciones)
         * Actualmente no se usa, pero está preparada para cuando sea necesaria
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ejemplo de migración futura:
                // database.execSQL("ALTER TABLE items ADD COLUMN newColumn TEXT")
            }
        }

        /**
         * Cerrar la base de datos (para testing o casos especiales)
         */
        fun closeDatabase() {
            INSTANCE?.close()
            INSTANCE = null
        }

        /**
         * Verificar si la base de datos está abierta
         */
        fun isDatabaseOpen(): Boolean {
            return INSTANCE?.isOpen == true
        }

        /**
         * Obtener información de la base de datos para debugging
         */
        suspend fun getDatabaseInfo(context: Context): DatabaseInfo {
            val db = getDatabase(context)
            val itemCount = db.itemDao().getTotalItemsCount()
            val dbFile = context.getDatabasePath(Constants.DATABASE_NAME)
            val dbSize = if (dbFile.exists()) dbFile.length() else 0L
            
            return DatabaseInfo(
                itemCount = itemCount,
                databaseSize = dbSize,
                databasePath = dbFile.absolutePath,
                isOpen = isDatabaseOpen()
            )
        }
    }

    /**
     * Información de la base de datos para debugging y estadísticas
     */
    data class DatabaseInfo(
        val itemCount: Int,
        val databaseSize: Long,
        val databasePath: String,
        val isOpen: Boolean
    )
}