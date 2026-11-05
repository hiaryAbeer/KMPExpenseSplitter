import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import com.abeerapps.kmpexpensesplitter.buildlogic.libs
import com.abeerapps.kmpexpensesplitter.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Baseline for every shared Kotlin Multiplatform module.
 *
 * - Targets: Android (AGP KMP library plugin), iosArm64, iosSimulatorArm64.
 * - Android namespace is derived from the Gradle path, e.g. `:core:data` →
 *   `com.abeerapps.kmpexpensesplitter.core.data`, so modules never collide.
 * - `commonTest` gets kotlin-test, coroutines-test and Turbine, so all unit tests
 *   are written once and run on both the JVM host and the iOS simulator.
 */
class KmpLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.multiplatform")
        pluginManager.apply("com.android.kotlin.multiplatform.library")

        extensions.configure<KotlinMultiplatformExtension> {
            iosArm64()
            iosSimulatorArm64()

            extensions.configure<KotlinMultiplatformAndroidLibraryTarget> {
                namespace = "com.abeerapps.kmpexpensesplitter" +
                    path.replace(':', '.').replace('-', '_')
                compileSdk = libs.version("android-compileSdk").toInt()
                minSdk = libs.version("android-minSdk").toInt()
                compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
                withHostTest { }
            }

            compilerOptions {
                // expect/actual classes are still Beta; opt in explicitly to silence the warning.
                freeCompilerArgs.add("-Xexpect-actual-classes")
            }

            sourceSets.getByName("commonTest").dependencies {
                implementation(libs.findLibrary("kotlin-test").get())
                implementation(libs.findLibrary("kotlinx-coroutines-test").get())
                implementation(libs.findLibrary("turbine").get())
            }
        }
    }
}
