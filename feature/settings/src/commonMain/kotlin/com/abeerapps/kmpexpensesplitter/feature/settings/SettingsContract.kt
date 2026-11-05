package com.abeerapps.kmpexpensesplitter.feature.settings

import androidx.compose.runtime.Immutable
import com.abeerapps.kmpexpensesplitter.core.domain.model.Currency
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile

/** Which bottom sheet is open (at most one at a time). */
enum class SettingsSheet { EditName, Currency, Theme, Export, ClearData, About }

/** Everything the Settings screen renders. Text-field values live in their own flows. */
@Immutable
data class SettingsUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val currency: Currency = SupportedCurrencies.default,
    val theme: ThemePreference = ThemePreference.System,
    /** Export needs data: only true when at least one group exists. */
    val canExport: Boolean = false,
    val versionName: String = "",
    val openSheet: SettingsSheet? = null,
    val currencyResults: List<Currency> = SupportedCurrencies.all,
    /** A save / clear / export is running; buttons show a spinner. */
    val isWorking: Boolean = false,
    val message: SettingsMessage? = null,
)

/** One-off feedback shown in a snackbar. */
sealed interface SettingsMessage {
    data object SaveFailed : SettingsMessage
    data object ClearFailed : SettingsMessage
    data object ExportFailed : SettingsMessage
    data class ExportSaved(val fileName: String, val location: String) : SettingsMessage
}

/** Whether [input] can be saved as the new name for a user currently called [currentName]. */
internal fun canSaveName(input: String, currentName: String): Boolean =
    UserProfile.isValidName(input) && input.trim() != currentName

sealed interface SettingsAction {
    // Rows
    data object ProfileClicked : SettingsAction
    data object CurrencyClicked : SettingsAction
    data object ThemeClicked : SettingsAction
    data object ExportClicked : SettingsAction
    data object ClearDataClicked : SettingsAction
    data object AboutClicked : SettingsAction
    data object SheetDismissed : SettingsAction

    // Sheets
    data class NameChanged(val name: String) : SettingsAction
    data object SaveNameClicked : SettingsAction
    data class CurrencyQueryChanged(val query: String) : SettingsAction
    data class CurrencySelected(val code: CurrencyCode) : SettingsAction
    data class ThemeSelected(val theme: ThemePreference) : SettingsAction
    data object ConfirmExportClicked : SettingsAction
    data object ConfirmClearClicked : SettingsAction

    data object MessageShown : SettingsAction
}

sealed interface SettingsEvent {
    /** All local data was deleted: restart the app flow at onboarding. */
    data object DataCleared : SettingsEvent
}
