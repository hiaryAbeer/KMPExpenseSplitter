package com.abeerapps.kmpexpensesplitter.core.domain.config

/** Facts about the installed app, supplied by each platform at startup. */
interface AppInfo {
    /** User-facing version, e.g. "1.0.0" (Android versionName / iOS CFBundleShortVersionString). */
    val versionName: String
}
