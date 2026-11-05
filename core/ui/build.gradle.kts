/*
 * :core:ui — Compose UI that knows about domain types and is shared by
 * several features (e.g. the currency picker used by registration and settings).
 *
 * Sits between :core:designsystem (domain-agnostic building blocks) and the
 * features, so features can share UI without depending on each other.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            api(project(":core:designsystem"))
        }
    }
}

compose.resources {
    // Public: features reuse its icons (e.g. the currency icon in Settings).
    publicResClass = true
    packageOfResClass = "com.abeerapps.kmpexpensesplitter.core.ui.resources"
}
