package com.abeerapps.kmpexpensesplitter.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.AppNavigationBar
import com.abeerapps.kmpexpensesplitter.core.designsystem.component.NavigationItem
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.Res as DsRes
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_home
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_settings
import com.abeerapps.kmpexpensesplitter.core.designsystem.resources.ic_users
import com.abeerapps.kmpexpensesplitter.feature.home.HomeScreen
import com.abeerapps.kmpexpensesplitter.feature.settings.SettingsScreen
import com.abeerapps.kmpexpensesplitter.resources.Res
import com.abeerapps.kmpexpensesplitter.resources.nav_groups
import com.abeerapps.kmpexpensesplitter.resources.nav_home
import com.abeerapps.kmpexpensesplitter.resources.nav_settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Top-level tabs, in bottom-bar order. */
private enum class MainTab { Home, Groups, Settings }

/**
 * Main app shell after registration: the design's bottom navigation with
 * Home, Groups and Settings. Groups is a placeholder for now.
 *
 * The selected tab survives rotation and process death (rememberSaveable).
 */
@Composable
internal fun MainShell(
    onCreateGroup: () -> Unit,
    onGroupClick: (groupId: String) -> Unit,
    onDataCleared: () -> Unit,
) {
    var selected by rememberSaveable { mutableIntStateOf(MainTab.Home.ordinal) }
    val items = listOf(
        NavigationItem(stringResource(Res.string.nav_home), painterResource(DsRes.drawable.ic_home)),
        NavigationItem(stringResource(Res.string.nav_groups), painterResource(DsRes.drawable.ic_users)),
        NavigationItem(stringResource(Res.string.nav_settings), painterResource(DsRes.drawable.ic_settings)),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        // The nav bar handles the bottom inset itself; screens handle the top.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            AppNavigationBar(items = items, selectedIndex = selected, onSelect = { selected = it })
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            when (MainTab.entries[selected]) {
                MainTab.Home -> HomeScreen(onCreateGroup = onCreateGroup, onGroupClick = onGroupClick)
                MainTab.Groups -> PlaceholderScreen(title = "Groups")
                MainTab.Settings -> SettingsScreen(onDataCleared = onDataCleared)
            }
        }
    }
}
