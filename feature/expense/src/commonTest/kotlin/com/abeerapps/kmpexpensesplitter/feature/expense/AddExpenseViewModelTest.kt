package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseShare
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseCategory
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.model.SplitType
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.AddExpenseUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpensesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraftError
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObservePeopleGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    private val today = LocalDate(2026, 4, 29)
    private val usd = CurrencyCode.of("USD")!!
    private val profiles = FakeProfiles(UserProfile("Ahmad", usd))
    private val expenses = FakeExpenses()
    private val groups = FakeGroups()
    private val aqaba = ExpenseGroup("t1", "Aqaba Trip", today)
    private val trips = FakeExpenseGroups(listOf(aqaba))

    private val you = Participant(Participant.YOU_ID, "Ahmad")
    private val sara = Participant("s", "Sara")
    private val trip = PeopleGroup("g1", "Trip", listOf(you, sara), today)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private val dateProvider = DateProvider { today }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = AddExpenseViewModel(
        observeExpenseGroups = ObserveExpenseGroupsUseCase(trips),
        observeGroupList = ObservePeopleGroupsUseCase(groups, profiles),
        observeUserProfile = ObserveUserProfileUseCase(profiles),
        addExpense = AddExpenseUseCase(expenses, newId = { "e1" }),
        observeExpenses = ObserveExpensesUseCase(expenses),
        dateProvider = dateProvider,
        savedStateHandle = savedState,
    )

    /** A valid form for a one-person split, so share inputs are easy to reason about. */
    private fun AddExpenseViewModel.fillValidForm() {
        groups.groups.value = listOf(trip)
        onAction(AddExpenseAction.GroupSelected(trip.id))
        onAction(AddExpenseAction.ParticipantToggled(sara.id)) // Only you share.
        onAction(AddExpenseAction.TitleChanged("Lunch"))
        onAction(AddExpenseAction.AmountChanged("12.50"))
    }

    @Test
    fun `starts with no group so split between is empty and you pay`() {
        val state = viewModel().uiState.value
        assertFalse(state.isLoading)
        assertEquals(usd, state.currency)
        assertNull(state.group)
        assertTrue(state.participants.isEmpty())
        assertEquals(listOf(you), state.payerOptions)
        assertEquals(Participant.YOU_ID, state.paidBy)
        assertEquals(today to today, state.startDate to state.endDate)
        assertEquals(ExpenseCategory.Food, state.category)
        assertFalse(state.isSplitHintError) // Disabled section: no red hint.
    }

    @Test
    fun `a group is required to save`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.TitleChanged("Lunch"))
        vm.onAction(AddExpenseAction.AmountChanged("12.50"))
        assertFalse(vm.uiState.value.canSave)
        assertTrue(ExpenseDraftError.NoPeopleGroup in vm.uiState.value.errors)

        groups.groups.value = listOf(trip)
        vm.onAction(AddExpenseAction.GroupSelected(trip.id))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `selecting a group lists and ticks its members and closes the sheet`() {
        groups.groups.value = listOf(trip)
        val vm = viewModel()
        vm.onAction(AddExpenseAction.GroupClicked)
        assertEquals(AddExpenseSheet.PeopleGroup, vm.uiState.value.openSheet)

        vm.onAction(AddExpenseAction.GroupSelected(trip.id))

        val state = vm.uiState.value
        assertNull(state.openSheet)
        assertEquals(trip, state.group)
        assertEquals(listOf(you, sara), state.participants)
        assertEquals(setOf(you.id, sara.id), state.selected)
        assertEquals(you.id, state.paidBy)
    }

    @Test
    fun `members cannot be toggled without a group`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.ParticipantToggled(you.id))
        assertTrue(vm.uiState.value.selected.isEmpty())
    }

    @Test
    fun `existing groups preselect the first one`() {
        val older = PeopleGroup("g0", "Older", listOf(you), LocalDate(2026, 1, 1))
        groups.groups.value = listOf(trip, older)

        val state = viewModel().uiState.value

        assertEquals(trip, state.group)
        assertEquals(setOf(you.id, sara.id), state.selected)
    }

    @Test
    fun `manage buttons close the sheet and open the manage screens`() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(AddExpenseAction.ExpenseGroupClicked)
            vm.onAction(AddExpenseAction.ManageExpenseGroupsClicked)
            assertNull(vm.uiState.value.openSheet)
            assertEquals(AddExpenseEvent.OpenManageExpenseGroups, awaitItem())

            vm.onAction(AddExpenseAction.GroupClicked)
            vm.onAction(AddExpenseAction.ManagePeopleGroupsClicked)
            assertNull(vm.uiState.value.openSheet)
            assertEquals(AddExpenseEvent.OpenManagePeopleGroups, awaitItem())
        }
    }

    @Test
    fun `groups added on the manage screens are selected when none is`() {
        trips.groups.value = emptyList()
        val vm = viewModel()
        vm.onAction(AddExpenseAction.TitleChanged("Lunch")) // Typed before leaving: kept.
        assertNull(vm.uiState.value.expenseGroup)
        assertNull(vm.uiState.value.group)

        trips.groups.value = listOf(aqaba)
        groups.groups.value = listOf(trip)

        val state = vm.uiState.value
        assertEquals(aqaba, state.expenseGroup)
        assertEquals(trip, state.group)
        assertEquals(setOf(you.id, sara.id), state.selected)
        assertEquals("Lunch", state.title)
    }

    @Test
    fun `deleting the selected groups on the manage screens falls back to the newest`() {
        val petra = ExpenseGroup("t2", "Petra", today)
        val family = PeopleGroup("g2", "Family", listOf(you), today)
        trips.groups.value = listOf(petra, aqaba)
        groups.groups.value = listOf(family, trip)
        val vm = viewModel()
        vm.onAction(AddExpenseAction.ExpenseGroupSelected(aqaba.id))
        vm.onAction(AddExpenseAction.GroupSelected(trip.id))

        trips.groups.value = listOf(petra)
        groups.groups.value = listOf(family)

        assertEquals(petra, vm.uiState.value.expenseGroup)
        assertEquals(family, vm.uiState.value.group)
        assertEquals(setOf(you.id), vm.uiState.value.selected)
    }

    @Test
    fun `editing the selected group's members on the manage screen updates split between`() {
        groups.groups.value = listOf(trip)
        val vm = viewModel()
        vm.onAction(AddExpenseAction.PaidByClicked)
        vm.onAction(AddExpenseAction.PayerSelected(sara.id))

        val omar = Participant("o", "Omar")
        groups.groups.value = listOf(trip.copy(members = listOf(you, omar))) // Sara removed, Omar added.

        val state = vm.uiState.value
        assertEquals(listOf(you, omar), state.participants)
        assertEquals(setOf(you.id, omar.id), state.selected)
        assertEquals(you.id, state.paidBy) // Sara can't pay any more.
    }

    @Test
    fun `save is disabled until title and amount are valid`() {
        groups.groups.value = listOf(trip)
        val vm = viewModel()
        vm.onAction(AddExpenseAction.GroupSelected(trip.id))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddExpenseAction.TitleChanged("Lunch"))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddExpenseAction.AmountChanged("12.50"))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `invalid amount input is ignored`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.AmountChanged("12.5"))
        vm.onAction(AddExpenseAction.AmountChanged("12.555"))
        vm.onAction(AddExpenseAction.AmountChanged("12a"))
        assertEquals("12.5", vm.uiState.value.amount)
    }

    @Test
    fun `unticking everyone blocks save and flags the equal hint`() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onAction(AddExpenseAction.ParticipantToggled(Participant.YOU_ID)) // Sara was already unticked.
        assertFalse(vm.uiState.value.canSave)
        assertTrue(vm.uiState.value.isSplitHintError)
    }

    @Test
    fun `percentage split needs 100 percent and keeps inputs per split type`() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onAction(AddExpenseAction.SplitTypeSelected(SplitType.Percentage))
        assertFalse(vm.uiState.value.canSave)
        assertFalse(vm.uiState.value.isSplitHintError) // Nothing typed yet.

        vm.onAction(AddExpenseAction.ShareChanged(Participant.YOU_ID, "90"))
        assertTrue(vm.uiState.value.isSplitHintError)

        vm.onAction(AddExpenseAction.ShareChanged(Participant.YOU_ID, "100"))
        assertTrue(vm.uiState.value.canSave)

        vm.onAction(AddExpenseAction.SplitTypeSelected(SplitType.Exact))
        assertEquals("", vm.uiState.value.shareInput(Participant.YOU_ID))
        vm.onAction(AddExpenseAction.SplitTypeSelected(SplitType.Percentage))
        assertEquals("100", vm.uiState.value.shareInput(Participant.YOU_ID))
    }

    @Test
    fun `exact split must match the total`() {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onAction(AddExpenseAction.SplitTypeSelected(SplitType.Exact))
        vm.onAction(AddExpenseAction.ShareChanged(Participant.YOU_ID, "12"))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddExpenseAction.ShareChanged(Participant.YOU_ID, "12.5"))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `one tapped day means start and end are the same`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.DateClicked)
        assertEquals(AddExpenseSheet.Date, vm.uiState.value.openSheet)

        vm.onAction(AddExpenseAction.DateRangeSelected(LocalDate(2026, 4, 20), end = null))

        val state = vm.uiState.value
        assertEquals(LocalDate(2026, 4, 20) to LocalDate(2026, 4, 20), state.startDate to state.endDate)
        assertNull(state.openSheet)
    }

    @Test
    fun `a second day sets the end date`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.DateRangeSelected(LocalDate(2026, 4, 20), LocalDate(2026, 4, 23)))
        assertEquals(LocalDate(2026, 4, 20) to LocalDate(2026, 4, 23), vm.uiState.value.let { it.startDate to it.endDate })
    }

    @Test
    fun `future dates are clamped to today`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.DateRangeSelected(LocalDate(2026, 4, 28), LocalDate(2026, 5, 3)))
        assertEquals(LocalDate(2026, 4, 28) to today, vm.uiState.value.let { it.startDate to it.endDate })
        vm.onAction(AddExpenseAction.DateRangeSelected(LocalDate(2026, 5, 3), null))
        assertEquals(today to today, vm.uiState.value.let { it.startDate to it.endDate })
    }

    /** A stored 30.00 expense in Aqaba / Trip, split by percentage: you 60%, Sara 40%. */
    private fun storedExpense() = Expense(
        id = "old", expenseGroupId = aqaba.id, peopleGroupId = trip.id, title = "Boat", amount = Money(30_00, usd),
        category = ExpenseCategory.Entertainment, paidBy = sara.id, splitType = SplitType.Percentage,
        shares = listOf(ExpenseShare(you.id, Money(18_00, usd)), ExpenseShare(sara.id, Money(12_00, usd))),
        startDate = LocalDate(2026, 4, 20), endDate = LocalDate(2026, 4, 21), note = "Sunset",
    )

    @Test
    fun `editing fills the form from the stored expense`() {
        groups.groups.value = listOf(trip)
        expenses.stored += storedExpense()

        val state = viewModel(SavedStateHandle(mapOf(AddExpenseViewModel.ARG_EXPENSE_ID to "old"))).uiState.value

        assertTrue(state.isEditing)
        assertEquals("Boat", state.title)
        assertEquals("30.00", state.amount)
        assertEquals(ExpenseCategory.Entertainment, state.category)
        assertEquals(aqaba.id, state.expenseGroupId)
        assertEquals(trip, state.group)
        assertEquals(sara.id, state.paidBy)
        assertEquals(SplitType.Percentage, state.splitType)
        assertEquals("60.00", state.shareInput(you.id))
        assertEquals("40.00", state.shareInput(sara.id))
        assertEquals(LocalDate(2026, 4, 20) to LocalDate(2026, 4, 21), state.startDate to state.endDate)
        assertEquals("Sunset", state.note)
        assertTrue(state.canSave)
    }

    @Test
    fun `saving an edit updates the same expense`() = runTest {
        groups.groups.value = listOf(trip)
        expenses.stored += storedExpense()
        val vm = viewModel(SavedStateHandle(mapOf(AddExpenseViewModel.ARG_EXPENSE_ID to "old")))

        vm.onAction(AddExpenseAction.TitleChanged("Boat trip"))
        vm.events.test {
            vm.onAction(AddExpenseAction.SaveClicked)
            assertEquals(AddExpenseEvent.Saved, awaitItem())
        }

        val saved = expenses.stored.single()
        assertEquals("old", saved.id)
        assertEquals("Boat trip", saved.title)
        assertEquals(listOf(18_00L, 12_00L), saved.shares.map { it.amount.minorUnits })
    }

    @Test
    fun `opening from a group's details preselects that expense group`() {
        trips.groups.value = listOf(ExpenseGroup("t2", "Newer", today), aqaba)
        val vm = viewModel(SavedStateHandle(mapOf(AddExpenseViewModel.ARG_EXPENSE_GROUP_ID to aqaba.id)))
        assertEquals(aqaba.id, vm.uiState.value.expenseGroupId)
    }

    @Test
    fun `the newest expense group is preselected`() {
        trips.groups.value = listOf(ExpenseGroup("t2", "Newer", today), aqaba)
        assertEquals("t2", viewModel().uiState.value.expenseGroupId)
    }

    @Test
    fun `an expense group is required to save`() {
        trips.groups.value = emptyList()
        val vm = viewModel()
        vm.fillValidForm()
        assertNull(vm.uiState.value.expenseGroup)
        assertEquals(listOf(ExpenseDraftError.NoExpenseGroup), vm.uiState.value.errors)
        assertFalse(vm.uiState.value.canSave)
    }

    @Test
    fun `expense group sheet searches by name and selects`() {
        val beach = ExpenseGroup("t2", "Dead Sea", today)
        trips.groups.value = listOf(beach, aqaba)
        val vm = viewModel()
        vm.onAction(AddExpenseAction.ExpenseGroupClicked)
        assertEquals(AddExpenseSheet.ExpenseGroup, vm.uiState.value.openSheet)

        vm.onAction(AddExpenseAction.ExpenseGroupQueryChanged("aqa"))
        assertEquals(listOf(aqaba), vm.uiState.value.filteredExpenseGroups)

        vm.onAction(AddExpenseAction.ExpenseGroupSelected(aqaba.id))
        assertEquals(aqaba, vm.uiState.value.expenseGroup)
        assertNull(vm.uiState.value.openSheet)
    }

    @Test
    fun `saving stores the expense and emits Saved`() = runTest {
        val vm = viewModel()
        vm.fillValidForm()
        vm.onAction(AddExpenseAction.CategorySelected(ExpenseCategory.Transport))
        vm.onAction(AddExpenseAction.NoteChanged("  Taxi  "))

        vm.events.test {
            vm.onAction(AddExpenseAction.SaveClicked)
            assertEquals(AddExpenseEvent.Saved, awaitItem())
        }
        val saved = expenses.stored.single()
        assertEquals(aqaba.id, saved.expenseGroupId)
        assertEquals(trip.id, saved.peopleGroupId)
        assertEquals(today to today, saved.startDate to saved.endDate)
        assertEquals("Lunch", saved.title)
        assertEquals(12_50L, saved.amount.minorUnits)
        assertEquals(usd, saved.amount.currency)
        assertEquals(ExpenseCategory.Transport, saved.category)
        assertEquals("Taxi", saved.note)
    }

    @Test
    fun `storage failure shows a message and allows retry`() {
        expenses.fail = true
        val vm = viewModel()
        vm.fillValidForm()
        vm.onAction(AddExpenseAction.SaveClicked)

        assertEquals(AddExpenseMessage.SaveFailed, vm.uiState.value.message)
        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.canSave)
        vm.onAction(AddExpenseAction.MessageShown)
        assertNull(vm.uiState.value.message)
    }

    @Test
    fun `title longer than the limit is ignored`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.TitleChanged("a".repeat(Expense.MAX_TITLE_LENGTH + 1)))
        assertEquals("", vm.uiState.value.title)
    }

    private class FakeProfiles(initial: UserProfile?) : UserProfileRepository {
        val profile = MutableStateFlow(initial)
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) {
            this.profile.value = profile
        }
    }

    private class FakeGroups : PeopleGroupRepository {
        override suspend fun updateGroup(group: PeopleGroup) = Unit
        override suspend fun deleteGroup(groupId: String) = Unit
        val groups = MutableStateFlow<List<PeopleGroup>>(emptyList())
        override fun observeGroups(): Flow<List<PeopleGroup>> = groups
        override suspend fun createGroup(group: PeopleGroup) {
            groups.value = listOf(group) + groups.value
        }
    }

    private class FakeExpenseGroups(initial: List<ExpenseGroup>) : ExpenseGroupRepository {
        override suspend fun updateExpenseGroup(group: ExpenseGroup) = Unit
        override suspend fun deleteExpenseGroup(groupId: String) = Unit
        val groups = MutableStateFlow(initial)
        override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups
        override suspend fun createExpenseGroup(group: ExpenseGroup) {
            groups.value = listOf(group) + groups.value
        }
    }

    private class FakeExpenses : ExpenseRepository {
        override suspend fun updateExpense(expense: Expense) {
            val index = stored.indexOfFirst { it.id == expense.id }
            if (index >= 0) stored[index] = expense
        }
        override suspend fun deleteExpense(expenseId: String) = Unit
        override suspend fun deleteExpensesIn(expenseGroupId: String) = Unit
        val stored = mutableListOf<Expense>()
        var fail = false
        override fun observeExpenses(): Flow<List<Expense>> = MutableStateFlow(stored.toList())
        override suspend fun addExpense(expense: Expense) {
            if (fail) error("disk full")
            stored += expense
        }
    }
}
