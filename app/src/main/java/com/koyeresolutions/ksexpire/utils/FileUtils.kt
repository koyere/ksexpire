package com.koyeresolutions.ksexpire.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades para manejo de archivos e imágenes
 * Implementa almacenamiento con rutas relativas para robustez en backup/restore
 */
object FileUtils {

    /**
     * Crear directorios necesarios para la app
     */
    fun createAppDirectories(context: Context) {
        val receiptsDir = File(context.filesDir, Constants.RECEIPTS_DIR)
        val backupsDir = File(context.filesDir, Constants.BACKUPS_DIR)
        val tempDir = File(context.cacheDir, Constants.TEMP_DIR)
        
        receiptsDir.mkdirs()
        backupsDir.mkdirs()
        tempDir.mkdirs()
    }

    /**
     * Generar nombre único para archivo de imagen
     * Formato: img_yyyyMMdd_HHmmss.jpg
     */
    fun generateImageFileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            .format(Date())
        return "img_${timestamp}${Constants.IMAGE_EXTENSION}"
    }

    /**
     * Obtener ruta completa de imagen desde nombre de archivo
     * IMPORTANTE: Construye ruta dinámicamente para robustez
     */
    fun getImageFile(context: Context, fileName: String): File {
        return File(context.filesDir, "${Constants.RECEIPTS_DIR}/$fileName")
    }

    /**
     * Guardar imagen comprimida en almacenamiento privado
     * @param bitmap Imagen a guardar
     * @param context Contexto de la aplicación
     * @return Nombre del archivo (ruta relativa) o null si hay error
     */
    fun saveCompressedImage(bitmap: Bitmap, context: Context): String? {
        return try {
            val fileName = generateImageFileName()
            val file = getImageFile(context, fileName)
            
            // Comprimir y guardar
            FileOutputStream(file).use { out ->
                bitmap.compress(
                    Bitmap.CompressFormat.JPEG,
                    Constants.IMAGE_QUALITY,
                    out
                )
            }
            
            fileName // Retornar solo el nombre del archivo
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Cargar imagen desde almacenamiento privado
     * @param context Contexto de la aplicación
     * @param fileName Nombre del archivo (ruta relativa)
     * @return Bitmap o null si no existe
     */
    fun loadImageFromFile(context: Context, fileName: String): Bitmap? {
        return try {
            val file = getImageFile(context, fileName)
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Eliminar archivo de imagen
     * @param context Contexto de la aplicación
     * @param fileName Nombre del archivo a eliminar
     * @return true si se eliminó correctamente
     */
    fun deleteImageFile(context: Context, fileName: String): Boolean {
        return try {
            val file = getImageFile(context, fileName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Redimensionar bitmap manteniendo proporción
     */
    fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scale = minOf(scaleWidth, scaleHeight)
        
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, false)
    }

    /**
     * Corregir orientación de imagen basada en EXIF
     */
    fun correctImageOrientation(bitmap: Bitmap, imagePath: String): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            
            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            e.printStackTrace()
            bitmap
        }
    }

    /**
     * Comprimir bitmap a JPEG con calidad específica
     */
    fun compressBitmapToJpeg(bitmap: Bitmap, quality: Int = Constants.IMAGE_QUALITY): ByteArray {
        val outputStream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }

    /**
     * Crear miniatura de imagen para preview
     */
    fun createThumbnail(bitmap: Bitmap, maxSize: Int = 200): Bitmap {
        val ratio = minOf(
            maxSize.toFloat() / bitmap.width,
            maxSize.toFloat() / bitmap.height
        )
        
        val width = (bitmap.width * ratio).toInt()
        val height = (bitmap.height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, width, height, true)
    }

    /**
     * Obtener tamaño de directorio en bytes
     */
    fun getDirectorySize(directory: File): Long {
        var size = 0L
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    getDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        return size
    }

    /**
     * Limpiar archivos temporales
     */
    fun cleanTempFiles(context: Context) {
        val tempDir = File(context.cacheDir, Constants.TEMP_DIR)
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { file ->
                file.delete()
            }
        }
    }

    /**
     * Verificar si hay espacio suficiente para guardar archivo
     */
    fun hasEnoughSpace(context: Context, requiredBytes: Long): Boolean {
        return context.filesDir.freeSpace >= requiredBytes
    }
}