package com.abeerapps.kmpexpensesplitter.feature.registration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RegisterResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.RegisterUserUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SearchCurrenciesUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for "Set up your profile". Shared by Android and iOS.
 *
 * Form input is mirrored into [SavedStateHandle], so a half-filled form
 * survives process death on Android. The currency search query is transient.
 */
class RegistrationViewModel(
    private val registerUser: RegisterUserUseCase,
    private val searchCurrencies: SearchCurrenciesUseCase,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _uiState = MutableStateFlow(restoreState())
    val uiState: StateFlow<RegistrationUiState> = _uiState.asStateFlow()

    private val _events = Channel<RegistrationEvent>(Channel.BUFFERED)
    val events: Flow<RegistrationEvent> = _events.receiveAsFlow()

    fun onAction(action: RegistrationAction) {
        when (action) {
            is RegistrationAction.NameChanged -> onNameChanged(action.name)
            // Every opening starts with an empty search showing the full list.
            RegistrationAction.CurrencyFieldClicked -> _uiState.update {
                it.copy(isCurrencySheetVisible = true, currencyQuery = "", currencyResults = searchCurrencies(""))
            }
            RegistrationAction.CurrencySheetDismissed ->
                _uiState.update { it.copy(isCurrencySheetVisible = false) }
            is RegistrationAction.CurrencyQueryChanged -> _uiState.update {
                // ~150 items: searching synchronously is well under a frame.
                it.copy(currencyQuery = action.query, currencyResults = searchCurrencies(action.query))
            }
            is RegistrationAction.CurrencySelected -> onCurrencySelected(action.code)
            RegistrationAction.SaveClicked -> save()
            RegistrationAction.ErrorShown -> _uiState.update { it.copy(error = null) }
        }
    }

    private fun onNameChanged(name: String) {
        // Cap input instead of erroring: the user simply can't type past the limit.
        val capped = name.take(UserProfile.MAX_NAME_LENGTH)
        savedStateHandle[KEY_NAME] = capped
        _uiState.update { it.copy(name = capped) }
    }

    private fun onCurrencySelected(code: CurrencyCode) {
        val currency = SupportedCurrencies.find(code) ?: return
        savedStateHandle[KEY_CURRENCY] = code.value
        _uiState.update { it.copy(selectedCurrency = currency, isCurrencySheetVisible = false) }
    }

    private fun save() {
        val state = _uiState.value
        if (!state.canSave) return // Also guards against double taps while saving.

        _uiState.update { it.copy(isSaving = true, isCurrencySheetVisible = false, error = null) }
        viewModelScope.launch {
            when (registerUser(state.name, state.selectedCurrency.code)) {
                RegisterResult.Success -> _events.send(RegistrationEvent.Registered)
                // InvalidName can't happen (canSave checked it), but stay safe.
                RegisterResult.InvalidName,
                RegisterResult.StorageError,
                -> _uiState.update { it.copy(error = RegistrationError.SaveFailed) }
            }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun restoreState(): RegistrationUiState {
        val name = savedStateHandle.get<String>(KEY_NAME).orEmpty()
        val currency = savedStateHandle.get<String>(KEY_CURRENCY)
            ?.let(CurrencyCode::of)
            ?.let(SupportedCurrencies::find)
            ?: SupportedCurrencies.default
        return RegistrationUiState(name = name, selectedCurrency = currency)
    }

    private companion object {
        const val KEY_NAME = "registration_name"
        const val KEY_CURRENCY = "registration_currency"
    }
}
