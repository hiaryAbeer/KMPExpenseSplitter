package com.abeerapps.kmpexpensesplitter.feature.expense

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
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
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreateExpenseGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.CreatePeopleGroupUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveExpenseGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExpenseDraftError
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.MemberNameError
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
    private var nextId = 0

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
        createExpenseGroup = CreateExpenseGroupUseCase(trips, dateProvider, newId = { "t${nextId++}" }),
        observeGroupList = ObservePeopleGroupsUseCase(groups, profiles),
        observeUserProfile = ObserveUserProfileUseCase(profiles),
        addExpense = AddExpenseUseCase(expenses, newId = { "e1" }),
        createGroup = CreatePeopleGroupUseCase(groups, profiles, dateProvider, newId = { "n${nextId++}" }),
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
    fun `adding a group creates it with you and selects it`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.GroupClicked)
        vm.onAction(AddExpenseAction.AddGroupClicked)
        assertEquals(AddExpenseSheet.CreateGroup, vm.uiState.value.openSheet)
        assertFalse(vm.uiState.value.newGroup.canSave)

        vm.onAction(AddExpenseAction.NewGroupNameChanged("Aqaba"))
        vm.onAction(AddExpenseAction.MemberInputChanged("Sara"))
        vm.onAction(AddExpenseAction.AddMemberClicked)
        vm.onAction(AddExpenseAction.MemberInputChanged("sara"))
        vm.onAction(AddExpenseAction.AddMemberClicked)
        assertEquals(MemberNameError.Duplicate, vm.uiState.value.newGroup.memberError)
        // A valid name still in the input is included on "Add".
        vm.onAction(AddExpenseAction.MemberInputChanged("Omar"))
        assertTrue(vm.uiState.value.newGroup.canSave)

        vm.onAction(AddExpenseAction.CreateGroupClicked)

        val state = vm.uiState.value
        assertNull(state.openSheet)
        assertEquals("Aqaba", state.group?.name)
        assertEquals(listOf("Ahmad", "Sara", "Omar"), state.participants.map { it.name })
        assertEquals(state.participants.map { it.id }.toSet(), state.selected)
        assertEquals(listOf("Aqaba"), groups.groups.value.map { it.name })
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
    fun `add needs a group name and at least one member`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.AddGroupClicked)
        assertFalse(vm.uiState.value.newGroup.canSave) // You're in, but no name yet.

        vm.onAction(AddExpenseAction.NewGroupNameChanged("Solo"))
        assertTrue(vm.uiState.value.newGroup.canSave) // You alone are enough.

        vm.onAction(AddExpenseAction.YouRemoved)
        assertFalse(vm.uiState.value.newGroup.includeYou)
        assertFalse(vm.uiState.value.newGroup.canSave) // Nobody left.

        vm.onAction(AddExpenseAction.MemberInputChanged("Sara"))
        vm.onAction(AddExpenseAction.AddMemberClicked)
        assertTrue(vm.uiState.value.newGroup.canSave)
        vm.onAction(AddExpenseAction.MemberRemoved(0))
        assertFalse(vm.uiState.value.newGroup.canSave)
    }

    @Test
    fun `a group created without you makes its first member the payer`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.AddGroupClicked)
        vm.onAction(AddExpenseAction.NewGroupNameChanged("Parents"))
        vm.onAction(AddExpenseAction.YouRemoved)
        vm.onAction(AddExpenseAction.MemberInputChanged("Mom"))
        vm.onAction(AddExpenseAction.AddMemberClicked)
        vm.onAction(AddExpenseAction.MemberInputChanged("Dad"))
        vm.onAction(AddExpenseAction.CreateGroupClicked)

        val state = vm.uiState.value
        assertEquals(listOf("Mom", "Dad"), state.participants.map { it.name })
        assertEquals(state.participants.first().id, state.paidBy)
    }

    @Test
    fun `typing your own name after removing yourself adds you back`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.AddGroupClicked)
        vm.onAction(AddExpenseAction.YouRemoved)
        vm.onAction(AddExpenseAction.MemberInputChanged(" ahmad "))
        vm.onAction(AddExpenseAction.AddMemberClicked)

        val form = vm.uiState.value.newGroup
        assertTrue(form.includeYou)
        assertTrue(form.members.isEmpty())
        assertEquals("", form.memberInput)
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
    fun `adding an expense group starts from the search text then selects it`() {
        val vm = viewModel()
        vm.onAction(AddExpenseAction.ExpenseGroupClicked)
        vm.onAction(AddExpenseAction.ExpenseGroupQueryChanged("  Petra  "))
        vm.onAction(AddExpenseAction.AddExpenseGroupClicked)

        assertEquals(AddExpenseSheet.CreateExpenseGroup, vm.uiState.value.openSheet)
        assertEquals("Petra", vm.uiState.value.newExpenseGroup.name)
        assertTrue(vm.uiState.value.newExpenseGroup.canCreate)

        vm.onAction(AddExpenseAction.NewExpenseGroupNameChanged(" "))
        assertFalse(vm.uiState.value.newExpenseGroup.canCreate)
        vm.onAction(AddExpenseAction.NewExpenseGroupNameChanged("Petra Trip"))
        vm.onAction(AddExpenseAction.CreateExpenseGroupClicked)

        val state = vm.uiState.value
        assertNull(state.openSheet)
        assertEquals("Petra Trip", state.expenseGroup?.name)
        assertEquals(listOf("Petra Trip", "Aqaba Trip"), trips.groups.value.map { it.name })
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
