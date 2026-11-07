package com.abeerapps.kmpexpensesplitter.core.data.database

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money

/**
 * Column encodings shared by the SQLDelight repositories. Changing any of these
 * is a data migration: rows already on users' devices use them.
 *
 * - Dates: ISO-8601 `yyyy-MM-dd` (`LocalDate.toString()` / `LocalDate.parse`), which sorts as text.
 * - Money: minor units (`Long`) plus the ISO 4217 code.
 * - Enums: their constant name.
 */
internal fun money(minorUnits: Long, currency: String): Money =
    Money(minorUnits, requireNotNull(CurrencyCode.of(currency)) { "Stored currency '$currency' is not ISO 4217" })
