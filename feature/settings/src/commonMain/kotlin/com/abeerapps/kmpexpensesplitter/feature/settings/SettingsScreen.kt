package com.abeerapps.kmpexpensesplitter.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppCard
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_chevron_right
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_download
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_info
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_monitor
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_moon
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_sun
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_trash
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.designsystem.util.ObserveAsEvents
import com.abeerapps.kmpexpensesplitter.core.domain.model.ThemePreference
import com.abeerapps.kmpexpensesplitter.core.ui.displayLabel
import com.abeerapps.kmpexpensesplitter.core.ui.resources.Res as UiRes
import com.abeerapps.kmpexpensesplitter.core.ui.resources.ic_currency
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_about
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear_failed
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_clear_subtitle
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_currency
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_edit_profile
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_failed
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_saved
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_subtitle
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_export_unavailable
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_save_failed
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_section_about
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_section_data
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_section_preferences
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_theme
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_theme_dark
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_theme_light
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_theme_system
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_title
import com.abeerapps.kmpexpensesplitter.feature.settings.resources.settings_version
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests. */
object SettingsTestTags {
    const val PROFILE_CARD = "settings_profile"
    const val CURRENCY_ROW = "settings_currency"
    const val THEME_ROW = "settings_theme"
    const val EXPORT_ROW = "settings_export"
    const val CLEAR_ROW = "settings_clear"
    const val ABOUT_ROW = "settings_about"
}

/**
 * Stateful entry point for the Settings tab.
 *
 * @param onDataCleared all local data was deleted; the app should restart at onboarding.
 */
@Composable
fun SettingsScreen(
    onDataCleared: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nameInput by viewModel.nameInput.collectAsStateWithLifecycle()
    val currencyQuery by viewModel.currencyQuery.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            SettingsEvent.DataCleared -> onDataCleared()
        }
    }

    SettingsContent(uiState = uiState, onAction = viewModel::onAction, modifier = modifier)
    SettingsSheets(
        uiState = uiState,
        nameInput = nameInput,
        currencyQuery = currencyQuery,
        onAction = viewModel::onAction,
    )
}

/** Stateless Settings list. Previewable. */
@Composable
fun SettingsContent(
    uiState: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ExpenseSplitterTheme.spacing
    val snackbarHostState = remember { SnackbarHostState() }

    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                when (message) {
                    SettingsMessage.SaveFailed -> getString(Res.string.settings_save_failed)
                    SettingsMessage.ClearFailed -> getString(Res.string.settings_clear_failed)
                    SettingsMessage.ExportFailed -> getString(Res.string.settings_export_failed)
                    is SettingsMessage.ExportSaved ->
                        getString(Res.string.settings_export_saved, message.fileName, message.location)
                },
            )
            onAction(SettingsAction.MessageShown)
        }
    }

    Box(modifier.fillMaxSize()) {
        if (!uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Text(
                    text = stringResource(Res.string.settings_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )

                ProfileCard(name = uiState.name, onClick = { onAction(SettingsAction.ProfileClicked) })

                Section(stringResource(Res.string.settings_section_preferences)) {
                    SettingsRow(
                        icon = painterResource(UiRes.drawable.ic_currency),
                        title = stringResource(Res.string.settings_currency),
                        subtitle = uiState.currency.displayLabel(),
                        onClick = { onAction(SettingsAction.CurrencyClicked) },
                        modifier = Modifier.testTag(SettingsTestTags.CURRENCY_ROW),
                    )
                    SettingsRow(
                        icon = uiState.theme.icon(),
                        title = stringResource(Res.string.settings_theme),
                        subtitle = uiState.theme.label(),
                        onClick = { onAction(SettingsAction.ThemeClicked) },
                        modifier = Modifier.testTag(SettingsTestTags.THEME_ROW),
                    )
                }

                Section(stringResource(Res.string.settings_section_data)) {
                    SettingsRow(
                        icon = painterResource(DsRes.drawable.ic_download),
                        title = stringResource(Res.string.settings_export),
                        subtitle = stringResource(
                            if (uiState.canExport) Res.string.settings_export_subtitle else Res.string.settings_export_unavailable,
                        ),
                        enabled = uiState.canExport,
                        onClick = { onAction(SettingsAction.ExportClicked) },
                        modifier = Modifier.testTag(SettingsTestTags.EXPORT_ROW),
                    )
                    SettingsRow(
                        icon = painterResource(DsRes.drawable.ic_trash),
                        title = stringResource(Res.string.settings_clear),
                        subtitle = stringResource(Res.string.settings_clear_subtitle),
                        onClick = { onAction(SettingsAction.ClearDataClicked) },
                        modifier = Modifier.testTag(SettingsTestTags.CLEAR_ROW),
                    )
                }

                Section(stringResource(Res.string.settings_section_about)) {
                    SettingsRow(
                        icon = painterResource(DsRes.drawable.ic_info),
                        title = stringResource(Res.string.settings_about),
                        subtitle = stringResource(Res.string.settings_version, uiState.versionName),
                        onClick = { onAction(SettingsAction.AboutClicked) },
                        modifier = Modifier.testTag(SettingsTestTags.ABOUT_ROW),
                    )
                }
            }
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
    }
}

/** Top card: avatar, name and a chevron. Tapping it edits the profile. */
@Composable
private fun ProfileCard(name: String, onClick: () -> Unit) {
    val colors = ExpenseSplitterTheme.extendedColors
    val primary = MaterialTheme.colorScheme.primary
    val editLabel = stringResource(Res.string.settings_edit_profile)

    AppCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(onClickLabel = editLabel, role = Role.Button, onClick = onClick)
            .testTag(SettingsTestTags.PROFILE_CARD),
        tint = Brush.linearGradient(listOf(primary.copy(alpha = 0.10f), primary.copy(alpha = 0.05f))),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(64.dp).background(colors.avatarColorFor(name), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = name.trim().take(1).uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onAvatar,
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Chevron()
        }
    }
}

/** "PREFERENCES" label followed by its cards (12dp below the label, 8dp between cards). */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    val spacing = ExpenseSplitterTheme.spacing
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.55.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = spacing.xs).semantics { heading() },
        )
        content()
    }
}

/** One settings card: tinted round icon, title + subtitle, chevron. */
@Composable
private fun SettingsRow(
    icon: Painter,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    AppCard(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .alpha(if (enabled) 1f else 0.5f),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(icon)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Chevron()
        }
    }
}

/** 40dp circle in primary @10% with a 20dp primary icon (as in the design). */
@Composable
internal fun IconTile(icon: Painter) {
    Box(
        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Chevron() {
    Icon(
        painter = painterResource(DsRes.drawable.ic_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(20.dp),
    )
}

@Composable
internal fun ThemePreference.icon(): Painter = painterResource(
    when (this) {
        ThemePreference.Light -> DsRes.drawable.ic_sun
        ThemePreference.Dark -> DsRes.drawable.ic_moon
        ThemePreference.System -> DsRes.drawable.ic_monitor
    },
)

@Composable
internal fun ThemePreference.label(): String = stringResource(
    when (this) {
        ThemePreference.Light -> Res.string.settings_theme_light
        ThemePreference.Dark -> Res.string.settings_theme_dark
        ThemePreference.System -> Res.string.settings_theme_system
    },
)

@Preview
@Composable
private fun SettingsPreview() {
    ExpenseSplitterTheme {
        SettingsContent(
            uiState = SettingsUiState(isLoading = false, name = "Ahmad", theme = ThemePreference.Light, versionName = "1.0.0"),
            onAction = {},
        )
    }
}

@Preview
@Composable
private fun SettingsDarkPreview() {
    ExpenseSplitterTheme(darkTheme = true) {
        SettingsContent(
            uiState = SettingsUiState(isLoading = false, name = "Ahmad", theme = ThemePreference.Dark, versionName = "1.0.0"),
            onAction = {},
        )
    }
}
