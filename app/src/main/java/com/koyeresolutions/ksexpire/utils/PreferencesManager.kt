package com.koyeresolutions.ksexpire.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager para SharedPreferences
 * Maneja configuraciones y contadores de la app
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        Constants.PREFS_NAME, 
        Context.MODE_PRIVATE
    )

    /**
     * Símbolo de moneda preferido
     */
    fun getCurrencySymbol(): String {
        return prefs.getString(Constants.PREF_CURRENCY_SYMBOL, null) 
            ?: CurrencyUtils.getLocalCurrencySymbol()
    }

    fun setCurrencySymbol(symbol: String) {
        prefs.edit().putString(Constants.PREF_CURRENCY_SYMBOL, symbol).apply()
    }

    /**
     * Contador de ítems creados (para trigger de review)
     */
    fun getItemsCreatedCount(): Int {
        return prefs.getInt(Constants.PREF_ITEMS_CREATED_COUNT, 0)
    }

    fun setItemsCreatedCount(count: Int) {
        prefs.edit().putInt(Constants.PREF_ITEMS_CREATED_COUNT, count).apply()
    }

    /**
     * Estado de review solicitado
     */
    fun isReviewRequested(): Boolean {
        return prefs.getBoolean(Constants.PREF_REVIEW_REQUESTED, false)
    }

    fun setReviewRequested(requested: Boolean) {
        prefs.edit().putBoolean(Constants.PREF_REVIEW_REQUESTED, requested).apply()
    }

    /**
     * Limpiar todas las preferencias
     */
    fun clearAll() {
        prefs.edit().clear().apply()
    }
}