package com.koyeresolutions.ksexpire.ocr

/**
 * Resultado del procesamiento OCR de un recibo
 */
data class OcrResult(
    val detectedName: String? = null,
    val detectedPrice: Double? = null,
    val detectedDate: Long? = null,
    val rawText: String = "",
    val confidence: Float = 0f
) {
    fun hasAnyData(): Boolean = detectedName != null || detectedPrice != null || detectedDate != null
}
