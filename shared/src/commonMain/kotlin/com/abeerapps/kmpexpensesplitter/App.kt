package com.abeerapps.kmpexpensesplitter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.abeerapps.kmpexpensesplitter.core.designsystem.theme.ExpenseSplitterTheme
import com.abeerapps.kmpexpensesplitter.navigation.AppNavHost
import org.koin.compose.viewmodel.koinViewModel

/**
 * Root composable shared by Android (`MainActivity`) and iOS (`MainViewController`).
 *
 * While the stored profile is being read (a few ms) only the themed background
 * is drawn, so there is no flash of the wrong screen.
 */
@Composable
fun App(viewModel: AppViewModel = koinViewModel()) {
    ExpenseSplitterTheme {
        val startState by viewModel.startState.collectAsStateWithLifecycle()

        when (val state = startState) {
            AppStartState.Loading -> Box(
                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
            )
            is AppStartState.Ready -> AppNavHost(startDestination = state.startDestination)
        }
    }
}
