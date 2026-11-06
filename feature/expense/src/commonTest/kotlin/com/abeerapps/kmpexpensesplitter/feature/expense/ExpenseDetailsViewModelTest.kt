package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.SavedStateHandle
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.DeleteExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseDetailsUseCase
import com.abeerapps.kmpexpensesplitter.feature.expense.details.ExpenseDetailsAction
import com.abeerapps.kmpexpensesplitter.feature.expense.details.ExpenseDetailsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseDetailsViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val day = LocalDate(2026, 4, 25)
    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")

    private val items = MutableStateFlow(
        listOf(
            Expense(
                id = "e1", expenseGroupId = "t", peopleGroupId = "pg", title = "Hotel", amount = Money(450_00, jod),
                category = ExpenseCategory.Hotel, paidBy = you.id, splitType = SplitType.Equal,
                // Stored out of member order on purpose: the screen lists people in group order.
                shares = listOf(ExpenseShare(sara.id, Money(225_00, jod)), ExpenseShare(you.id, Money(225_00, jod))),
                startDate = day, endDate = day, note = null,
            ),
        ),
    )
    private val expenses = object : ExpenseRepository {
        override fun observeExpenses(): Flow<List<Expense>> = items
        override suspend fun addExpense(expense: Expense) = Unit
        override suspend fun updateExpense(expense: Expense) = Unit
        override suspend fun deleteExpense(expenseId: String) { items.value = items.value.filterNot { it.id == expenseId } }
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
    }
    private val people = object : PeopleGroupRepository {
        override fun observeGroups(): Flow<List<PeopleGroup>> = MutableStateFlow(listOf(PeopleGroup("pg", "Friends", listOf(you, sara), day)))
        override suspend fun createGroup(group: PeopleGroup) = Unit
        override suspend fun updateGroup(group: PeopleGroup) = Unit
        override suspend fun deleteGroup(groupId: String) = Unit
    }
    private val profiles = object : UserProfileRepository {
        override fun observeProfile(): Flow<UserProfile?> = MutableStateFlow(UserProfile("Ahmad", jod))
        override suspend fun saveProfile(profile: UserProfile) = Unit
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = ExpenseDetailsViewModel(
        observeDetails = ObserveExpenseDetailsUseCase(expenses, people, profiles),
        deleteExpense = DeleteExpenseUseCase(expenses),
        savedStateHandle = SavedStateHandle(mapOf(ExpenseDetailsViewModel.ARG_EXPENSE_ID to "e1")),
    )

    @Test
    fun `shows payer and shares with names in group order`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        val details = vm.uiState.value.details!!
        assertEquals("Ahmad", details.payerName)
        assertEquals(listOf("Ahmad" to 225_00L, "Sara" to 225_00L), details.shares.map { it.name to it.amount.minorUnits })
    }

    @Test
    fun `delete asks first then removes the expense and closes`() = runTest {
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect {} }

        vm.onAction(ExpenseDetailsAction.DeleteClicked)
        assertTrue(vm.uiState.value.confirmingDelete)
        assertEquals(1, items.value.size) // Nothing deleted before confirming.

        vm.onAction(ExpenseDetailsAction.SheetDismissed)
        assertFalse(vm.uiState.value.confirmingDelete)

        vm.onAction(ExpenseDetailsAction.DeleteClicked)
        vm.onAction(ExpenseDetailsAction.DeleteConfirmed)
        assertTrue(items.value.isEmpty())
        assertTrue(vm.uiState.value.isMissing) // The screen closes itself.
    }
}
