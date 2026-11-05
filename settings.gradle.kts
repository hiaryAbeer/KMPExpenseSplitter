rootProject.name = "KMPExpenseSplitter"

pluginManagement {
    // Convention plugins shared by all modules (see "Build logic" in README.md).
    includeBuild("build-logic")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

// App entry points
include(":androidApp")
include(":shared")          // Umbrella: app shell, navigation, DI graph, iOS framework

// Core layers
include(":core:domain")     // Entities, repository contracts, use cases (pure Kotlin)
include(":core:data")       // Repository implementations, persistence
include(":core:designsystem") // Theme + reusable Compose components

// Features
include(":feature:onboarding")
include(":feature:registration")
