package com.abeerapps.kmpexpensesplitter

import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveOnboardingStatusUseCase
import com.abeerapps.kmpexpensesplitter.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repository = FakeUserProfileRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = AppViewModel(ObserveOnboardingStatusUseCase(repository))

    @Test
    fun `no registered user starts on onboarding`() = runTest(dispatcher) {
        viewModel().startState.test {
            assertEquals(AppStartState.Loading, awaitItem())
            assertEquals(AppStartState.Ready(AppDestination.Onboarding), awaitItem())
        }
    }

    @Test
    fun `registered user skips onboarding and starts on home`() = runTest(dispatcher) {
        repository.profile.value = UserProfile("Abeer", CurrencyCode.of("JOD")!!)

        viewModel().startState.test {
            assertEquals(AppStartState.Loading, awaitItem())
            assertEquals(AppStartState.Ready(AppDestination.Home), awaitItem())
        }
    }

    @Test
    fun `start destination is not recomputed after it is resolved`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.startState.test {
            skipItems(1) // Loading
            assertEquals(AppStartState.Ready(AppDestination.Onboarding), awaitItem())

            repository.profile.value = UserProfile("Abeer", CurrencyCode.of("JOD")!!)
            expectNoEvents()
        }
    }

    private class FakeUserProfileRepository : UserProfileRepository {
        val profile = MutableStateFlow<UserProfile?>(null)
        override fun observeProfile(): Flow<UserProfile?> = profile
        override suspend fun saveProfile(profile: UserProfile) {
            this.profile.value = profile
        }
    }
}
