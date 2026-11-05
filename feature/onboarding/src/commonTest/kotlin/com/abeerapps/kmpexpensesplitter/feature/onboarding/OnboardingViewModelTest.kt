package com.abeerapps.kmpexpensesplitter.feature.onboarding

import app.cash.turbine.test
import com.abeerapps.kmpexpensesplitter.core.domain.config.FeatureFlags
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnboardingViewModelTest {

    private fun viewModel(skipEnabled: Boolean) = OnboardingViewModel(
        featureFlags = object : FeatureFlags {
            override val isOnboardingSkipEnabled = skipEnabled
        },
    )

    @Test
    fun `skip is hidden when flag is off`() {
        assertFalse(viewModel(skipEnabled = false).uiState.value.isSkipVisible)
    }

    @Test
    fun `skip is visible when flag is on`() {
        assertTrue(viewModel(skipEnabled = true).uiState.value.isSkipVisible)
    }

    @Test
    fun `register emits navigate to registration`() = runTest {
        val vm = viewModel(skipEnabled = false)

        vm.events.test {
            vm.onAction(OnboardingAction.RegisterClicked)
            assertEquals(OnboardingEvent.NavigateToRegistration, awaitItem())
        }
    }

    @Test
    fun `skip emits navigate to home when enabled`() = runTest {
        val vm = viewModel(skipEnabled = true)

        vm.events.test {
            vm.onAction(OnboardingAction.SkipClicked)
            assertEquals(OnboardingEvent.NavigateToHome, awaitItem())
        }
    }

    @Test
    fun `skip is ignored when disabled`() = runTest {
        val vm = viewModel(skipEnabled = false)

        vm.events.test {
            vm.onAction(OnboardingAction.SkipClicked)
            expectNoEvents()
        }
    }

    @Test
    fun `event sent before collection is delivered once collected`() = runTest {
        val vm = viewModel(skipEnabled = false)
        vm.onAction(OnboardingAction.RegisterClicked) // e.g. during a config change

        vm.events.test {
            assertEquals(OnboardingEvent.NavigateToRegistration, awaitItem())
            expectNoEvents()
        }
    }
}
