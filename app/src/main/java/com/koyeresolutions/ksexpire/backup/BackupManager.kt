package com.koyeresolutions.ksexpire.backup

import android.content.Context
import android.net.Uri
import com.koyeresolutions.ksexpire.data.entities.Item
import com.koyeresolutions.ksexpire.utils.Constants
import com.koyeresolutions.ksexpire.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manager para backup y restauración de datos
 * IMPLEMENTA LA FUNCIONALIDAD DE BACKUP DEL PLANNING
 */
@OptIn(InternalSerializationApi::class)
class BackupManager(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    companion object {
        private const val BACKUP_VERSION = 1
        private const val METADATA_FILE = "backup_metadata.json"
        private const val ITEMS_FILE = "items.json"
        private const val IMAGES_DIR = "images/"
    }

    /**
     * Crear backup completo con base de datos e imágenes
     */
    suspend fun createBackup(items: List<Item>, outputUri: Uri): Result<BackupResult> {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val outputStream = contentResolver.openOutputStream(outputUri)
                    ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo de destino"))

                ZipOutputStream(BufferedOutputStream(outputStream)).use { zipOut ->
                    // Crear metadata del backup
                    val metadata = BackupMetadata(
                        version = BACKUP_VERSION,
                        createdAt = System.currentTimeMillis(),
                        appVersion = getAppVersion(),
                        itemsCount = items.size,
                        imagesCount = items.count { !it.imagePath.isNullOrBlank() }
                    )

                    // Agregar metadata al ZIP
                    addMetadataToZip(zipOut, metadata)

                    // Agregar datos de ítems al ZIP
                    addItemsToZip(zipOut, items)

                    // Agregar imágenes al ZIP
                    val imagesAdded = addImagesToZip(zipOut, items)

                    val result = BackupResult(
                        success = true,
                        itemsBackedUp = items.size,
                        imagesBackedUp = imagesAdded,
                        backupSize = getFileSize(outputUri),
                        message = "Backup creado exitosamente"
                    )

                    Result.success(result)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Restaurar backup desde archivo ZIP
     */
    suspend fun restoreBackup(inputUri: Uri): Result<RestoreResult> {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(inputUri)
                    ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo de backup"))

                var metadata: BackupMetadata? = null
                var items: List<Item> = emptyList()
                val restoredImages = mutableListOf<String>()

                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry

                    while (entry != null) {
                        when {
                            entry.name == METADATA_FILE -> {
                                metadata = readMetadataFromZip(zipIn)
                            }
                            entry.name == ITEMS_FILE -> {
                                items = readItemsFromZip(zipIn)
                            }
                            entry.name.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                                val imageName = entry.name.substring(IMAGES_DIR.length)
                                if (restoreImageFromZip(zipIn, imageName)) {
                                    restoredImages.add(imageName)
                                }
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                // Validar backup
                val validMetadata = metadata
                if (validMetadata == null) {
                    return@withContext Result.failure(Exception("Archivo de backup inválido: falta metadata"))
                }

                if (items.isEmpty()) {
                    return@withContext Result.failure(Exception("Archivo de backup inválido: no contiene datos"))
                }

                // Validar compatibilidad de versión
                if (validMetadata.version > BACKUP_VERSION) {
                    return@withContext Result.failure(Exception("Versión de backup no compatible"))
                }

                val result = RestoreResult(
                    success = true,
                    itemsRestored = items.size,
                    imagesRestored = restoredImages.size,
                    backupDate = validMetadata.createdAt,
                    items = items,
                    message = "Backup restaurado exitosamente"
                )

                Result.success(result)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Agregar metadata al ZIP
     */
    private fun addMetadataToZip(zipOut: ZipOutputStream, metadata: BackupMetadata) {
        val entry = ZipEntry(METADATA_FILE)
        zipOut.putNextEntry(entry)
        
        val metadataJson = json.encodeToString(metadata)
        zipOut.write(metadataJson.toByteArray())
        
        zipOut.closeEntry()
    }

    /**
     * Agregar ítems al ZIP
     */
    private fun addItemsToZip(zipOut: ZipOutputStream, items: List<Item>) {
        val entry = ZipEntry(ITEMS_FILE)
        zipOut.putNextEntry(entry)
        
        val itemsJson = json.encodeToString(items)
        zipOut.write(itemsJson.toByteArray())
        
        zipOut.closeEntry()
    }

    /**
     * Agregar imágenes al ZIP
     */
    private fun addImagesToZip(zipOut: ZipOutputStream, items: List<Item>): Int {
        var imagesAdded = 0
        
        items.forEach { item ->
            if (!item.imagePath.isNullOrBlank()) {
                val imageFile = FileUtils.getImageFile(context, item.imagePath)
                if (imageFile.exists()) {
                    try {
                        val entry = ZipEntry("$IMAGES_DIR${item.imagePath}")
                        zipOut.putNextEntry(entry)
                        
                        FileInputStream(imageFile).use { fileIn ->
                            fileIn.copyTo(zipOut)
                        }
                        
                        zipOut.closeEntry()
                        imagesAdded++
                    } catch (e: Exception) {
                        // Continuar con la siguiente imagen si hay error
                    }
                }
            }
        }
        
        return imagesAdded
    }

    /**
     * Leer metadata desde ZIP
     */
    private fun readMetadataFromZip(zipIn: ZipInputStream): BackupMetadata {
        val metadataJson = zipIn.readBytes().toString(Charsets.UTF_8)
        return json.decodeFromString<BackupMetadata>(metadataJson)
    }

    /**
     * Leer ítems desde ZIP
     */
    private fun readItemsFromZip(zipIn: ZipInputStream): List<Item> {
        val itemsJson = zipIn.readBytes().toString(Charsets.UTF_8)
        return json.decodeFromString<List<Item>>(itemsJson)
    }

    /**
     * Restaurar imagen desde ZIP
     */
    private fun restoreImageFromZip(zipIn: ZipInputStream, imageName: String): Boolean {
        return try {
            val imageFile = FileUtils.getImageFile(context, imageName)
            
            // Crear directorio si no existe
            imageFile.parentFile?.mkdirs()
            
            FileOutputStream(imageFile).use { fileOut ->
                zipIn.copyTo(fileOut)
            }
            
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Generar nombre de archivo de backup
     */
    fun generateBackupFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "${Constants.BACKUP_FILE_PREFIX}${timestamp}${Constants.BACKUP_EXTENSION}"
    }

    /**
     * Obtener versión de la app
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    /**
     * Obtener tamaño de archivo
     */
    private fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                pfd.statSize
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Validar archivo de backup
     */
    suspend fun validateBackupFile(inputUri: Uri): Result<BackupValidation> {
        return withContext(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(inputUri)
                    ?: return@withContext Result.failure(Exception("No se pudo abrir el archivo"))

                var hasMetadata = false
                var hasItems = false
                var itemsCount = 0
                var imagesCount = 0

                ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                    var entry: ZipEntry? = zipIn.nextEntry

                    while (entry != null) {
                        when {
                            entry.name == METADATA_FILE -> hasMetadata = true
                            entry.name == ITEMS_FILE -> {
                                hasItems = true
                                // Contar ítems sin cargar todo en memoria
                                val itemsJson = zipIn.readBytes().toString(Charsets.UTF_8)
                                val items = json.decodeFromString<List<Item>>(itemsJson)
                                itemsCount = items.size
                            }
                            entry.name.startsWith(IMAGES_DIR) && !entry.isDirectory -> {
                                imagesCount++
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                val validation = BackupValidation(
                    isValid = hasMetadata && hasItems,
                    hasMetadata = hasMetadata,
                    hasItems = hasItems,
                    itemsCount = itemsCount,
                    imagesCount = imagesCount,
                    fileSize = getFileSize(inputUri)
                )

                Result.success(validation)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    /**
     * Metadata del backup
     */
    @Serializable
    data class BackupMetadata(
        val version: Int,
        val createdAt: Long,
        val appVersion: String,
        val itemsCount: Int,
        val imagesCount: Int
    )

    /**
     * Resultado de backup
     */
    data class BackupResult(
        val success: Boolean,
        val itemsBackedUp: Int,
        val imagesBackedUp: Int,
        val backupSize: Long,
        val message: String
    )

    /**
     * Resultado de restauración
     */
    data class RestoreResult(
        val success: Boolean,
        val itemsRestored: Int,
        val imagesRestored: Int,
        val backupDate: Long,
        val items: List<Item>,
        val message: String
    )

    /**
     * Validación de backup
     */
    data class BackupValidation(
        val isValid: Boolean,
        val hasMetadata: Boolean,
        val hasItems: Boolean,
        val itemsCount: Int,
        val imagesCount: Int,
        val fileSize: Long
    )
}