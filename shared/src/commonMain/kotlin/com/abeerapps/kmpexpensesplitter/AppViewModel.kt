package com.abeerapps.kmpexpensesplitter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.OnboardingStatus
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveOnboardingStatusUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.navigation.AppDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

/** What the app shell renders before/after the start destination is known. */
sealed interface AppStartState {
    data object Loading : AppStartState
    data class Ready(val startDestination: AppDestination) : AppStartState
}

/**
 * Decides the start destination once per process:
 * no registered profile → [AppDestination.Onboarding], otherwise → [AppDestination.Home].
 *
 * Only the first status is used. Later changes (e.g. registration completing)
 * are handled by explicit navigation, so the NavHost graph is never rebuilt.
 */
class AppViewModel(
    observeOnboardingStatus: ObserveOnboardingStatusUseCase,
    observeThemePreference: ObserveThemePreferenceUseCase,
) : ViewModel() {

    /** Theme chosen in Settings; applied to the whole app. */
    val themePreference: StateFlow<ThemePreference> = observeThemePreference()
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemePreference.System)

    val startState: StateFlow<AppStartState> = flow {
        val destination = when (observeOnboardingStatus().first()) {
            OnboardingStatus.Required -> AppDestination.Onboarding
            OnboardingStatus.Completed -> AppDestination.Home
        }
        emit(AppStartState.Ready(destination))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, AppStartState.Loading)
}
