package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupportedCurrenciesTest {

    @Test
    fun `catalog covers the world's currencies`() {
        assertTrue(SupportedCurrencies.all.size > 140, "got ${SupportedCurrencies.all.size}")
    }

    @Test
    fun `codes are unique`() {
        val codes = SupportedCurrencies.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun `catalog is sorted by name`() {
        val names = SupportedCurrencies.all.map { it.name.lowercase() }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `symbols never repeat the code and every currency has a country`() {
        SupportedCurrencies.all.forEach {
            assertTrue(it.symbol != it.code.value, "${it.code} symbol repeats the code")
            assertTrue(it.symbol == it.symbol.trim(), "${it.code} symbol has stray whitespace")
            assertTrue(it.countries.isNotEmpty(), "${it.code} has no country")
        }
    }

    @Test
    fun `local symbol is used when English has none`() {
        assertEquals("د.أ.", SupportedCurrencies.default.symbol)
    }

    @Test
    fun `default is Jordanian Dinar used in Jordan only`() {
        val default = SupportedCurrencies.default
        assertEquals("JOD", default.code.value)
        assertEquals("Jordanian Dinar", default.name)
        assertEquals(listOf("Jordan"), default.countries)
    }

    // Product decisions applied by scripts/generate_currencies.py on top of CLDR.

    @Test
    fun `shekel is listed as New Shekel under Palestine`() {
        val shekel = SupportedCurrencies.find(CurrencyCode.of("ILS")!!)!!
        assertEquals("New Shekel", shekel.name)
        assertEquals(listOf("Palestine"), shekel.countries)
    }

    @Test
    fun `Israel does not appear anywhere in the catalog`() {
        SupportedCurrencies.all.forEach { currency ->
            assertTrue("israel" !in currency.name.lowercase(), "${currency.code} name mentions Israel")
            currency.countries.forEach { country ->
                assertTrue("israel" !in country.lowercase(), "${currency.code} lists $country")
            }
        }
    }

    @Test
    fun `Palestine is always named Palestine`() {
        val countries = SupportedCurrencies.all.flatMap { it.countries }
        assertTrue("Palestinian Territories" !in countries)
        assertTrue("Palestine" in countries)
    }

    @Test
    fun `find returns supported currency or null`() {
        assertEquals("Euro", SupportedCurrencies.find(CurrencyCode.of("EUR")!!)?.name)
        assertNull(SupportedCurrencies.find(CurrencyCode.of("XYZ")!!))
    }
}
