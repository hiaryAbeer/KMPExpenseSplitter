/*
 * :feature:registration — "Set up your profile": name + default currency.
 *
 * Like every feature it depends only on :core:domain and :core:designsystem.
 * On success it reports `onRegistered`; the app shell decides where to go next.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpCompose)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:domain"))
            implementation(project(":core:designsystem"))
            implementation(project(":core:ui"))
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.viewmodelSavedstate)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose.viewmodel)
        }
    }
}

compose.resources {
    packageOfResClass = "com.abeerapps.kmpexpensesplitter.feature.registration.resources"
}
