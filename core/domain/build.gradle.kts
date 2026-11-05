/*
 * :core:domain — the innermost layer of Clean Architecture.
 *
 * Holds entities, repository contracts and use cases. It must not depend on any
 * framework (no Compose, no Android, no persistence library) so business rules
 * stay portable and trivially unit-testable.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpLibrary)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
        }
    }
}
