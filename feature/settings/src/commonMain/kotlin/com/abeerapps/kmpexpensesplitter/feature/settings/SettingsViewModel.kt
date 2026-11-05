package com.abeerapps.kmpexpensesplitter.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abeerapps.kmpexpensesplitter.core.domain.config.AppInfo
import com.abeerapps.kmpexpensesplitter.core.domain.model.Currency
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExportResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ClearLocalDataUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ExportExpenseReportUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveGroupsUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ObserveUserProfileUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.ProfileUpdateResult
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SearchCurrenciesUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.SetThemePreferenceUseCase
import com.abeerapps.kmpexpensesplitter.core.domain.usecase.UpdateUserProfileUseCase
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Presentation logic for Settings. Shared by Android and iOS.
 *
 * Stored data (profile, theme, whether groups exist) flows in from use cases;
 * transient UI (open sheet, search, progress, messages) lives in [local].
 * Text-field values are separate flows ([nameInput], [currencyQuery]) so typing
 * never waits on the combined state.
 */
class SettingsViewModel(
    observeUserProfile: ObserveUserProfileUseCase,
    observeGroups: ObserveGroupsUseCase,
    observeTheme: ObserveThemePreferenceUseCase,
    private val updateProfile: UpdateUserProfileUseCase,
    private val setTheme: SetThemePreferenceUseCase,
    private val searchCurrencies: SearchCurrenciesUseCase,
    private val exportReport: ExportExpenseReportUseCase,
    private val clearLocalData: ClearLocalDataUseCase,
    appInfo: AppInfo,
) : ViewModel() {

    private data class Local(
        val openSheet: SettingsSheet? = null,
        val currencyResults: List<Currency> = SupportedCurrencies.all,
        val isWorking: Boolean = false,
        val message: SettingsMessage? = null,
    )

    private val local = MutableStateFlow(Local())
    private var currentName = ""

    private val _nameInput = MutableStateFlow("")
    val nameInput: StateFlow<String> = _nameInput.asStateFlow()

    private val _currencyQuery = MutableStateFlow("")
    val currencyQuery: StateFlow<String> = _currencyQuery.asStateFlow()

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        observeUserProfile(),
        observeGroups(),
        observeTheme(),
        local,
    ) { profile, groups, theme, local ->
        currentName = profile?.name.orEmpty()
        SettingsUiState(
            isLoading = false,
            name = currentName,
            currency = profile?.defaultCurrency?.let(SupportedCurrencies::find) ?: SupportedCurrencies.default,
            theme = theme,
            canExport = groups.isNotEmpty(),
            versionName = appInfo.versionName,
            openSheet = local.openSheet,
            currencyResults = local.currencyResults,
            isWorking = local.isWorking,
            message = local.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun onAction(action: SettingsAction) {
        when (action) {
            SettingsAction.ProfileClicked -> {
                _nameInput.value = currentName
                open(SettingsSheet.EditName)
            }
            SettingsAction.CurrencyClicked -> {
                _currencyQuery.value = ""
                local.update { it.copy(currencyResults = searchCurrencies("")) }
                open(SettingsSheet.Currency)
            }
            SettingsAction.ThemeClicked -> open(SettingsSheet.Theme)
            // Disabled rows can't be tapped, but never trust the UI alone.
            SettingsAction.ExportClicked -> if (uiState.value.canExport) open(SettingsSheet.Export)
            SettingsAction.ClearDataClicked -> open(SettingsSheet.ClearData)
            SettingsAction.AboutClicked -> open(SettingsSheet.About)
            SettingsAction.SheetDismissed -> local.update { it.copy(openSheet = null) }

            is SettingsAction.NameChanged -> _nameInput.value = action.name.take(UserProfile.MAX_NAME_LENGTH)
            SettingsAction.SaveNameClicked -> saveName()
            is SettingsAction.CurrencyQueryChanged -> {
                _currencyQuery.value = action.query
                local.update { it.copy(currencyResults = searchCurrencies(action.query)) }
            }
            is SettingsAction.CurrencySelected -> saveCurrency(action.code)
            is SettingsAction.ThemeSelected -> {
                local.update { it.copy(openSheet = null) }
                viewModelScope.launch { setTheme(action.theme) }
            }
            SettingsAction.ConfirmExportClicked -> export()
            SettingsAction.ConfirmClearClicked -> clearData()
            SettingsAction.MessageShown -> local.update { it.copy(message = null) }
        }
    }

    private fun open(sheet: SettingsSheet) = local.update { it.copy(openSheet = sheet) }

    private fun saveName() {
        if (!canSaveName(_nameInput.value, currentName) || local.value.isWorking) return
        runWork {
            when (updateProfile.updateName(_nameInput.value)) {
                ProfileUpdateResult.Success -> local.update { it.copy(openSheet = null) }
                else -> local.update { it.copy(message = SettingsMessage.SaveFailed) }
            }
        }
    }

    private fun saveCurrency(code: CurrencyCode) {
        local.update { it.copy(openSheet = null) }
        runWork {
            if (updateProfile.updateDefaultCurrency(code) != ProfileUpdateResult.Success) {
                local.update { it.copy(message = SettingsMessage.SaveFailed) }
            }
        }
    }

    private fun export() {
        if (local.value.isWorking) return
        runWork {
            val message = when (val result = exportReport()) {
                is ExportResult.Saved -> SettingsMessage.ExportSaved(result.fileName, result.location)
                ExportResult.Failed -> SettingsMessage.ExportFailed
            }
            local.update { it.copy(openSheet = null, message = message) }
        }
    }

    private fun clearData() {
        if (local.value.isWorking) return
        runWork {
            if (clearLocalData()) {
                local.update { it.copy(openSheet = null) }
                _events.send(SettingsEvent.DataCleared)
            } else {
                local.update { it.copy(message = SettingsMessage.ClearFailed) }
            }
        }
    }

    /** Runs [block] with [SettingsUiState.isWorking] set, so buttons show progress. */
    private fun runWork(block: suspend () -> Unit) {
        local.update { it.copy(isWorking = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                local.update { it.copy(isWorking = false) }
            }
        }
    }
}
