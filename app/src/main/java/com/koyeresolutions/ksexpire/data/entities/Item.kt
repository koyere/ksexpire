package com.koyeresolutions.ksexpire.data.entities

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable
import com.koyeresolutions.ksexpire.utils.Constants
import java.util.*

/**
 * Entidad principal que representa tanto suscripciones como garantías/recibos
 * Diseñada para máxima flexibilidad y eficiencia de almacenamiento
 */
@Entity(tableName = "items")
@Parcelize
@Serializable
data class Item(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /**
     * Tipo de ítem: 0 = Garantía/Recibo, 1 = Suscripción
     * @see Constants.ITEM_TYPE_WARRANTY
     * @see Constants.ITEM_TYPE_SUBSCRIPTION
     */
    val type: Int,
    
    /**
     * Nombre del ítem (obligatorio)
     * Ej: "Netflix", "Garantía Samsung TV", "Amazon Prime"
     */
    val name: String,
    
    /**
     * Precio del ítem (opcional)
     * Para suscripciones: precio por período de facturación
     * Para garantías: precio de compra (opcional)
     */
    val price: Double? = null,
    
    /**
     * Fecha de compra/inicio (obligatorio)
     * Timestamp en milisegundos
     */
    val purchaseDate: Long,
    
    /**
     * Fecha de vencimiento (obligatorio)
     * Para suscripciones: próxima fecha de cobro
     * Para garantías: fecha de vencimiento de la garantía
     */
    val expiryDate: Long,
    
    /**
     * Frecuencia de facturación (solo para suscripciones)
     * @see Constants.FREQUENCY_WEEKLY
     * @see Constants.FREQUENCY_MONTHLY  
     * @see Constants.FREQUENCY_ANNUAL
     */
    val billingFrequency: String? = null,
    
    /**
     * Ruta relativa de la imagen del recibo
     * IMPORTANTE: Solo nombre del archivo, no ruta absoluta
     * Ej: "img_20241213_143022.jpg"
     */
    val imagePath: String? = null,
    
    /**
     * Configuración de notificaciones en formato JSON
     * Ej: {"subscription_days": 1, "warranty_days_1": 30, "warranty_days_2": 7}
     */
    val notificationsConfig: String? = null,
    
    /**
     * Estado activo del ítem
     * true = activo, false = pausado/inactivo
     */
    val isActive: Boolean = true,
    
    /**
     * Fecha de creación del registro
     * Timestamp en milisegundos
     */
    val createdAt: Long = System.currentTimeMillis(),
    
    /**
     * Fecha de última modificación
     * Timestamp en milisegundos
     */
    val updatedAt: Long = System.currentTimeMillis()
) : Parcelable {

    /**
     * Verificar si es una suscripción
     */
    fun isSubscription(): Boolean = type == Constants.ITEM_TYPE_SUBSCRIPTION

    /**
     * Verificar si es una garantía/recibo
     */
    fun isWarranty(): Boolean = type == Constants.ITEM_TYPE_WARRANTY

    /**
     * Calcular días hasta el vencimiento
     * @return Número de días (positivo = futuro, negativo = pasado)
     */
    fun getDaysUntilExpiry(): Int {
        val now = System.currentTimeMillis()
        val diffMillis = expiryDate - now
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    /**
     * Verificar si el ítem está vencido
     */
    fun isExpired(): Boolean = getDaysUntilExpiry() < 0

    /**
     * Verificar si el ítem está por vencer (próximos 7 días)
     */
    fun isExpiringSoon(): Boolean {
        val days = getDaysUntilExpiry()
        return days in 0..7
    }

    /**
     * Obtener estado de vigencia para garantías
     * @return 0 = Vencida, 1 = Por vencer, 2 = Vigente
     */
    fun getWarrantyStatus(): Int {
        val days = getDaysUntilExpiry()
        return when {
            days < 0 -> 0 // Vencida
            days <= 30 -> 1 // Por vencer
            else -> 2 // Vigente
        }
    }

    /**
     * Calcular progreso de vigencia para garantías (0.0 a 1.0)
     */
    fun getWarrantyProgress(): Float {
        if (isWarranty()) {
            val totalDays = (expiryDate - purchaseDate) / (1000 * 60 * 60 * 24)
            val elapsedDays = (System.currentTimeMillis() - purchaseDate) / (1000 * 60 * 60 * 24)
            return if (totalDays > 0) {
                (elapsedDays.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
        return 0f
    }

    /**
     * Normalizar precio a mensual para cálculo de gasto mensual
     * IMPORTANTE: Implementa la lógica de normalización del planning
     */
    fun getNormalizedMonthlyPrice(): Double {
        if (!isSubscription() || price == null) return 0.0
        
        return when (billingFrequency) {
            Constants.FREQUENCY_MONTHLY -> price
            Constants.FREQUENCY_ANNUAL -> price / Constants.MONTHS_IN_YEAR
            Constants.FREQUENCY_WEEKLY -> price * Constants.WEEKS_IN_MONTH
            else -> 0.0
        }
    }

    /**
     * Calcular próxima fecha de cobro para suscripciones
     */
    fun getNextBillingDate(): Long? {
        if (!isSubscription() || billingFrequency == null) return null
        
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = expiryDate
        
        return when (billingFrequency) {
            Constants.FREQUENCY_WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
                calendar.timeInMillis
            }
            Constants.FREQUENCY_MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
                calendar.timeInMillis
            }
            Constants.FREQUENCY_ANNUAL -> {
                calendar.add(Calendar.YEAR, 1)
                calendar.timeInMillis
            }
            else -> null
        }
    }

    /**
     * Validar datos del ítem
     * @return Lista de errores de validación (vacía si es válido)
     */
    fun validate(): List<String> {
        val errors = mutableListOf<String>()
        
        if (name.isBlank()) {
            errors.add("El nombre es obligatorio")
        }
        
        if (purchaseDate <= 0) {
            errors.add("La fecha de compra es obligatoria")
        }
        
        if (expiryDate <= 0) {
            errors.add("La fecha de vencimiento es obligatoria")
        }
        
        if (expiryDate <= purchaseDate) {
            errors.add("La fecha de vencimiento debe ser posterior a la de compra")
        }
        
        if (isSubscription() && billingFrequency.isNullOrBlank()) {
            errors.add("La frecuencia de cobro es obligatoria para suscripciones")
        }
        
        if (price != null && price < 0) {
            errors.add("El precio no puede ser negativo")
        }
        
        return errors
    }

    companion object {
        /**
         * Crear nueva suscripción con valores por defecto
         */
        fun createSubscription(
            name: String,
            price: Double?,
            billingFrequency: String,
            nextBillingDate: Long
        ): Item {
            return Item(
                type = Constants.ITEM_TYPE_SUBSCRIPTION,
                name = name,
                price = price,
                purchaseDate = System.currentTimeMillis(),
                expiryDate = nextBillingDate,
                billingFrequency = billingFrequency
            )
        }

        /**
         * Crear nueva garantía/recibo con valores por defecto
         */
        fun createWarranty(
            name: String,
            purchaseDate: Long,
            expiryDate: Long,
            price: Double? = null
        ): Item {
            return Item(
                type = Constants.ITEM_TYPE_WARRANTY,
                name = name,
                price = price,
                purchaseDate = purchaseDate,
                expiryDate = expiryDate
            )
        }
    }
}