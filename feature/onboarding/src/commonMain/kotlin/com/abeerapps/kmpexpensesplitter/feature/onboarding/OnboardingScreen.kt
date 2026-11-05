package com.abeerapps.kmpexpensesplitter.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.PrimaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.TertiaryButton
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.core.designsystem.util.ObserveAsEvents
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.Res
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.onboarding_illustration
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.onboarding_register
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.onboarding_skip
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.onboarding_subtitle
import com.abeerapps.kmpexpensesplitter.feature.onboarding.resources.onboarding_title
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/** Test tags for UI tests (Android instrumentation / Compose UI tests). */
object OnboardingTestTags {
    const val REGISTER_BUTTON = "onboarding_register"
    const val SKIP_BUTTON = "onboarding_skip"
}

/**
 * Stateful entry point used by the app's navigation graph.
 *
 * @param onNavigateToRegistration called when the user chooses to register.
 * @param onNavigateToHome called when the user skips (only if enabled).
 */
@Composable
fun OnboardingScreen(
    onNavigateToRegistration: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: OnboardingViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            OnboardingEvent.NavigateToRegistration -> onNavigateToRegistration()
            OnboardingEvent.NavigateToHome -> onNavigateToHome()
        }
    }

    OnboardingContent(uiState = uiState, onAction = viewModel::onAction)
}

/** Stateless UI: renders [uiState] and reports user intent via [onAction]. Previewable. */
@Composable
fun OnboardingContent(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = ExpenseSplitterTheme.spacing

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Scrollable so large font scales / small screens never clip the copy.
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.onboarding_illustration),
                    contentDescription = null, // Decorative; the title carries the meaning.
                    modifier = Modifier.size(220.dp),
                )
                Spacer(Modifier.height(spacing.md))
                Text(
                    text = stringResource(Res.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(spacing.md))
                Text(
                    text = stringResource(Res.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                PrimaryButton(
                    text = stringResource(Res.string.onboarding_register),
                    onClick = { onAction(OnboardingAction.RegisterClicked) },
                    modifier = Modifier.testTag(OnboardingTestTags.REGISTER_BUTTON),
                )
                if (uiState.isSkipVisible) {
                    TertiaryButton(
                        text = stringResource(Res.string.onboarding_skip),
                        onClick = { onAction(OnboardingAction.SkipClicked) },
                        modifier = Modifier.testTag(OnboardingTestTags.SKIP_BUTTON),
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun OnboardingContentPreview() {
    ExpenseSplitterTheme {
        OnboardingContent(uiState = OnboardingUiState(isSkipVisible = false), onAction = {})
    }
}

@Preview
@Composable
private fun OnboardingContentWithSkipPreview() {
    ExpenseSplitterTheme(darkTheme = true) {
        OnboardingContent(uiState = OnboardingUiState(isSkipVisible = true), onAction = {})
    }
}
