package com.koyeresolutions.ksexpire.utils

import android.content.Context
import android.content.SharedPreferences
import java.text.NumberFormat
import java.util.*

/**
 * Utilidades para manejo de monedas y formateo de precios
 * IMPLEMENTA LA FUNCIONALIDAD DE MONEDA DEL PLANNING
 */
object CurrencyUtils {

    // Símbolos de moneda soportados
    private val supportedCurrencies = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "MXN" to "$",
        "COP" to "$",
        "ARS" to "$",
        "CLP" to "$",
        "PEN" to "S/",
        "BRL" to "R$"
    )

    /**
     * Obtener formateador de moneda automático basado en la configuración del sistema
     */
    fun getAutoCurrencyFormatter(): NumberFormat {
        return NumberFormat.getCurrencyInstance()
    }

    /**
     * Obtener símbolo de moneda configurado por el usuario
     */
    fun getCurrencySymbol(context: Context): String {
        val prefs = getPreferences(context)
        val savedSymbol = prefs.getString(Constants.PREF_CURRENCY_SYMBOL, null)
        
        return if (savedSymbol != null) {
            savedSymbol
        } else {
            // Detectar automáticamente basado en la configuración del sistema
            getAutoCurrencySymbol()
        }
    }

    /**
     * Establecer símbolo de moneda personalizado
     */
    fun setCurrencySymbol(context: Context, symbol: String) {
        val prefs = getPreferences(context)
        prefs.edit()
            .putString(Constants.PREF_CURRENCY_SYMBOL, symbol)
            .apply()
    }

    /**
     * Detectar símbolo de moneda automáticamente
     */
    private fun getAutoCurrencySymbol(): String {
        return try {
            val currency = Currency.getInstance(Locale.getDefault())
            supportedCurrencies[currency.currencyCode] ?: currency.symbol
        } catch (e: Exception) {
            "$" // Fallback por defecto
        }
    }

    /**
     * Formatear precio con símbolo de moneda
     */
    fun formatPrice(context: Context, price: Double?): String {
        if (price == null || price == 0.0) {
            return "-"
        }
        
        val symbol = getCurrencySymbol(context)
        return "$symbol${formatNumber(price)}"
    }

    /**
     * Formatear precio sin símbolo (solo número)
     */
    fun formatNumber(price: Double): String {
        return if (price == price.toInt().toDouble()) {
            // Si es un número entero, no mostrar decimales
            String.format(Locale.getDefault(), "%.0f", price)
        } else {
            // Mostrar hasta 2 decimales
            String.format(Locale.getDefault(), "%.2f", price)
        }
    }

    /**
     * Formatear gasto mensual con descripción
     */
    fun formatMonthlyExpense(context: Context, monthlyExpense: Double): String {
        val formattedPrice = formatPrice(context, monthlyExpense)
        return "$formattedPrice/mes"
    }

    /**
     * Parsear precio desde string
     */
    fun parsePrice(priceString: String): Double? {
        return try {
            // Remover símbolos de moneda y espacios
            val cleanString = priceString
                .replace(Regex("[^0-9.,]"), "")
                .replace(",", ".")
            
            if (cleanString.isBlank()) {
                null
            } else {
                cleanString.toDouble()
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Validar que un precio sea válido
     */
    fun isValidPrice(price: Double?): Boolean {
        return price != null && price >= 0 && price.isFinite()
    }

    /**
     * Obtener lista de monedas soportadas para configuración
     */
    fun getSupportedCurrencies(): List<CurrencyOption> {
        val autoOption = CurrencyOption(
            code = "AUTO",
            symbol = getAutoCurrencySymbol(),
            name = "Automático (${getAutoCurrencySymbol()})"
        )
        
        val manualOptions = supportedCurrencies.map { (code, symbol) ->
            CurrencyOption(
                code = code,
                symbol = symbol,
                name = "$code ($symbol)"
            )
        }
        
        return listOf(autoOption) + manualOptions
    }

    /**
     * Verificar si el usuario ha configurado una moneda personalizada
     */
    fun hasCustomCurrency(context: Context): Boolean {
        val prefs = getPreferences(context)
        return prefs.contains(Constants.PREF_CURRENCY_SYMBOL)
    }

    /**
     * Resetear a detección automática de moneda
     */
    fun resetToAutoCurrency(context: Context) {
        val prefs = getPreferences(context)
        prefs.edit()
            .remove(Constants.PREF_CURRENCY_SYMBOL)
            .apply()
    }

    /**
     * Obtener código de moneda actual del sistema
     */
    fun getCurrentCurrencyCode(): String {
        return try {
            Currency.getInstance(Locale.getDefault()).currencyCode
        } catch (e: Exception) {
            "USD"
        }
    }

    /**
     * Formatear precio para diferentes frecuencias de suscripción
     */
    fun formatSubscriptionPrice(context: Context, price: Double?, frequency: String?): String {
        if (price == null) return "-"
        
        val formattedPrice = formatPrice(context, price)
        val frequencyText = when (frequency) {
            Constants.FREQUENCY_WEEKLY -> "/semana"
            Constants.FREQUENCY_MONTHLY -> "/mes"
            Constants.FREQUENCY_ANNUAL -> "/año"
            else -> ""
        }
        
        return "$formattedPrice$frequencyText"
    }

    /**
     * Calcular y formatear ahorro anual vs mensual
     */
    fun formatAnnualSavings(context: Context, monthlyPrice: Double, annualPrice: Double): String {
        val monthlyCost = monthlyPrice * 12
        val savings = monthlyCost - annualPrice
        
        return if (savings > 0) {
            "Ahorras ${formatPrice(context, savings)} al año"
        } else {
            ""
        }
    }

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Opción de moneda para configuración
     */
    data class CurrencyOption(
        val code: String,
        val symbol: String,
        val name: String
    )
}