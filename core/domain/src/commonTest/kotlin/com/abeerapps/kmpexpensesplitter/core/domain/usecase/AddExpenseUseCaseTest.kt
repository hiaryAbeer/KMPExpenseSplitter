package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitError
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitInput
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AddExpenseUseCaseTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val people = listOf(Participant("a", "Ahmad"), Participant("s", "Sara"))
    private val repository = FakeExpenseRepository()
    private val useCase = AddExpenseUseCase(repository, newId = { "id-1" })

    private fun draft(
        expenseGroupId: String? = "t1",
        peopleGroupId: String? = "g1",
        startDate: LocalDate = LocalDate(2026, 4, 29),
        endDate: LocalDate = startDate,
        title: String = "  Dinner  ",
        amount: Long = 30_00,
        paidBy: String? = "a",
        split: SplitInput = SplitInput.Equal(listOf("a", "s")),
        note: String = "  ",
    ) = ExpenseDraft(
        expenseGroupId = expenseGroupId,
        peopleGroupId = peopleGroupId,
        title = title,
        amount = Money(amount, jod),
        category = ExpenseCategory.Food,
        paidBy = paidBy,
        split = split,
        startDate = startDate,
        endDate = endDate,
        note = note,
        participants = people,
    )

    @Test
    fun `valid draft is stored with trimmed text and computed shares`() = runTest {
        val result = useCase(draft())

        val expected = Expense(
            id = "id-1",
            expenseGroupId = "t1",
            peopleGroupId = "g1",
            title = "Dinner",
            amount = Money(30_00, jod),
            category = ExpenseCategory.Food,
            paidBy = "a",
            splitType = SplitType.Equal,
            shares = listOf(ExpenseShare("a", Money(15_00, jod)), ExpenseShare("s", Money(15_00, jod))),
            startDate = LocalDate(2026, 4, 29),
            endDate = LocalDate(2026, 4, 29),
            note = null,
        )
        assertEquals(AddExpenseResult.Success(expected), result)
        assertEquals(listOf(expected), repository.stored)
    }

    @Test
    fun `blank title and zero amount and missing payer are reported together`() = runTest {
        val result = useCase(draft(title = " ", amount = 0, paidBy = null))

        assertEquals(
            AddExpenseResult.Invalid(
                listOf(ExpenseDraftError.BlankTitle, ExpenseDraftError.NoPayer, ExpenseDraftError.NonPositiveAmount),
            ),
            result,
        )
        assertTrue(repository.stored.isEmpty())
    }

    @Test
    fun `a people group is required`() {
        assertEquals(listOf(ExpenseDraftError.NoPeopleGroup), useCase.validate(draft(peopleGroupId = null)))
    }

    @Test
    fun `an expense group is required`() {
        assertEquals(listOf(ExpenseDraftError.NoExpenseGroup), useCase.validate(draft(expenseGroupId = null)))
    }

    @Test
    fun `a date range is stored and must not end before it starts`() = runTest {
        val start = LocalDate(2026, 4, 27)
        val end = LocalDate(2026, 4, 29)
        val stored = (useCase(draft(startDate = start, endDate = end)) as AddExpenseResult.Success).expense
        assertEquals(start to end, stored.startDate to stored.endDate)
        assertEquals(listOf(ExpenseDraftError.EndBeforeStart), useCase.validate(draft(startDate = end, endDate = start)))
    }

    @Test
    fun `split errors are reported`() {
        val errors = useCase.validate(draft(split = SplitInput.Percentage(mapOf("a" to 50_00L, "s" to 40_00L))))
        assertEquals(listOf(ExpenseDraftError.InvalidSplit(SplitError.PercentagesNotHundred)), errors)
    }

    @Test
    fun `people outside the participant list are rejected`() {
        assertEquals(listOf(ExpenseDraftError.UnknownParticipant), useCase.validate(draft(paidBy = "x")))
        assertEquals(
            listOf(ExpenseDraftError.UnknownParticipant),
            useCase.validate(draft(split = SplitInput.Exact(mapOf("x" to 30_00L)))),
        )
    }

    @Test
    fun `overlong title and note are rejected`() {
        val errors = useCase.validate(draft(title = "a".repeat(Expense.MAX_TITLE_LENGTH + 1), note = "n".repeat(Expense.MAX_NOTE_LENGTH + 1)))
        assertEquals(listOf(ExpenseDraftError.TitleTooLong, ExpenseDraftError.NoteTooLong), errors)
    }

    @Test
    fun `storage failure is reported instead of crashing`() = runTest {
        repository.failNext = true
        assertEquals(AddExpenseResult.StorageError, useCase(draft()))
    }

    private class FakeExpenseRepository : ExpenseRepository {
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
        val stored = mutableListOf<Expense>()
        var failNext = false
        override fun observeExpenses(): Flow<List<Expense>> = MutableStateFlow(stored.toList())
        override suspend fun addExpense(expense: Expense) {
            if (failNext) {
                failNext = false
                error("disk full")
            }
            stored += expense
        }
    }
}
