package com.abeerapps.kmpexpensesplitter.config

import com.abeerapps.kmpexpensesplitter.core.domain.config.AppInfo
import com.abeerapps.kmpexpensesplitter.core.domain.config.FeatureFlags

/**
 * Build-time configuration supplied by each platform entry point.
 *
 * Android passes `BuildConfig.DEBUG`; iOS passes the Swift `DEBUG` flag, so the
 * same shared code behaves correctly in debug and production builds.
 *
 * @property isDebug enables verbose logging and debug-only tooling.
 * @property versionName user-facing version (Android versionName / iOS CFBundleShortVersionString).
 */
data class AppConfig(
    val isDebug: Boolean,
    override val versionName: String,
) : AppInfo

/**
 * Release-controlled feature flags. Flip a value here to ship/hide a feature;
 * replace with a remote-config backed implementation when needed.
 */
internal object StaticFeatureFlags : FeatureFlags {
    /** Hidden until the "continue without account" flow is designed. */
    override val isOnboardingSkipEnabled: Boolean = false
}
