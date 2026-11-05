/*
 * :core:data — implements the domain repository contracts.
 *
 * Owns persistence (DataStore today; Room/SQLDelight for expenses and Ktor for
 * sync will land here too). Exposes only Koin modules + domain types; concrete
 * classes are `internal` so features can't bypass the domain layer.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.androidx.datastore.preferences.core)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.android)
        }
    }
}
