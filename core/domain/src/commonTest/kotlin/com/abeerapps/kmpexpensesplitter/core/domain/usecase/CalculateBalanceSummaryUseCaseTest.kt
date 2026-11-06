package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalculateBalanceSummaryUseCaseTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val usd = CurrencyCode.of("USD")!!
    private val calculate = CalculateBalanceSummaryUseCase()

    private fun group(balance: Long, currency: CurrencyCode = jod) = GroupSummary(
        id = balance.toString(),
        name = "G$balance",
        memberNames = listOf("A"),
        totalSpent = Money(100_00, currency),
        yourBalance = Money(balance, currency),
        lastActivity = kotlinx.datetime.LocalDate(2026, 4, 29),
    )

    @Test
    fun `no groups gives zero totals in the default currency`() {
        val summary = calculate(emptyList(), jod)
        assertEquals(Money.zero(jod), summary.owedToYou)
        assertEquals(Money.zero(jod), summary.youOwe)
    }

    @Test
    fun `positive and negative balances are summed separately`() {
        // Matches the design: +85.25 and +12.50 owed to you, -45.00 you owe.
        val summary = calculate(listOf(group(85_25), group(-45_00), group(12_50), group(0)), jod)
        assertEquals(Money(97_75, jod), summary.owedToYou)
        assertEquals(Money(45_00, jod), summary.youOwe)
    }

    @Test
    fun `groups in other currencies are not mixed in`() {
        val summary = calculate(listOf(group(10_00), group(99_00, usd)), jod)
        assertEquals(Money(10_00, jod), summary.owedToYou)
    }

    @Test
    fun `adding different currencies is rejected`() {
        assertFailsWith<IllegalArgumentException> { Money(1, jod) + Money(1, usd) }
    }
}
