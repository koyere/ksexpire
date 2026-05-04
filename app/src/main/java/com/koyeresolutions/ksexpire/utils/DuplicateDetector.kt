package com.koyeresolutions.ksexpire.utils

import com.koyeresolutions.ksexpire.data.entities.Item

/**
 * Detector de ítems duplicados o similares
 * Usa distancia de Levenshtein y comparación de nombres base
 */
object DuplicateDetector {

    /**
     * Buscar ítems similares al nombre dado
     * @param name Nombre a comparar
     * @param existingItems Lista de ítems existentes
     * @param currentItemId ID del ítem actual (para excluirlo en modo edición)
     * @return Lista de ítems similares encontrados
     */
    fun findSimilarItems(name: String, existingItems: List<Item>, currentItemId: Long? = null): List<Item> {
        if (name.isBlank() || name.length < 3) return emptyList()
        
        val normalizedName = name.trim().lowercase()
        
        return existingItems.filter { item ->
            // Excluir el ítem actual en modo edición
            if (item.id == currentItemId) return@filter false
            
            val existingName = item.name.trim().lowercase()
            
            // Criterio 1: Nombre contiene al otro
            val containsMatch = normalizedName.contains(existingName) || 
                               existingName.contains(normalizedName)
            
            // Criterio 2: Distancia de Levenshtein baja
            val distance = levenshteinDistance(normalizedName, existingName)
            val maxLen = maxOf(normalizedName.length, existingName.length)
            val similarity = 1.0 - (distance.toDouble() / maxLen.toDouble())
            val levenshteinMatch = similarity >= 0.7 // 70% similar
            
            // Criterio 3: Mismo nombre base (ej: "Netflix" y "Netflix Premium")
            val baseMatch = getBaseName(normalizedName) == getBaseName(existingName)
            
            containsMatch || levenshteinMatch || baseMatch
        }
    }

    /**
     * Calcular distancia de Levenshtein entre dos strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Eliminación
                    dp[i][j - 1] + 1,      // Inserción
                    dp[i - 1][j - 1] + cost // Sustitución
                )
            }
        }
        return dp[m][n]
    }

    /**
     * Obtener nombre base (primera palabra significativa)
     * "Netflix Premium" → "netflix"
     * "Amazon Prime Video" → "amazon"
     */
    private fun getBaseName(name: String): String {
        val words = name.split(" ", "-", "_").filter { it.length >= 3 }
        return words.firstOrNull() ?: name
    }
}
