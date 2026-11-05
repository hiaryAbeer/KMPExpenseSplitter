import SwiftUI
import Shared

/// iOS process entry point.
///
/// Starts the shared Kotlin DI graph exactly once, before any UI is created.
/// `isDebug` follows the Xcode build configuration: Debug → verbose logging,
/// Release → warnings/errors only (mirrors `BuildConfig.DEBUG` on Android).
@main
struct iOSApp: App {

    init() {
        #if DEBUG
        let isDebug = true
        #else
        let isDebug = false
        #endif
        MainViewControllerKt.startKoin(isDebug: isDebug)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
