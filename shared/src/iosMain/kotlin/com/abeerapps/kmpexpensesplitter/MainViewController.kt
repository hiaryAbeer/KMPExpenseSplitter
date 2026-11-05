package com.abeerapps.kmpexpensesplitter

import androidx.compose.ui.window.ComposeUIViewController
import com.abeerapps.kmpexpensesplitter.config.AppConfig
import com.abeerapps.kmpexpensesplitter.di.initKoin
import platform.UIKit.UIViewController

/**
 * iOS entry points, called from Swift.
 *
 * Swift names: `MainViewControllerKt.MainViewController()` and
 * `MainViewControllerKt.startKoin(isDebug:versionName:)`.
 */
@Suppress("FunctionName", "unused") // Called from Swift.
fun MainViewController(): UIViewController = ComposeUIViewController { App() }

/**
 * Starts the shared DI graph. Call once from `iOSApp.init()`.
 * Named `startKoin` (not `initKoin`) because Kotlin/Native prefixes Swift
 * names starting with `init` with `do`.
 */
@Suppress("unused") // Called from Swift.
fun startKoin(isDebug: Boolean, versionName: String) {
    initKoin(AppConfig(isDebug = isDebug, versionName = versionName))
}
