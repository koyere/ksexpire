package com.koyeresolutions.ksexpire.data.database

import androidx.room.TypeConverter
import java.util.*

/**
 * Convertidores de tipo para Room Database
 * Maneja la conversión entre tipos complejos y tipos primitivos para almacenamiento
 */
class Converters {

    /**
     * Convertir Date a Long (timestamp)
     */
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    /**
     * Convertir Long (timestamp) a Date
     */
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    /**
     * Convertir String a List<String>
     */
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    /**
     * Convertir List<String> a String
     */
    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}