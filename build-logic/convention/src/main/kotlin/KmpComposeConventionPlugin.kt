import com.abeerapps.kmpexpensesplitter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Adds Compose Multiplatform (shared UI) on top of [KmpLibraryConventionPlugin].
 *
 * Used by UI-bearing modules only (design system, features, app shell). Pure
 * domain/data modules stay Compose-free so they compile fast and stay testable.
 */
class KmpComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("expensesplitter.kmp.library")
        pluginManager.apply("org.jetbrains.compose")
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        extensions.configure<KotlinMultiplatformExtension> {
            sourceSets.getByName("commonMain").dependencies {
                implementation(libs.findLibrary("compose-runtime").get())
                implementation(libs.findLibrary("compose-foundation").get())
                implementation(libs.findLibrary("compose-material3").get())
                implementation(libs.findLibrary("compose-ui").get())
                implementation(libs.findLibrary("compose-components-resources").get())
                implementation(libs.findLibrary("compose-uiToolingPreview").get())
            }
        }

        dependencies {
            // Enables @Preview rendering in Android Studio for commonMain composables.
            add("androidRuntimeClasspath", libs.findLibrary("compose-uiTooling").get())
        }
    }
}
