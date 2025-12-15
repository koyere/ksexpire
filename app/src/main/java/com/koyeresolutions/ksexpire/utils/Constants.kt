package com.koyeresolutions.ksexpire.utils

/**
 * Constantes globales de la aplicación
 */
object Constants {
    
    // Base de datos
    const val DATABASE_NAME = "ks_expire_database"
    const val DATABASE_VERSION = 1
    
    // Tipos de ítems
    const val ITEM_TYPE_WARRANTY = 0
    const val ITEM_TYPE_SUBSCRIPTION = 1
    
    // Frecuencias de cobro
    const val FREQUENCY_WEEKLY = "WEEKLY"
    const val FREQUENCY_MONTHLY = "MONTHLY"
    const val FREQUENCY_ANNUAL = "ANNUAL"
    
    // Directorios de archivos
    const val RECEIPTS_DIR = "receipts"
    const val BACKUPS_DIR = "backups"
    const val TEMP_DIR = "temp"
    
    // Formatos de archivo
    const val IMAGE_EXTENSION = ".jpg"
    const val BACKUP_EXTENSION = ".zip"
    
    // Configuración de imágenes
    const val IMAGE_QUALITY = 75 // Compresión JPEG 75%
    const val MAX_IMAGE_WIDTH = 1024
    const val MAX_IMAGE_HEIGHT = 1024
    
    // Notificaciones
    const val NOTIFICATION_REQUEST_CODE_BASE = 1000
    const val DEFAULT_SUBSCRIPTION_REMINDER_DAYS = 1
    const val DEFAULT_WARRANTY_REMINDER_DAYS_1 = 30
    const val DEFAULT_WARRANTY_REMINDER_DAYS_2 = 7
    
    // Backup
    const val BACKUP_MIME_TYPE = "application/zip"
    const val BACKUP_FILE_PREFIX = "ks_expire_backup_"
    
    // Review
    const val REVIEW_TRIGGER_ITEM_COUNT = 3
    
    // Enlaces del desarrollador
    const val DEVELOPER_WEBSITE = "https://www.koyeresolutions.com/"
    const val DEVELOPER_LINKEDIN = "https://www.linkedin.com/in/eduardo-escobar-38a888161/"
    const val DEVELOPER_GITHUB = "https://github.com/koyere"
    const val DEVELOPER_DISCORD = "https://discord.gg/xKUjn3EJzR"
    const val DEVELOPER_EMAIL = "info@koyeresolutions.com"
    
    // Conversiones de tiempo
    const val DAYS_IN_WEEK = 7
    const val WEEKS_IN_MONTH = 4.33 // Promedio de semanas por mes
    const val MONTHS_IN_YEAR = 12
    
    // SharedPreferences
    const val PREFS_NAME = "ks_expire_prefs"
    const val PREF_CURRENCY_SYMBOL = "currency_symbol"
    const val PREF_ITEMS_CREATED_COUNT = "items_created_count"
    const val PREF_REVIEW_REQUESTED = "review_requested"
}