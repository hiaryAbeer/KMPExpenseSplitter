/*
 * :feature:home — the Home tab: greeting, balance summary, group search and
 * recent groups (with an empty state until groups exist).
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
    packageOfResClass = "com.abeerapps.kmpexpensesplitter.feature.home.resources"
}
