package com.abeerapps.kmpexpensesplitter.core.domain.model

/**
 * An amount of money in a currency.
 *
 * Stored as an integer number of hundredths ([minorUnits]) so arithmetic is
 * exact; never use floating point for money. The design shows two decimals for
 * every currency (e.g. "97.75 JOD"), so the scale is fixed at 2 for now.
 *
 * @property minorUnits amount × 100, e.g. 9775 for 97.75. Negative = debt.
 */
data class Money(
    val minorUnits: Long,
    val currency: CurrencyCode,
) {
    val isPositive: Boolean get() = minorUnits > 0
    val isNegative: Boolean get() = minorUnits < 0
    val isZero: Boolean get() = minorUnits == 0L

    operator fun plus(other: Money): Money {
        require(currency == other.currency) { "Cannot add $currency and ${other.currency}" }
        return Money(minorUnits + other.minorUnits, currency)
    }

    operator fun unaryMinus(): Money = Money(-minorUnits, currency)

    companion object {
        const val SCALE = 100

        fun zero(currency: CurrencyCode) = Money(0, currency)
    }
}
