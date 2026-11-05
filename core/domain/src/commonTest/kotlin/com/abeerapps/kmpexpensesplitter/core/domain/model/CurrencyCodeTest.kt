package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrencyCodeTest {

    @Test
    fun `valid upper-case code is accepted`() {
        assertEquals("USD", CurrencyCode.of("USD")?.value)
    }

    @Test
    fun `input is trimmed and upper-cased`() {
        assertEquals("JOD", CurrencyCode.of("  jod ")?.value)
    }

    @Test
    fun `invalid codes are rejected`() {
        listOf("", "US", "USDT", "U5D", "€UR", "   ").forEach { raw ->
            assertNull(CurrencyCode.of(raw), "expected '$raw' to be rejected")
        }
    }
}
