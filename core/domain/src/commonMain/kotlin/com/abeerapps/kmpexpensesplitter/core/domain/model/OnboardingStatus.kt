package com.abeerapps.kmpexpensesplitter.core.domain.model

/** Whether the user still needs to go through onboarding/registration. */
enum class OnboardingStatus {
    /** No complete profile is stored: show onboarding. */
    Required,

    /** A valid profile exists: go straight to the main app. */
    Completed,
}
