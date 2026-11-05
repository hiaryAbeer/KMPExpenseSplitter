package com.abeerapps.kmpexpensesplitter.feature.onboarding

import androidx.compose.runtime.Immutable

/**
 * Unidirectional data flow contract for the onboarding screen:
 * UI renders [OnboardingUiState], sends [OnboardingAction]s, and reacts to
 * one-shot [OnboardingEvent]s (navigation).
 */
@Immutable
data class OnboardingUiState(
    /** Controlled by `FeatureFlags.isOnboardingSkipEnabled`; hidden for now. */
    val isSkipVisible: Boolean = false,
)

sealed interface OnboardingAction {
    data object RegisterClicked : OnboardingAction
    data object SkipClicked : OnboardingAction
}

sealed interface OnboardingEvent {
    data object NavigateToRegistration : OnboardingEvent
    data object NavigateToHome : OnboardingEvent
}
