/*
 * :feature:onboarding — first screen for users without a registered profile.
 *
 * Depends on :core:domain (contracts) and :core:designsystem only — never on
 * :core:data — so the feature can be developed and tested with fakes.
 * Navigation is wired by the app shell (:shared); this module just exposes
 * callbacks, which keeps it navigation-library agnostic.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose.viewmodel)
        }
    }
}

compose.resources {
    // Unique, stable package for generated `Res` so modules don't clash.
    packageOfResClass = "com.abeerapps.kmpexpensesplitter.feature.onboarding.resources"
}
