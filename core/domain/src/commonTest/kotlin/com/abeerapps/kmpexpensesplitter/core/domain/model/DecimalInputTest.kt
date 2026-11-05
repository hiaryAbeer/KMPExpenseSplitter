package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DecimalInputTest {

    @Test
    fun `sanitize accepts digits and one separator with up to two decimals`() {
        assertEquals("12.5", DecimalInput.sanitize("12.5"))
        assertEquals("12.50", DecimalInput.sanitize("12,50"))
        assertEquals("", DecimalInput.sanitize(""))
        assertEquals(".", DecimalInput.sanitize("."))
    }

    @Test
    fun `sanitize rejects letters and extra decimals and separators`() {
        assertNull(DecimalInput.sanitize("12a"))
        assertNull(DecimalInput.sanitize("1.234"))
        assertNull(DecimalInput.sanitize("1.2.3"))
        assertNull(DecimalInput.sanitize("-5"))
        assertNull(DecimalInput.sanitize("1234567890"))
    }

    @Test
    fun `toHundredths converts to minor units`() {
        assertEquals(1250L, DecimalInput.toHundredths("12.5"))
        assertEquals(1205L, DecimalInput.toHundredths("12.05"))
        assertEquals(50L, DecimalInput.toHundredths(".5"))
        assertEquals(1200L, DecimalInput.toHundredths("12."))
        assertEquals(0L, DecimalInput.toHundredths("0"))
    }

    @Test
    fun `toHundredths returns null for empty or invalid input`() {
        assertNull(DecimalInput.toHundredths(""))
        assertNull(DecimalInput.toHundredths("."))
        assertNull(DecimalInput.toHundredths("abc"))
    }
}
