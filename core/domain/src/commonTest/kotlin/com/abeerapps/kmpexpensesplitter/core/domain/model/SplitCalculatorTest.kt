package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class SplitCalculatorTest {

    private val jod = CurrencyCode.of("JOD")!!
    private fun money(units: Long) = Money(units, jod)

    private fun shares(result: SplitCalculator.Result): Map<String, Long> =
        (result as SplitCalculator.Result.Success).shares.associate { it.participantId to it.amount.minorUnits }

    @Test
    fun `equal split divides evenly`() {
        val result = SplitCalculator.split(money(100_00), SplitInput.Equal(listOf("a", "b", "c", "d")))
        assertEquals(mapOf("a" to 25_00L, "b" to 25_00L, "c" to 25_00L, "d" to 25_00L), shares(result))
    }

    @Test
    fun `equal split gives leftover cents to the first people and never loses a cent`() {
        val result = shares(SplitCalculator.split(money(100_00), SplitInput.Equal(listOf("a", "b", "c"))))
        assertEquals(mapOf("a" to 33_34L, "b" to 33_33L, "c" to 33_33L), result)
        assertEquals(100_00L, result.values.sum())
    }

    @Test
    fun `equal split ignores duplicate ids`() {
        val result = shares(SplitCalculator.split(money(10_00), SplitInput.Equal(listOf("a", "a", "b"))))
        assertEquals(mapOf("a" to 5_00L, "b" to 5_00L), result)
    }

    @Test
    fun `equal split without people fails`() {
        assertEquals(
            SplitCalculator.Result.Failure(SplitError.NoParticipants),
            SplitCalculator.split(money(10_00), SplitInput.Equal(emptyList())),
        )
    }

    @Test
    fun `percentage split applies percentages and rounds by largest remainder`() {
        // 10.00 at 33.33 / 33.33 / 33.34 → 3.333, 3.333, 3.334 → 3.33, 3.33, 3.34
        val input = SplitInput.Percentage(mapOf("a" to 33_33L, "b" to 33_33L, "c" to 33_34L))
        val result = shares(SplitCalculator.split(money(10_00), input))
        assertEquals(mapOf("a" to 3_33L, "b" to 3_33L, "c" to 3_34L), result)
    }

    @Test
    fun `percentage split skips zero percent people`() {
        val input = SplitInput.Percentage(mapOf("a" to 100_00L, "b" to 0L))
        assertEquals(mapOf("a" to 50_00L), shares(SplitCalculator.split(money(50_00), input)))
    }

    @Test
    fun `percentages must total exactly 100`() {
        val input = SplitInput.Percentage(mapOf("a" to 50_00L, "b" to 49_99L))
        assertEquals(
            SplitCalculator.Result.Failure(SplitError.PercentagesNotHundred),
            SplitCalculator.split(money(10_00), input),
        )
    }

    @Test
    fun `exact split keeps the amounts`() {
        val input = SplitInput.Exact(mapOf("a" to 7_50L, "b" to 2_50L))
        assertEquals(mapOf("a" to 7_50L, "b" to 2_50L), shares(SplitCalculator.split(money(10_00), input)))
    }

    @Test
    fun `exact amounts must match the total`() {
        val input = SplitInput.Exact(mapOf("a" to 7_50L, "b" to 2_49L))
        assertEquals(
            SplitCalculator.Result.Failure(SplitError.AmountsNotMatchingTotal),
            SplitCalculator.split(money(10_00), input),
        )
    }

    @Test
    fun `exact split with only zeros has no participants`() {
        assertEquals(
            SplitCalculator.Result.Failure(SplitError.NoParticipants),
            SplitCalculator.split(money(10_00), SplitInput.Exact(mapOf("a" to 0L))),
        )
    }
}
