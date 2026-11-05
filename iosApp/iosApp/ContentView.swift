import UIKit
import SwiftUI
import Shared

/// Bridges the shared Compose Multiplatform UI (`App()` in Kotlin) into SwiftUI.
///
/// Native SwiftUI screens can sit alongside it later (e.g. inside a
/// `NavigationStack`) for platform-specific flows.
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Self.Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Self.Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea() // Compose handles insets via safeDrawingPadding().
    }
}
