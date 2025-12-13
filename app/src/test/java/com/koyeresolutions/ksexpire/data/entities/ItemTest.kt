package com.koyeresolutions.ksexpire.data.entities

import com.koyeresolutions.ksexpire.utils.Constants
import org.junit.Assert.*
import org.junit.Test
import java.util.*

/**
 * Pruebas unitarias para la entidad Item
 * Valida la lógica de negocio crítica implementada
 */
class ItemTest {

    @Test
    fun `createSubscription should create valid subscription`() {
        val name = "Netflix"
        val price = 15.99
        val frequency = Constants.FREQUENCY_MONTHLY
        val nextBilling = System.currentTimeMillis() + (30 * 24 * 60 * 60 * 1000L)
        
        val subscription = Item.createSubscription(name, price, frequency, nextBilling)
        
        assertTrue(subscription.isSubscription())
        assertFalse(subscription.isWarranty())
        assertEquals(name, subscription.name)
        assertEquals(price, subscription.price, 0.01)
        assertEquals(frequency, subscription.billingFrequency)
        assertEquals(nextBilling, subscription.expiryDate)
    }

    @Test
    fun `createWarranty should create valid warranty`() {
        val name = "Samsung TV"
        val purchaseDate = System.currentTimeMillis()
        val expiryDate = purchaseDate + (365 * 24 * 60 * 60 * 1000L) // 1 año
        val price = 899.99
        
        val warranty = Item.createWarranty(name, purchaseDate, expiryDate, price)
        
        assertTrue(warranty.isWarranty())
        assertFalse(warranty.isSubscription())
        assertEquals(name, warranty.name)
        assertEquals(price, warranty.price, 0.01)
        assertEquals(purchaseDate, warranty.purchaseDate)
        assertEquals(expiryDate, warranty.expiryDate)
    }

    @Test
    fun `getNormalizedMonthlyPrice should calculate correctly for different frequencies`() {
        val monthlySubscription = Item.createSubscription(
            "Monthly Service", 10.0, Constants.FREQUENCY_MONTHLY, System.currentTimeMillis()
        )
        assertEquals(10.0, monthlySubscription.getNormalizedMonthlyPrice(), 0.01)
        
        val annualSubscription = Item.createSubscription(
            "Annual Service", 120.0, Constants.FREQUENCY_ANNUAL, System.currentTimeMillis()
        )
        assertEquals(10.0, annualSubscription.getNormalizedMonthlyPrice(), 0.01)
        
        val weeklySubscription = Item.createSubscription(
            "Weekly Service", 2.5, Constants.FREQUENCY_WEEKLY, System.currentTimeMillis()
        )
        assertEquals(10.825, weeklySubscription.getNormalizedMonthlyPrice(), 0.01)
    }

    @Test
    fun `getDaysUntilExpiry should calculate correctly`() {
        val now = System.currentTimeMillis()
        val futureDate = now + (5 * 24 * 60 * 60 * 1000L) // 5 días en el futuro
        val pastDate = now - (3 * 24 * 60 * 60 * 1000L) // 3 días en el pasado
        
        val futureItem = Item.createWarranty("Future", now, futureDate)
        val pastItem = Item.createWarranty("Past", pastDate, pastDate)
        
        assertEquals(5, futureItem.getDaysUntilExpiry())
        assertTrue(pastItem.getDaysUntilExpiry() < 0)
    }

    @Test
    fun `isExpired should work correctly`() {
        val now = System.currentTimeMillis()
        val futureDate = now + (24 * 60 * 60 * 1000L) // 1 día en el futuro
        val pastDate = now - (24 * 60 * 60 * 1000L) // 1 día en el pasado
        
        val validItem = Item.createWarranty("Valid", now, futureDate)
        val expiredItem = Item.createWarranty("Expired", pastDate, pastDate)
        
        assertFalse(validItem.isExpired())
        assertTrue(expiredItem.isExpired())
    }

    @Test
    fun `isExpiringSoon should work correctly`() {
        val now = System.currentTimeMillis()
        val soonDate = now + (3 * 24 * 60 * 60 * 1000L) // 3 días
        val farDate = now + (30 * 24 * 60 * 60 * 1000L) // 30 días
        
        val soonItem = Item.createWarranty("Soon", now, soonDate)
        val farItem = Item.createWarranty("Far", now, farDate)
        
        assertTrue(soonItem.isExpiringSoon())
        assertFalse(farItem.isExpiringSoon())
    }

    @Test
    fun `getWarrantyStatus should return correct status`() {
        val now = System.currentTimeMillis()
        val expiredDate = now - (24 * 60 * 60 * 1000L) // Vencida
        val soonDate = now + (15 * 24 * 60 * 60 * 1000L) // Por vencer (15 días)
        val validDate = now + (60 * 24 * 60 * 60 * 1000L) // Vigente (60 días)
        
        val expiredItem = Item.createWarranty("Expired", now, expiredDate)
        val soonItem = Item.createWarranty("Soon", now, soonDate)
        val validItem = Item.createWarranty("Valid", now, validDate)
        
        assertEquals(0, expiredItem.getWarrantyStatus()) // Vencida
        assertEquals(1, soonItem.getWarrantyStatus()) // Por vencer
        assertEquals(2, validItem.getWarrantyStatus()) // Vigente
    }

    @Test
    fun `getWarrantyProgress should calculate correctly`() {
        val now = System.currentTimeMillis()
        val purchaseDate = now - (180 * 24 * 60 * 60 * 1000L) // Hace 180 días
        val expiryDate = now + (180 * 24 * 60 * 60 * 1000L) // En 180 días (total 360 días)
        
        val warranty = Item.createWarranty("Test", purchaseDate, expiryDate)
        val progress = warranty.getWarrantyProgress()
        
        // Debería estar aproximadamente al 50% (180 días transcurridos de 360 total)
        assertTrue(progress > 0.4f && progress < 0.6f)
    }

    @Test
    fun `validate should catch validation errors`() {
        // Ítem válido
        val validItem = Item.createSubscription(
            "Valid", 10.0, Constants.FREQUENCY_MONTHLY, System.currentTimeMillis() + 1000
        )
        assertTrue(validItem.validate().isEmpty())
        
        // Nombre vacío
        val emptyNameItem = validItem.copy(name = "")
        assertTrue(emptyNameItem.validate().contains("El nombre es obligatorio"))
        
        // Fecha de vencimiento anterior a compra
        val invalidDateItem = validItem.copy(
            purchaseDate = System.currentTimeMillis(),
            expiryDate = System.currentTimeMillis() - 1000
        )
        assertTrue(invalidDateItem.validate().contains("La fecha de vencimiento debe ser posterior a la de compra"))
        
        // Suscripción sin frecuencia
        val noFrequencyItem = validItem.copy(billingFrequency = null)
        assertTrue(noFrequencyItem.validate().contains("La frecuencia de cobro es obligatoria para suscripciones"))
        
        // Precio negativo
        val negativePriceItem = validItem.copy(price = -10.0)
        assertTrue(negativePriceItem.validate().contains("El precio no puede ser negativo"))
    }

    @Test
    fun `getNextBillingDate should calculate correctly`() {
        val now = System.currentTimeMillis()
        
        val monthlySubscription = Item.createSubscription(
            "Monthly", 10.0, Constants.FREQUENCY_MONTHLY, now
        )
        
        val nextBilling = monthlySubscription.getNextBillingDate()
        assertNotNull(nextBilling)
        
        // Verificar que la próxima fecha es aproximadamente un mes después
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = now
        calendar.add(Calendar.MONTH, 1)
        val expectedTime = calendar.timeInMillis
        
        // Permitir diferencia de 1 día por variaciones en días del mes
        val diff = Math.abs(nextBilling!! - expectedTime)
        assertTrue(diff < (24 * 60 * 60 * 1000L))
    }
}