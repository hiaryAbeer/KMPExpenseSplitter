package com.abeerapps.kmpexpensesplitter.feature.settings

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.config.AppInfo
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.AppPreferencesRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseReportExporter
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.PeopleGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Participant
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import kotlinx.datetime.LocalDate
import com.abeerapps.kmpexpensesplitter.core.domain.repository.LocalDataCleaner
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ClearLocalDataUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExportExpenseReportUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SearchCurrenciesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SetThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val eur = CurrencyCode.of("EUR")!!

    private val profiles = object : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(UserProfile("Ahmad", jod))
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) { this.profile.value = profile }
    }
    private val groups = object : ExpenseGroupRepository {
        val groups = MutableStateFlow<List<ExpenseGroup>>(emptyList())
        override fun observeExpenseGroups(): Flow<List<ExpenseGroup>> = groups
        override suspend fun createExpenseGroup(group: ExpenseGroup) { groups.value = listOf(group) + groups.value }
    }
    private val people = object : PeopleGroupRepository {
        override fun observeGroups(): Flow<List<PeopleGroup>> = MutableStateFlow(emptyList())
        override suspend fun createGroup(group: PeopleGroup) = Unit
    }
    private val expenses = object : ExpenseRepository {
        override fun observeExpenses(): Flow<List<Expense>> = MutableStateFlow(emptyList())
        override suspend fun addExpense(expense: Expense) = Unit
    }
    private val observeGroups get() = ObserveGroupsUseCase(groups, people, expenses, profiles)
    private val prefs = object : AppPreferencesRepository {
        val theme = MutableStateFlow(ThemePreference.System)
        override fun observeTheme(): Flow<ThemePreference> = theme
        override suspend fun setTheme(theme: ThemePreference) { this.theme.value = theme }
    }
    private var clearCalls = 0
    private var clearSucceeds = true
    private val cleaner = object : LocalDataCleaner {
        override suspend fun clearAll() {
            clearCalls++
            if (!clearSucceeds) error("io")
        }
    }
    private val exporter = object : ExpenseReportExporter {
        override suspend fun export(groups: List<GroupSummary>) = ExportResult.Saved("report.pdf", "Downloads")
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(
        observeUserProfile = ObserveUserProfileUseCase(profiles),
        observeGroups = observeGroups,
        observeTheme = ObserveThemePreferenceUseCase(prefs),
        updateProfile = UpdateUserProfileUseCase(profiles),
        setTheme = SetThemePreferenceUseCase(prefs),
        searchCurrencies = SearchCurrenciesUseCase(),
        exportReport = ExportExpenseReportUseCase(observeGroups, exporter),
        clearLocalData = ClearLocalDataUseCase(cleaner),
        appInfo = object : AppInfo { override val versionName = "1.0.0" },
    )

    private suspend fun ReceiveTurbine<SettingsUiState>.awaitLoaded(): SettingsUiState {
        var s = awaitItem()
        while (s.isLoading) s = awaitItem()
        return s
    }

    @Test
    fun `shows profile currency theme and version`() = runTest {
        viewModel().uiState.test {
            val s = awaitLoaded()
            assertEquals("Ahmad", s.name)
            assertEquals(jod, s.currency.code)
            assertEquals(ThemePreference.System, s.theme)
            assertEquals("1.0.0", s.versionName)
            assertNull(s.openSheet)
        }
    }

    @Test
    fun `export is disabled without data and its sheet cannot open`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            assertFalse(awaitLoaded().canExport)
            vm.onAction(SettingsAction.ExportClicked)
            expectNoEvents()
        }
    }

    @Test
    fun `export is enabled when groups exist and saves the report`() = runTest {
        groups.groups.value = listOf(ExpenseGroup("1", "Trip", LocalDate(2026, 4, 1)))
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(awaitLoaded().canExport)
            vm.onAction(SettingsAction.ExportClicked)
            assertEquals(SettingsSheet.Export, awaitItem().openSheet)
            vm.onAction(SettingsAction.ConfirmExportClicked)
            val done = expectMostRecentItem()
            assertNull(done.openSheet)
            assertEquals(SettingsMessage.ExportSaved("report.pdf", "Downloads"), done.message)
        }
    }

    @Test
    fun `edit name prefills validates and saves`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.ProfileClicked)
            assertEquals(SettingsSheet.EditName, awaitItem().openSheet)
            assertEquals("Ahmad", vm.nameInput.value)
            assertFalse(canSaveName(vm.nameInput.value, "Ahmad")) // Unchanged name can't be saved.

            vm.onAction(SettingsAction.NameChanged("Sara"))
            vm.onAction(SettingsAction.SaveNameClicked)
            val s = expectMostRecentItem()
            assertEquals("Sara", s.name)
            assertNull(s.openSheet)
        }
        assertEquals(UserProfile("Sara", jod), profiles.profile.value)
    }

    @Test
    fun `blank name is not saved`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.ProfileClicked)
            vm.onAction(SettingsAction.NameChanged("   "))
            vm.onAction(SettingsAction.SaveNameClicked)
            cancelAndIgnoreRemainingEvents()
        }
        assertEquals("Ahmad", profiles.profile.value?.name)
    }

    @Test
    fun `currency sheet searches and selecting saves and closes`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.CurrencyClicked)
            vm.onAction(SettingsAction.CurrencyQueryChanged("euro"))
            assertEquals("EUR", expectMostRecentItem().currencyResults.first().code.value)

            vm.onAction(SettingsAction.CurrencySelected(eur))
            val s = expectMostRecentItem()
            assertEquals(eur, s.currency.code)
            assertNull(s.openSheet)
        }
    }

    @Test
    fun `theme selection is saved and closes the sheet`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.ThemeClicked)
            vm.onAction(SettingsAction.ThemeSelected(ThemePreference.Dark))
            val s = expectMostRecentItem()
            assertEquals(ThemePreference.Dark, s.theme)
            assertNull(s.openSheet)
        }
    }

    @Test
    fun `confirming clear wipes data and emits DataCleared`() = runTest {
        val vm = viewModel()
        vm.events.test {
            vm.onAction(SettingsAction.ClearDataClicked)
            vm.onAction(SettingsAction.ConfirmClearClicked)
            assertEquals(SettingsEvent.DataCleared, awaitItem())
        }
        assertEquals(1, clearCalls)
    }

    @Test
    fun `failed clear shows a message and keeps the user in settings`() = runTest {
        clearSucceeds = false
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.ClearDataClicked)
            vm.onAction(SettingsAction.ConfirmClearClicked)
            assertEquals(SettingsMessage.ClearFailed, expectMostRecentItem().message)
        }
    }

    @Test
    fun `dismiss closes any sheet`() = runTest {
        val vm = viewModel()
        vm.uiState.test {
            awaitLoaded()
            vm.onAction(SettingsAction.AboutClicked)
            assertEquals(SettingsSheet.About, awaitItem().openSheet)
            vm.onAction(SettingsAction.SheetDismissed)
            assertNull(awaitItem().openSheet)
        }
    }
}
