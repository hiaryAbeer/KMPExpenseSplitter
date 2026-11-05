package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchCurrenciesUseCaseTest {

    private val search = SearchCurrenciesUseCase()

    private fun codes(query: String) = search(query).map { it.code.value }

    @Test
    fun `blank query returns the full catalog`() {
        assertEquals(SupportedCurrencies.all.size, search("   ").size)
    }

    @Test
    fun `exact code ranks first and is case insensitive`() {
        assertEquals("JOD", codes("JOD").first())
        assertEquals("JOD", codes("jod").first())
    }

    @Test
    fun `code prefix matches`() {
        assertTrue(codes("us").first() == "USD")
    }

    @Test
    fun `currency name prefix matches`() {
        assertEquals("JOD", codes("Jordanian").first())
    }

    @Test
    fun `word inside currency name matches`() {
        assertTrue(codes("dinar").containsAll(listOf("JOD", "KWD", "BHD")))
    }

    @Test
    fun `code match ranks above name match`() {
        // "eur" is EUR's code and also inside no other code; Euro must come first.
        assertEquals("EUR", codes("eur").first())
    }

    @Test
    fun `country name is not searchable`() {
        assertTrue(search("germany").isEmpty())
        assertTrue(search("palestine").isEmpty())
    }

    @Test
    fun `symbol is not searchable`() {
        assertTrue(search("€").isEmpty())
        assertTrue(search("$").isEmpty())
    }

    @Test
    fun `accents are ignored in names`() {
        // "Sao Tome & Principe Dobra" in CLDR is spelled "São Tomé & Príncipe Dobra".
        assertEquals("STN", codes("sao tome").first())
    }

    @Test
    fun `unknown query returns nothing`() {
        assertTrue(search("zzzz").isEmpty())
    }
}
