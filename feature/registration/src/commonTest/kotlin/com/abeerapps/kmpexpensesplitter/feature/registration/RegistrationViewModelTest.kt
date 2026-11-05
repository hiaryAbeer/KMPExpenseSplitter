package com.abeerapps.kmpexpensesplitter.feature.registration

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RegisterUserUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SearchCurrenciesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class RegistrationViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeRepository()
    private val eur = CurrencyCode.of("EUR")!!

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(handle: SavedStateHandle = SavedStateHandle()) =
        RegistrationViewModel(RegisterUserUseCase(repository), SearchCurrenciesUseCase(), handle)

    @Test
    fun `initial state has empty name with JOD preselected and save disabled`() {
        val state = viewModel().uiState.value

        assertEquals("", state.name)
        assertEquals("JOD", state.selectedCurrency.code.value)
        assertEquals(SupportedCurrencies.all.size, state.currencyResults.size)
        assertFalse(state.isCurrencySheetVisible)
        assertFalse(state.canSave)
    }

    @Test
    fun `typing a name enables save but blank does not`() {
        val vm = viewModel()

        vm.onAction(RegistrationAction.NameChanged("   "))
        assertFalse(vm.uiState.value.canSave)

        vm.onAction(RegistrationAction.NameChanged("Alex"))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `name input is capped at max length`() {
        val vm = viewModel()
        vm.onAction(RegistrationAction.NameChanged("a".repeat(UserProfile.MAX_NAME_LENGTH + 10)))
        assertEquals(UserProfile.MAX_NAME_LENGTH, vm.uiState.value.name.length)
    }

    @Test
    fun `clicking the field opens the sheet and dismiss closes it`() {
        val vm = viewModel()

        vm.onAction(RegistrationAction.CurrencyFieldClicked)
        assertTrue(vm.uiState.value.isCurrencySheetVisible)

        vm.onAction(RegistrationAction.CurrencySheetDismissed)
        assertFalse(vm.uiState.value.isCurrencySheetVisible)
    }

    @Test
    fun `typing a query filters the currencies`() {
        val vm = viewModel()
        vm.onAction(RegistrationAction.CurrencyFieldClicked)

        vm.onAction(RegistrationAction.CurrencyQueryChanged("jordan"))

        val state = vm.uiState.value
        assertEquals("jordan", state.currencyQuery)
        assertEquals("JOD", state.currencyResults.first().code.value)
        assertTrue(state.currencyResults.size < SupportedCurrencies.all.size)
    }

    @Test
    fun `reopening the sheet clears the previous search`() {
        val vm = viewModel()
        vm.onAction(RegistrationAction.CurrencyFieldClicked)
        vm.onAction(RegistrationAction.CurrencyQueryChanged("euro"))
        vm.onAction(RegistrationAction.CurrencySheetDismissed)

        vm.onAction(RegistrationAction.CurrencyFieldClicked)

        assertEquals("", vm.uiState.value.currencyQuery)
        assertEquals(SupportedCurrencies.all.size, vm.uiState.value.currencyResults.size)
    }

    @Test
    fun `selecting a currency updates selection and closes the sheet`() {
        val vm = viewModel()
        vm.onAction(RegistrationAction.CurrencyFieldClicked)

        vm.onAction(RegistrationAction.CurrencySelected(eur))

        assertEquals(eur, vm.uiState.value.selectedCurrency.code)
        assertFalse(vm.uiState.value.isCurrencySheetVisible)
    }

    @Test
    fun `save persists profile and emits Registered`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.onAction(RegistrationAction.NameChanged(" Alex "))
        vm.onAction(RegistrationAction.CurrencySelected(eur))

        vm.events.test {
            vm.onAction(RegistrationAction.SaveClicked)
            assertTrue(vm.uiState.value.isSaving)

            assertEquals(RegistrationEvent.Registered, awaitItem())
        }
        advanceUntilIdle()
        assertEquals(UserProfile("Alex", eur), repository.saved)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test
    fun `save is ignored when name is invalid`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.events.test {
            vm.onAction(RegistrationAction.SaveClicked)
            advanceUntilIdle()
            expectNoEvents()
        }
        assertNull(repository.saved)
    }

    @Test
    fun `storage failure shows error and can be cleared`() = runTest(dispatcher) {
        repository.failure = IllegalStateException("disk full")
        val vm = viewModel()
        vm.onAction(RegistrationAction.NameChanged("Alex"))

        vm.onAction(RegistrationAction.SaveClicked)
        advanceUntilIdle()

        assertEquals(RegistrationError.SaveFailed, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSaving)
        assertTrue(vm.uiState.value.canSave) // User can retry.

        vm.onAction(RegistrationAction.ErrorShown)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `form input is restored from saved state`() {
        val handle = SavedStateHandle()
        viewModel(handle).apply {
            onAction(RegistrationAction.NameChanged("Alex"))
            onAction(RegistrationAction.CurrencySelected(eur))
        }

        val restored = viewModel(handle).uiState.value // e.g. after process death

        assertEquals("Alex", restored.name)
        assertEquals(eur, restored.selectedCurrency.code)
    }

    private class FakeRepository : UserProfileRepository {
        var saved: UserProfile? = null
        var failure: Exception? = null
        override fun observeProfile(): Flow<UserProfile?> = flowOf(saved)
        override suspend fun saveProfile(profile: UserProfile) {
            failure?.let { throw it }
            saved = profile
        }
    }
}
