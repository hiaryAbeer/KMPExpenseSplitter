package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class ExpenseGroupDetailsTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val omar = Participant("o", "Omar")
    private val trip = ExpenseGroup("t", "Aqaba Trip", LocalDate(2026, 4, 1))
    private val friends = PeopleGroup("pg1", "Friends", listOf(you, sara), LocalDate(2026, 4, 1))
    private val family = PeopleGroup("pg2", "Family", listOf(you, omar), LocalDate(2026, 4, 1))

    private fun expense(
        id: String,
        group: String,
        paidBy: String,
        total: Long,
        shares: Map<String, Long>,
        category: ExpenseCategory,
        day: Int,
        trip: String = "t",
        currency: CurrencyCode = jod,
    ) = Expense(
        id = id, expenseGroupId = trip, peopleGroupId = group, title = id, amount = Money(total, currency),
        category = category, paidBy = paidBy, splitType = SplitType.Exact,
        shares = shares.map { (who, units) -> ExpenseShare(who, Money(units, currency)) },
        startDate = LocalDate(2026, 4, day), endDate = LocalDate(2026, 4, day), note = null,
    )

    private val expenses = listOf(
        // You paid 90 for the hotel, split with Sara.
        expense("hotel", "pg1", you.id, 90_00, mapOf(you.id to 45_00L, sara.id to 45_00L), ExpenseCategory.Hotel, day = 24),
        // Omar paid 30 for dinner, split with you.
        expense("dinner", "pg2", omar.id, 30_00, mapOf(you.id to 15_00L, omar.id to 15_00L), ExpenseCategory.Food, day = 25),
        // Another trip and another currency: both ignored.
        expense("other", "pg1", sara.id, 10_00, mapOf(you.id to 10_00L), ExpenseCategory.Food, day = 25, trip = "x"),
        expense("usd", "pg1", sara.id, 10_00, mapOf(you.id to 10_00L), ExpenseCategory.Food, day = 25, currency = CurrencyCode.of("USD")!!),
    )

    private val details = ExpenseGroupDetails.build(trip, listOf(friends, family), expenses, jod)

    @Test
    fun `lists the group's expenses newest first with payer names`() {
        assertEquals(listOf("dinner" to "Omar", "hotel" to "Ahmad"), details.expenses.map { it.expense.id to it.payerName })
        assertEquals(Money(120_00, jod), details.totalSpent)
        assertEquals(listOf("Ahmad", "Sara", "Omar"), details.memberNames)
    }

    @Test
    fun `balances are paid minus shares across people groups`() {
        // You: paid 90, owe 45 + 15 → +30. Omar: paid 30, owes 15 → +15. Sara: owes 45 → -45.
        assertEquals(
            listOf("Ahmad" to 30_00L, "Omar" to 15_00L, "Sara" to -45_00L),
            details.balances.map { it.name to it.balance.minorUnits },
        )
        assertEquals(0L, details.balances.sumOf { it.balance.minorUnits }) // Money is never created or lost.
    }

    @Test
    fun `stats total each category with its share`() {
        assertEquals(
            listOf(Triple(ExpenseCategory.Hotel, 90_00L, 0.75), Triple(ExpenseCategory.Food, 30_00L, 0.25)),
            details.categoryTotals.map { Triple(it.category, it.total.minorUnits, it.share) },
        )
    }
}
