package com.abeerapps.kmpexpensesplitter.feature.groups

import com.abeerapps.kmpexpensesplitter.feature.groups.details.ChartScale
import kotlin.test.Test
import kotlin.test.assertEquals

class ChartScaleTest {

    @Test
    fun `ticks round up to a nice scale`() {
        // The design: Ahmad paid about 590 JOD → 0, 150, 300, 450, 600.
        assertEquals(listOf(0L, 150_00L, 300_00L, 450_00L, 600_00L), ChartScale.ticks(590_00))
        assertEquals(listOf(0L, 250L, 500L, 750L, 1_000L), ChartScale.ticks(10_00))
        assertEquals(listOf(0L, 30_00L, 60_00L, 90_00L, 120_00L), ChartScale.ticks(120_00))
    }

    @Test
    fun `an empty chart still gets a scale`() {
        assertEquals(listOf(0L, 25L, 50L, 75L, 100L), ChartScale.ticks(0))
    }

    @Test
    fun `labels drop needless decimals`() {
        assertEquals(listOf("0", "150", "2.5", "0.25"), listOf(0L, 150_00L, 250L, 25L).map(ChartScale::label))
    }
}
