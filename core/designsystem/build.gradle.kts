/*
 * :core:designsystem — shared UI foundation (Compose Multiplatform).
 *
 * Theme (colors, typography, spacing) and stateless building-block components.
 * Features depend on this instead of raw Material so the look stays consistent
 * on Android and iOS and can be re-skinned in one place.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // Re-exported: every screen needs Material3 + lifecycle-aware collection.
            api(libs.compose.material3)
            api(libs.androidx.lifecycle.runtimeCompose)
        }
    }
}
