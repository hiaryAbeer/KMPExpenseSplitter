package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.OnboardingStatus
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveOnboardingStatusUseCaseTest {

    private val usd = CurrencyCode.of("USD")!!
    private val repository = FakeUserProfileRepository()
    private val useCase = ObserveOnboardingStatusUseCase(repository)

    @Test
    fun `no profile requires onboarding`() = runTest {
        useCase().test {
            assertEquals(OnboardingStatus.Required, awaitItem())
        }
    }

    @Test
    fun `stored profile completes onboarding`() = runTest {
        repository.profile.value = UserProfile(name = "Abeer", defaultCurrency = usd)

        useCase().test {
            assertEquals(OnboardingStatus.Completed, awaitItem())
        }
    }

    @Test
    fun `blank name still requires onboarding`() = runTest {
        repository.profile.value = UserProfile(name = "   ", defaultCurrency = usd)

        useCase().test {
            assertEquals(OnboardingStatus.Required, awaitItem())
        }
    }

    @Test
    fun `registering switches status and duplicates are not re-emitted`() = runTest {
        useCase().test {
            assertEquals(OnboardingStatus.Required, awaitItem())

            repository.saveProfile(UserProfile("Abeer", usd))
            assertEquals(OnboardingStatus.Completed, awaitItem())

            // A profile edit that keeps the user registered must not re-trigger navigation.
            repository.saveProfile(UserProfile("Abeer H.", usd))
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
