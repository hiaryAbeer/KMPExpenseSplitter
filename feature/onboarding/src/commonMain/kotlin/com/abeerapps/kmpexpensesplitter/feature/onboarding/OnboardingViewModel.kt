package com.abeerapps.kmpexpensesplitter.feature.onboarding

import androidx.lifecycle.ViewModel
import com.abeerapps.kmpexpensesplitter.core.domain.config.FeatureFlags
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * Presentation logic for onboarding. Shared by Android and iOS.
 *
 * Navigation is emitted as events through a [Channel] (delivered exactly once,
 * buffered while the UI is not collecting) rather than as state, so it is not
 * replayed after rotation or when returning to the screen.
 */
class OnboardingViewModel(
    featureFlags: FeatureFlags,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        OnboardingUiState(isSkipVisible = featureFlags.isOnboardingSkipEnabled),
    )
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    private val _events = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = _events.receiveAsFlow()

    fun onAction(action: OnboardingAction) {
        when (action) {
            OnboardingAction.RegisterClicked ->
                _events.trySend(OnboardingEvent.NavigateToRegistration)

            OnboardingAction.SkipClicked ->
                // Defensive: ignore if the flag is off, even if the UI somehow sent it.
                if (_uiState.value.isSkipVisible) {
                    _events.trySend(OnboardingEvent.NavigateToHome)
                }
        }
    }
}
