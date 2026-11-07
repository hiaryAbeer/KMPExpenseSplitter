/*
 * :core:data — implements the domain repository contracts.
 *
 * Owns persistence: DataStore for the profile and preferences, SQLDelight
 * (SQLite) for expense groups, people groups, expenses and settlements; Ktor for
 * sync will land here too. Exposes only Koin modules + domain types; concrete
 * classes are `internal` so features can't bypass the domain layer.
 */
plugins {
    alias(libs.plugins.expensesplitter.kmpLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":core:domain"))
            implementation(libs.androidx.datastore.preferences.core)
            implementation(libs.sqldelight.coroutines)
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.kermit)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.android)
            implementation(libs.sqldelight.androidDriver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.nativeDriver)
        }
        getByName("androidHostTest").dependencies {
            implementation(libs.sqldelight.sqliteDriver)
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.abeerapps.kmpexpensesplitter.core.data.db")
            // Schema snapshots in src/commonMain/sqldelight/databases let `verifySqlDelightMigration`
            // prove every future .sqm migration upgrades version 1 correctly.
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            verifyMigrations.set(true)
        }
    }
}
