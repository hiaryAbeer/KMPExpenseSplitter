package com.abeerapps.kmpexpensesplitter.core.domain.model

/**
 * A currency the user can pick as their default.
 *
 * @property code ISO 4217 code, the identity of the currency (what gets stored).
 * @property symbol display symbol from CLDR: the English one when distinct from the
 *   code (`$`, `€`, `CA$`), else the issuing country's local one (`د.أ.`, `₸`), else
 *   empty, so the UI never shows the code twice.
 * @property name English display name. Storage only ever uses [code].
 * @property countries English names of the countries/territories where it is
 *   legal tender (not searchable; kept for future use, e.g. a locale-based default).
 */
data class Currency(
    val code: CurrencyCode,
    val symbol: String,
    val name: String,
    val countries: List<String> = emptyList(),
)

/**
 * Every currency in legal tender today (from Unicode CLDR), sorted by name.
 * Data lives in the generated `CurrencyData.kt`; see `scripts/generate_currencies.py`.
 */
object SupportedCurrencies {

    val all: List<Currency> = CURRENCY_DATA

    private val byCode: Map<CurrencyCode, Currency> = all.associateBy { it.code }

    /** Preselected on the registration screen: Jordanian Dinar. */
    val default: Currency = requireNotNull(byCode[CurrencyCode.of("JOD")]) { "JOD missing from catalog" }

    /** Looks up a supported currency by code, or `null` if unsupported. */
    fun find(code: CurrencyCode): Currency? = byCode[code]
}
