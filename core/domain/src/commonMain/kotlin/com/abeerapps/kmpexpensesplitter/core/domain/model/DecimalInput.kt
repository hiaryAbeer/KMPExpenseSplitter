package com.abeerapps.kmpexpensesplitter.core.domain.model

/**
 * Parsing for the decimal numbers users type (amounts and percentages).
 *
 * Both are kept as integers scaled by 100 so no floating point is ever involved:
 * an amount of `12.5` → `1250` minor units, a percentage of `33.33` → `3333`.
 */
object DecimalInput {

    /** Largest whole part accepted (9 digits), far below [Long] overflow even ×100×10_000. */
    const val MAX_WHOLE_DIGITS = 9
    private const val FRACTION_DIGITS = 2

    /**
     * Cleans what the user typed into an amount/percentage field, or returns `null`
     * when the change should be rejected (so the field keeps its previous value).
     *
     * Accepts digits and one decimal separator (`.` or `,`, normalised to `.`) with
     * at most 2 decimals and [MAX_WHOLE_DIGITS] whole digits.
     */
    fun sanitize(raw: String): String? {
        val text = raw.replace(',', '.')
        if (text.isEmpty()) return text
        if (text.any { it != '.' && !it.isDigit() }) return null
        val parts = text.split('.')
        if (parts.size > 2) return null
        if (parts[0].length > MAX_WHOLE_DIGITS) return null
        if (parts.size == 2 && parts[1].length > FRACTION_DIGITS) return null
        return text
    }

    /**
     * Converts sanitized text into hundredths: `"12.5"` → `1250`, `".5"` → `50`.
     * @return `null` for empty or invalid input.
     */
    fun toHundredths(text: String): Long? {
        val clean = sanitize(text.trim()) ?: return null
        if (clean.isEmpty() || clean == ".") return null
        val whole = clean.substringBefore('.').ifEmpty { "0" }.toLong()
        val fraction = clean.substringAfter('.', "").padEnd(FRACTION_DIGITS, '0').toLong()
        return whole * 100 + fraction
    }
}
