/*
 * :feature:groups — the Groups tab: every expense group with search (by name),
 * balance filters, an empty state, and the "+" button (Add Expense).
 *
 * Depends on :core:domain, :core:designsystem and :core:ui, like every feature.
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
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.compose.viewmodel)
        }
    }
}

compose.resources {
    packageOfResClass = "com.abeerapps.kmpexpensesplitter.feature.groups.resources"
}
