package com.abeerapps.kmpexpensesplitter.feature.registration

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.Currency
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile

/** Everything the "Set up your profile" screen renders. */
@Immutable
data class RegistrationUiState(
    val name: String = "",
    val selectedCurrency: Currency = SupportedCurrencies.default,
    val isCurrencySheetVisible: Boolean = false,
    val currencyQuery: String = "",
    val currencyResults: List<Currency> = SupportedCurrencies.all,
    val isSaving: Boolean = false,
    val error: RegistrationError? = null,
) {
    /** Save is possible once the name is valid and no save is in flight. */
    val canSave: Boolean get() = UserProfile.isValidName(name) && !isSaving
}

enum class RegistrationError { SaveFailed }

sealed interface RegistrationAction {
    data class NameChanged(val name: String) : RegistrationAction

    /** Opens the currency bottom sheet. */
    data object CurrencyFieldClicked : RegistrationAction
    data object CurrencySheetDismissed : RegistrationAction
    data class CurrencyQueryChanged(val query: String) : RegistrationAction
    data class CurrencySelected(val code: CurrencyCode) : RegistrationAction

    data object SaveClicked : RegistrationAction

    /** The UI has displayed [RegistrationUiState.error]; clear it. */
    data object ErrorShown : RegistrationAction
}

sealed interface RegistrationEvent {
    /** Profile saved; leave registration (and onboarding) for good. */
    data object Registered : RegistrationEvent
}
