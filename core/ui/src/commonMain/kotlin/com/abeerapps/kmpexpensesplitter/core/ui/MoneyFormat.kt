package com.abeerapps.kmpexpensesplitter.core.ui

import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import kotlin.math.absoluteValue

/**
 * Formats money as in the design: `97.75 JOD`, or `+85.25 JOD` / `-45.00 JOD`
 * with [showSign]. Two decimals, no grouping separators, code after the amount.
 *
 * Kept in common code (no platform NumberFormat) so Android and iOS render
 * identical strings; swap for locale-aware formatting when the app is localized.
 */
fun Money.format(showSign: Boolean = false): String {
    val abs = minorUnits.absoluteValue
    val whole = abs / Money.SCALE
    val fraction = (abs % Money.SCALE).toString().padStart(2, '0')
    val sign = when {
        isNegative -> "-"
        showSign && isPositive -> "+"
        else -> ""
    }
    return "$sign$whole.$fraction ${currency.value}"
}
