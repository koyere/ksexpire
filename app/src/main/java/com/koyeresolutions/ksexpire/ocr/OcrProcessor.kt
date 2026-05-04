package com.koyeresolutions.ksexpire.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*
import java.util.regex.Pattern
import kotlin.coroutines.resume

/**
 * Procesador OCR mejorado para extraer datos de recibos usando ML Kit
 * Optimizado para detección precisa de precios y fechas
 */
class OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Patrones de precio más robustos (ordenados por especificidad)
    private val pricePatterns = listOf(
        // Total/Subtotal con precio (más confiable)
        Pattern.compile("""(?:total|subtotal|monto|importe|amount|due|pagar|cobro)[\s:=]*[\$€£¥₡Q]?\s*(\d{1,6}(?:[.,]\d{1,2})?)""", Pattern.CASE_INSENSITIVE),
        // Símbolo de moneda seguido de número
        Pattern.compile("""[\$€£¥₡Q]\s*(\d{1,3}(?:[,. ]\d{3})*(?:[.,]\d{1,2}))"""),
        Pattern.compile("""[\$€£¥₡Q]\s*(\d+[.,]\d{2})"""),
        Pattern.compile("""[\$€£¥₡Q]\s*(\d+)"""),
        // Número seguido de símbolo de moneda
        Pattern.compile("""(\d{1,3}(?:[,. ]\d{3})*(?:[.,]\d{1,2}))\s*[\$€£¥₡Q]"""),
        // Número con formato de precio (X.XX o X,XX) en líneas con contexto de precio
        Pattern.compile("""(?:precio|price|costo|cost|valor)[\s:=]*(\d{1,6}(?:[.,]\d{1,2})?)""", Pattern.CASE_INSENSITIVE)
    )

    // Patrones de fecha más robustos
    private val datePatterns = listOf(
        // dd/mm/yyyy o dd-mm-yyyy o dd.mm.yyyy
        Pattern.compile("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{4})"""),
        // yyyy/mm/dd o yyyy-mm-dd
        Pattern.compile("""(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})"""),
        // dd/mm/yy
        Pattern.compile("""(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2})"""),
        // Formato texto: "13 dic 2024", "13 de diciembre 2024"
        Pattern.compile("""(\d{1,2})\s+(?:de\s+)?(?:ene|feb|mar|abr|may|jun|jul|ago|sep|oct|nov|dic)\w*\s+(\d{4})""", Pattern.CASE_INSENSITIVE)
    )

    // Meses en español para parseo
    private val monthNames = mapOf(
        "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4,
        "may" to 5, "jun" to 6, "jul" to 7, "ago" to 8,
        "sep" to 9, "oct" to 10, "nov" to 11, "dic" to 12
    )

    // Palabras a ignorar como nombre
    private val ignoreWords = setOf(
        "total", "subtotal", "iva", "tax", "cambio", "efectivo", "tarjeta",
        "gracias", "factura", "recibo", "ticket", "fecha", "hora", "caja",
        "cajero", "cliente", "rfc", "nit", "tel", "dirección", "sucursal",
        "visa", "mastercard", "debito", "credito", "vuelto", "pago",
        "folio", "serie", "no.", "num", "cantidad", "descripcion"
    )

    /**
     * Procesar imagen y extraer datos del recibo
     */
    suspend fun processImage(bitmap: Bitmap): OcrResult {
        val inputImage = InputImage.fromBitmap(bitmap, 0)
        
        val visionText = suspendCancellableCoroutine<Text?> { continuation ->
            recognizer.process(inputImage)
                .addOnSuccessListener { text ->
                    continuation.resume(text)
                }
                .addOnFailureListener { _ ->
                    continuation.resume(null)
                }
        }

        if (visionText == null || visionText.text.isBlank()) {
            return OcrResult(rawText = "")
        }

        val fullText = visionText.text
        
        // Usar bloques de texto para mejor contexto
        val blocks = visionText.textBlocks
        
        val detectedPrice = extractPrice(fullText, blocks)
        val detectedDate = extractDate(fullText)
        val detectedName = extractName(fullText, blocks)

        return OcrResult(
            detectedName = detectedName,
            detectedPrice = detectedPrice,
            detectedDate = detectedDate,
            rawText = fullText,
            confidence = calculateConfidence(detectedName, detectedPrice, detectedDate)
        )
    }

    /**
     * Extraer precio del texto — mejorado con análisis de bloques
     */
    private fun extractPrice(text: String, blocks: List<Text.TextBlock>): Double? {
        val candidates = mutableListOf<PriceCandidate>()

        // Primero buscar en el texto completo con patrones
        for ((priority, pattern) in pricePatterns.withIndex()) {
            val matcher = pattern.matcher(text)
            while (matcher.find()) {
                val priceStr = matcher.group(1) ?: continue
                val normalized = normalizePrice(priceStr)
                if (normalized != null && normalized > 0 && normalized < 1_000_000) {
                    candidates.add(PriceCandidate(normalized, priority))
                }
            }
        }

        // También buscar en cada bloque individualmente (mejor contexto)
        for (block in blocks) {
            val blockText = block.text
            for ((priority, pattern) in pricePatterns.withIndex()) {
                val matcher = pattern.matcher(blockText)
                while (matcher.find()) {
                    val priceStr = matcher.group(1) ?: continue
                    val normalized = normalizePrice(priceStr)
                    if (normalized != null && normalized > 0 && normalized < 1_000_000) {
                        // Dar más peso a bloques que contienen "total"
                        val bonus = if (blockText.lowercase().contains("total")) -5 else 0
                        candidates.add(PriceCandidate(normalized, priority + bonus))
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Estrategia: preferir el precio del patrón "total", luego el más alto
        val sorted = candidates.sortedWith(compareBy({ it.priority }, { -it.value }))
        return sorted.firstOrNull()?.value
    }

    /**
     * Normalizar string de precio a Double — mejorado
     */
    private fun normalizePrice(priceStr: String): Double? {
        return try {
            var cleaned = priceStr.trim()
            
            // Remover espacios entre dígitos (1 234,56 → 1234,56)
            cleaned = cleaned.replace(" ", "")
            
            // Corregir errores comunes de OCR en números
            cleaned = cleaned.replace("O", "0").replace("o", "0")
                .replace("l", "1").replace("I", "1")
                .replace("S", "5").replace("B", "8")
            
            // Determinar separador decimal
            val lastComma = cleaned.lastIndexOf(",")
            val lastDot = cleaned.lastIndexOf(".")
            
            cleaned = when {
                // Solo tiene coma y es decimal (X,XX)
                lastComma > 0 && lastDot < 0 && cleaned.length - lastComma <= 3 -> {
                    cleaned.replace(",", ".")
                }
                // Solo tiene punto y es decimal (X.XX)
                lastDot > 0 && lastComma < 0 && cleaned.length - lastDot <= 3 -> {
                    cleaned
                }
                // Tiene ambos: 1.234,56 (punto miles, coma decimal)
                lastComma > lastDot -> {
                    cleaned.replace(".", "").replace(",", ".")
                }
                // Tiene ambos: 1,234.56 (coma miles, punto decimal)
                lastDot > lastComma -> {
                    cleaned.replace(",", "")
                }
                // Coma como miles sin decimal (1,234)
                lastComma > 0 && cleaned.length - lastComma == 4 -> {
                    cleaned.replace(",", "")
                }
                // Punto como miles sin decimal (1.234)
                lastDot > 0 && cleaned.length - lastDot == 4 -> {
                    cleaned.replace(".", "")
                }
                else -> cleaned
            }
            
            cleaned.toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Extraer fecha del texto — mejorado
     */
    private fun extractDate(text: String): Long? {
        // Buscar formato con nombre de mes primero (más confiable)
        val monthPattern = datePatterns.last()
        val monthMatcher = monthPattern.matcher(text)
        if (monthMatcher.find()) {
            return try {
                val day = monthMatcher.group(1)?.toIntOrNull() ?: return null
                val monthStr = text.substring(monthMatcher.start(), monthMatcher.end())
                    .lowercase()
                val month = monthNames.entries.find { monthStr.contains(it.key) }?.value ?: return null
                val year = monthMatcher.group(2)?.toIntOrNull() ?: return null
                
                val calendar = Calendar.getInstance()
                calendar.set(year, month - 1, day, 0, 0, 0)
                calendar.timeInMillis
            } catch (e: Exception) {
                null
            }
        }

        // Buscar formatos numéricos
        for (pattern in datePatterns.dropLast(1)) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return try {
                    parseNumericDate(matcher, pattern)
                } catch (e: Exception) {
                    null
                }
            }
        }
        return null
    }

    /**
     * Parsear fecha numérica
     */
    private fun parseNumericDate(matcher: java.util.regex.Matcher, pattern: Pattern): Long? {
        val g1 = matcher.group(1)?.toIntOrNull() ?: return null
        val g2 = matcher.group(2)?.toIntOrNull() ?: return null
        val g3 = matcher.group(3)?.toIntOrNull() ?: return null

        val calendar = Calendar.getInstance()
        
        return when {
            // yyyy-mm-dd
            g1 > 1900 -> {
                if (g2 in 1..12 && g3 in 1..31) {
                    calendar.set(g1, g2 - 1, g3, 0, 0, 0)
                    calendar.timeInMillis
                } else null
            }
            // dd/mm/yy o dd/mm/yyyy
            else -> {
                val day = g1
                val month = g2
                var year = g3
                if (year < 100) year += 2000
                
                if (day in 1..31 && month in 1..12 && year in 2000..2100) {
                    calendar.set(year, month - 1, day, 0, 0, 0)
                    calendar.timeInMillis
                } else null
            }
        }
    }

    /**
     * Extraer nombre — mejorado con análisis de bloques
     */
    private fun extractName(text: String, blocks: List<Text.TextBlock>): String? {
        // Estrategia 1: Buscar el bloque más grande/prominente (generalmente el nombre de la tienda)
        val candidateBlocks = blocks
            .filter { block ->
                val blockText = block.text.trim()
                blockText.length in 3..50 &&
                !blockText.all { it.isDigit() || it == '.' || it == ',' || it == ' ' || it == '/' || it == '-' } &&
                !ignoreWords.any { blockText.lowercase().startsWith(it) } &&
                !pricePatterns.any { it.matcher(blockText).find() } &&
                !datePatterns.any { it.matcher(blockText).find() }
            }
            .sortedByDescending { it.boundingBox?.width() ?: 0 }

        // El bloque más ancho suele ser el nombre de la tienda/producto
        val topBlock = candidateBlocks.firstOrNull()
        if (topBlock != null) {
            val name = topBlock.text.trim().lines().first().trim()
            if (name.length in 3..40) return name
        }

        // Estrategia 2: Fallback a primera línea válida
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        for (line in lines) {
            if (line.length < 3 || line.length > 50) continue
            if (line.all { it.isDigit() || it == '.' || it == ',' || it == ' ' || it == '/' || it == '-' }) continue
            if (datePatterns.any { it.matcher(line).find() }) continue
            if (pricePatterns.any { it.matcher(line).find() }) continue
            val lowerLine = line.lowercase()
            if (ignoreWords.any { lowerLine.startsWith(it) || lowerLine == it }) continue
            if (lowerLine.contains("tel") || lowerLine.contains("dir") || 
                lowerLine.contains("col.") || lowerLine.contains("c.p.") ||
                lowerLine.contains("www.") || lowerLine.contains("@")) continue
            
            return line.take(40)
        }

        return null
    }

    /**
     * Calcular confianza del resultado
     */
    private fun calculateConfidence(name: String?, price: Double?, date: Long?): Float {
        var score = 0f
        if (name != null) score += 0.4f
        if (price != null) score += 0.35f
        if (date != null) score += 0.25f
        return score
    }

    /**
     * Candidato de precio con prioridad
     */
    private data class PriceCandidate(val value: Double, val priority: Int)

    /**
     * Liberar recursos
     */
    fun close() {
        recognizer.close()
    }
}
