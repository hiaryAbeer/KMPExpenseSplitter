package com.abeerapps.kmpexpensesplitter.feature.home

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals

class MoneyFormatTest {

    private val jod = CurrencyCode.of("JOD")!!

    @Test
    fun `formats like the design`() {
        assertEquals("97.75 JOD", Money(97_75, jod).format())
        assertEquals("1250.50 JOD", Money(1250_50, jod).format())
        assertEquals("0.00 JOD", Money.zero(jod).format())
    }

    @Test
    fun `pads single digit fractions`() {
        assertEquals("12.05 JOD", Money(12_05, jod).format())
    }

    @Test
    fun `shows sign only when asked and never for zero`() {
        assertEquals("+85.25 JOD", Money(85_25, jod).format(showSign = true))
        assertEquals("-45.00 JOD", Money(-45_00, jod).format(showSign = true))
        assertEquals("-45.00 JOD", Money(-45_00, jod).format())
        assertEquals("0.00 JOD", Money.zero(jod).format(showSign = true))
    }
}
