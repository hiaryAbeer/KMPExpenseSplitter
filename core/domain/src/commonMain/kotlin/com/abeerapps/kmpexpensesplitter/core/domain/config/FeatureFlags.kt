package com.abeerapps.kmpexpensesplitter.core.domain.config

/**
 * Switches for behaviour that is built but not yet released.
 *
 * Features read flags through this contract, never through hard-coded booleans,
 * so a flag can later be backed by remote config without touching UI code.
 */
interface FeatureFlags {

    /** Shows the "Skip" action on onboarding (continue without registering). */
    val isOnboardingSkipEnabled: Boolean
}
