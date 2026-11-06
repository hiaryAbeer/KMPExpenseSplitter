package com.abeerapps.kmpexpensesplitter.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppBottomSheet
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.DestructiveButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_user
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.ui.CurrencyPickerSheet
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about_description
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about_feature_balances
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about_feature_currencies
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about_feature_groups
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about_feature_privacy
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear_confirm
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear_sheet_message
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear_sheet_title
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_edit_name_title
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_download
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_sheet_message
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_name_placeholder
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_save
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_theme
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_version
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Shows the bottom sheet selected in [SettingsUiState.openSheet], if any. */
@Composable
internal fun SettingsSheets(
    uiState: SettingsUiState,
    nameInput: String,
    currencyQuery: String,
    onAction: (SettingsAction) -> Unit,
) {
    val dismiss = { onAction(SettingsAction.SheetDismissed) }
    when (uiState.openSheet) {
        null -> Unit
        SettingsSheet.EditName -> EditNameSheet(uiState, nameInput, onAction, dismiss)
        SettingsSheet.Currency -> CurrencyPickerSheet(
            selected = uiState.currency,
            query = currencyQuery,
            results = uiState.currencyResults,
            onQueryChange = { onAction(SettingsAction.CurrencyQueryChanged(it)) },
            onSelect = { onAction(SettingsAction.CurrencySelected(it)) },
            onDismiss = dismiss,
        )
        SettingsSheet.Theme -> ThemeSheet(uiState.theme, onAction, dismiss)
        SettingsSheet.Export -> AppBottomSheet(stringResource(Res.string.settings_export), dismiss) {
            BodyText(stringResource(Res.string.settings_export_sheet_message))
            PrimaryButton(
                text = stringResource(Res.string.settings_export_download),
                onClick = { onAction(SettingsAction.ConfirmExportClicked) },
                isLoading = uiState.isWorking,
            )
        }
        SettingsSheet.ClearData -> AppBottomSheet(stringResource(Res.string.settings_clear_sheet_title), dismiss) {
            BodyText(stringResource(Res.string.settings_clear_sheet_message))
            DestructiveButton(
                text = stringResource(Res.string.settings_clear_confirm),
                onClick = { onAction(SettingsAction.ConfirmClearClicked) },
                isLoading = uiState.isWorking,
            )
        }
        SettingsSheet.About -> AboutSheet(uiState.versionName, dismiss)
    }
}

@Composable
private fun EditNameSheet(
    uiState: SettingsUiState,
    nameInput: String,
    onAction: (SettingsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val save = { onAction(SettingsAction.SaveNameClicked) }
    AppBottomSheet(stringResource(Res.string.settings_edit_name_title), onDismiss) {
        AppTextField(
            value = nameInput,
            onValueChange = { onAction(SettingsAction.NameChanged(it)) },
            placeholder = stringResource(Res.string.settings_name_placeholder),
            leadingIcon = painterResource(DsRes.drawable.ic_user),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { save() }),
        )
        PrimaryButton(
            text = stringResource(Res.string.settings_save),
            onClick = save,
            enabled = canSaveName(nameInput, uiState.name) || uiState.isWorking,
            isLoading = uiState.isWorking,
        )
    }
}

@Composable
private fun ThemeSheet(
    selected: ThemePreference,
    onAction: (SettingsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(stringResource(Res.string.settings_theme), onDismiss) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm)) {
            listOf(ThemePreference.Light, ThemePreference.Dark, ThemePreference.System).forEach { option ->
                val isSelected = option == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.large)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = { onAction(SettingsAction.ThemeSelected(option)) },
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconTile(option.icon())
                    Text(
                        text = option.label(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = null, // The whole row is the click target.
                        colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSheet(versionName: String, onDismiss: () -> Unit) {
    AppBottomSheet(stringResource(Res.string.settings_about), onDismiss) {
        Text(
            text = stringResource(Res.string.settings_version, versionName),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        BodyText(stringResource(Res.string.settings_about_description))
        Column(verticalArrangement = Arrangement.spacedBy(ExpenseSplitterTheme.spacing.sm)) {
            listOf(
                Res.string.settings_about_feature_groups,
                Res.string.settings_about_feature_balances,
                Res.string.settings_about_feature_currencies,
                Res.string.settings_about_feature_privacy,
            ).forEach { BodyText(stringResource(it)) }
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
