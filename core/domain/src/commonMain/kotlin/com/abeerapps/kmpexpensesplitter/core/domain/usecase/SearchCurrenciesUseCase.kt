package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.Currency
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies

/**
 * Searches currencies by **name or ISO code** only ("dinar", "jordanian", "jod").
 * Symbols and country names are deliberately not searched.
 *
 * Matching is case- and accent-insensitive. Results are ranked:
 * exact code → code prefix → name word prefix → name substring.
 * Ties keep catalog (alphabetical) order. A blank query returns everything.
 */
class SearchCurrenciesUseCase(
    private val catalog: List<Currency> = SupportedCurrencies.all,
) {
    private val index: List<IndexedCurrency> = catalog.map { currency ->
        IndexedCurrency(
            currency = currency,
            code = currency.code.value.lowercase(),
            name = currency.name.fold(),
        )
    }

    operator fun invoke(query: String): List<Currency> {
        val q = query.fold().trim()
        if (q.isEmpty()) return catalog

        return index
            .mapNotNull { indexed -> indexed.rank(q)?.let { it to indexed.currency } }
            .sortedBy { it.first } // Stable: equal ranks keep alphabetical order.
            .map { it.second }
    }

    private class IndexedCurrency(
        val currency: Currency,
        val code: String,
        val name: String,
    ) {
        /** Lower is better; `null` = no match. */
        fun rank(q: String): Int? = when {
            code == q -> 0
            code.startsWith(q) -> 1
            name.hasWordStartingWith(q) -> 2
            name.contains(q) -> 3
            else -> null
        }
    }
}

private val WORD_SEPARATORS = Regex("[\\s\\-&’'(),.]+")

private fun String.hasWordStartingWith(prefix: String): Boolean =
    startsWith(prefix) || split(WORD_SEPARATORS).any { it.startsWith(prefix) }

/** Lower-cases and strips common Latin diacritics (no java.text in common code). */
internal fun String.fold(): String = buildString(length) {
    for (ch in this@fold.lowercase()) {
        append(
            when (ch) {
                'à', 'á', 'â', 'ã', 'ä', 'å', 'ā' -> 'a'
                'ç', 'č' -> 'c'
                'è', 'é', 'ê', 'ë', 'ē' -> 'e'
                'ì', 'í', 'î', 'ï', 'ī' -> 'i'
                'ñ' -> 'n'
                'ò', 'ó', 'ô', 'õ', 'ö', 'ø', 'ō' -> 'o'
                'š' -> 's'
                'ù', 'ú', 'û', 'ü', 'ū' -> 'u'
                'ý', 'ÿ' -> 'y'
                'ž' -> 'z'
                '’' -> '\''
                else -> ch
            },
        )
    }
}
