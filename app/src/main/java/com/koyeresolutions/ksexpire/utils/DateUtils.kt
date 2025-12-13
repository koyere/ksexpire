package com.koyeresolutions.ksexpire.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utilidades para manejo de fechas en la aplicación
 */
object DateUtils {

    // Formatos de fecha comunes
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val shortDateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    /**
     * Formatear timestamp a fecha legible
     */
    fun formatDate(timestamp: Long): String {
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Formatear timestamp a fecha y hora legible
     */
    fun formatDateTime(timestamp: Long): String {
        return dateTimeFormat.format(Date(timestamp))
    }

    /**
     * Formatear fecha corta (para listas)
     */
    fun formatShortDate(timestamp: Long): String {
        return shortDateFormat.format(Date(timestamp))
    }

    /**
     * Formatear mes y año
     */
    fun formatMonthYear(timestamp: Long): String {
        return monthYearFormat.format(Date(timestamp))
    }

    /**
     * Obtener timestamp del inicio del día
     */
    fun getStartOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Obtener timestamp del final del día
     */
    fun getEndOfDay(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
        return calendar.timeInMillis
    }

    /**
     * Calcular días entre dos fechas
     */
    fun daysBetween(startTimestamp: Long, endTimestamp: Long): Int {
        val diffMillis = endTimestamp - startTimestamp
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Agregar días a una fecha
     */
    fun addDays(timestamp: Long, days: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.DAY_OF_YEAR, days)
        return calendar.timeInMillis
    }

    /**
     * Agregar meses a una fecha
     */
    fun addMonths(timestamp: Long, months: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.MONTH, months)
        return calendar.timeInMillis
    }

    /**
     * Agregar años a una fecha
     */
    fun addYears(timestamp: Long, years: Int): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.add(Calendar.YEAR, years)
        return calendar.timeInMillis
    }

    /**
     * Verificar si una fecha es hoy
     */
    fun isToday(timestamp: Long): Boolean {
        val today = getStartOfDay(System.currentTimeMillis())
        val targetDay = getStartOfDay(timestamp)
        return today == targetDay
    }

    /**
     * Verificar si una fecha es mañana
     */
    fun isTomorrow(timestamp: Long): Boolean {
        val tomorrow = addDays(getStartOfDay(System.currentTimeMillis()), 1)
        val targetDay = getStartOfDay(timestamp)
        return tomorrow == targetDay
    }

    /**
     * Obtener descripción relativa de fecha (hoy, mañana, en X días)
     */
    fun getRelativeDateDescription(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val days = daysBetween(now, timestamp)
        
        return when {
            days == 0 -> "Hoy"
            days == 1 -> "Mañana"
            days == -1 -> "Ayer"
            days > 1 -> "En $days días"
            days < -1 -> "Hace ${-days} días"
            else -> formatDate(timestamp)
        }
    }

    /**
     * Parsear fecha desde string
     */
    fun parseDate(dateString: String): Long? {
        return try {
            dateFormat.parse(dateString)?.time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validar que una fecha sea válida
     */
    fun isValidDate(timestamp: Long): Boolean {
        return timestamp > 0 && timestamp < Long.MAX_VALUE
    }

    /**
     * Obtener timestamp de medianoche de hoy
     */
    fun getTodayStart(): Long = getStartOfDay(System.currentTimeMillis())

    /**
     * Obtener timestamp de final de hoy
     */
    fun getTodayEnd(): Long = getEndOfDay(System.currentTimeMillis())
}