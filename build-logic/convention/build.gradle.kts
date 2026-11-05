plugins {
    `kotlin-dsl`
}

group = "com.abeerapps.kmpexpensesplitter.buildlogic"

dependencies {
    // compileOnly: the real plugin versions are put on the classpath by the root
    // build script (`apply false`), so every module resolves the same versions.
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.composeCompiler.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kmpLibrary") {
            id = libs.plugins.expensesplitter.kmpLibrary.get().pluginId
            implementationClass = "KmpLibraryConventionPlugin"
        }
        register("kmpCompose") {
            id = libs.plugins.expensesplitter.kmpCompose.get().pluginId
            implementationClass = "KmpComposeConventionPlugin"
        }
    }
}
