package com.abeerapps.kmpexpensesplitter.feature.registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppTextField
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.FieldLabel
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.designsystem.util.ObserveAsEvents
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_user
import com.abeerapps.kmpexpensesplitter.core.ui.CurrencyField
import com.abeerapps.kmpexpensesplitter.core.ui.CurrencyPickerSheet
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.ic_profile_avatar
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_currency_label
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_error_save_failed
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_name_label
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_name_placeholder
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_save
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_subtitle
import com.abeerapps.kmpexpensesplitter.feature.registration.resources.registration_title
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object RegistrationTestTags {
    const val NAME_FIELD = "registration_name"
    const val SAVE_BUTTON = "registration_save"
}

/**
 * Stateful entry point used by the app's navigation graph.
 *
 * @param onRegistered called once the profile is saved.
 */
@Composable
fun RegistrationScreen(
    onRegistered: () -> Unit,
    viewModel: RegistrationViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegistrationEvent.Registered -> onRegistered()
        }
    }

    RegistrationContent(uiState = uiState, onAction = viewModel::onAction)
}

/** Stateless UI for "Set up your profile". Previewable. */
@Composable
fun RegistrationContent(
    uiState: RegistrationUiState,
    onAction: (RegistrationAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current

    uiState.error?.let { error ->
        LaunchedEffect(error) {
            val message = when (error) {
                RegistrationError.SaveFailed -> getString(Res.string.registration_error_save_failed)
            }
            snackbarHostState.showSnackbar(message)
            onAction(RegistrationAction.ErrorShown)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0), // Insets are handled per section below.
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            // Header + form scroll together, so small screens / large fonts never clip.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Header()
                Form(
                    uiState = uiState,
                    onAction = onAction,
                    onCurrencyClick = {
                        focusManager.clearFocus() // Close the name keyboard before the sheet opens.
                        onAction(RegistrationAction.CurrencyFieldClicked)
                    },
                )
            }

            // Bottom action stays above the keyboard and the navigation bar.
            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
                    .padding(horizontal = ExpenseSplitterTheme.spacing.screenHorizontal, vertical = ExpenseSplitterTheme.spacing.md),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.registration_save),
                    onClick = {
                        focusManager.clearFocus()
                        onAction(RegistrationAction.SaveClicked)
                    },
                    enabled = uiState.canSave || uiState.isSaving,
                    isLoading = uiState.isSaving,
                    modifier = Modifier.testTag(RegistrationTestTags.SAVE_BUTTON),
                )
            }
        }
    }
}

@Composable
private fun Header() {
    val background = MaterialTheme.colorScheme.background
    val gradientTop = ExpenseSplitterTheme.extendedColors.headerGradientTop

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(gradientTop, background)))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = ExpenseSplitterTheme.spacing.screenHorizontal, vertical = ExpenseSplitterTheme.spacing.md),
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_profile_avatar),
                contentDescription = null, // Decorative.
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(ExpenseSplitterTheme.spacing.md))
        Text(
            text = stringResource(Res.string.registration_title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.registration_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Form(
    uiState: RegistrationUiState,
    onAction: (RegistrationAction) -> Unit,
    onCurrencyClick: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
            // No top padding: the header's 16dp bottom padding already separates them.
            .padding(horizontal = ExpenseSplitterTheme.spacing.screenHorizontal),
    ) {
        FieldLabel(stringResource(Res.string.registration_name_label))
        Spacer(Modifier.height(8.dp))
        AppTextField(
            value = uiState.name,
            onValueChange = { onAction(RegistrationAction.NameChanged(it)) },
            placeholder = stringResource(Res.string.registration_name_placeholder),
            leadingIcon = painterResource(DsRes.drawable.ic_user),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.testTag(RegistrationTestTags.NAME_FIELD),
        )

        Spacer(Modifier.height(ExpenseSplitterTheme.spacing.md))
        FieldLabel(stringResource(Res.string.registration_currency_label))
        Spacer(Modifier.height(8.dp))
        CurrencyField(
            selected = uiState.selectedCurrency,
            isSheetVisible = uiState.isCurrencySheetVisible,
            onClick = onCurrencyClick,
        )
    }

    if (uiState.isCurrencySheetVisible) {
        CurrencyPickerSheet(
            selected = uiState.selectedCurrency,
            query = uiState.currencyQuery,
            results = uiState.currencyResults,
            onQueryChange = { onAction(RegistrationAction.CurrencyQueryChanged(it)) },
            onSelect = { onAction(RegistrationAction.CurrencySelected(it)) },
            onDismiss = { onAction(RegistrationAction.CurrencySheetDismissed) },
        )
    }
}

@Preview
@Composable
private fun RegistrationContentPreview() {
    ExpenseSplitterTheme {
        RegistrationContent(uiState = RegistrationUiState(), onAction = {})
    }
}

@Preview
@Composable
private fun RegistrationContentDarkPreview() {
    ExpenseSplitterTheme(darkTheme = true) {
        RegistrationContent(uiState = RegistrationUiState(name = "Alex"), onAction = {})
    }
}
