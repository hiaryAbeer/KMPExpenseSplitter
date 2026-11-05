package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.OnboardingStatus
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Decides whether onboarding must be shown.
 *
 * Business rule: onboarding is required until a profile with a non-blank name
 * and a default currency has been registered. The repository already guarantees
 * the currency is valid; the name check guards against legacy/corrupt data.
 */
class ObserveOnboardingStatusUseCase(
    private val userProfileRepository: UserProfileRepository,
) {
    operator fun invoke(): Flow<OnboardingStatus> =
        userProfileRepository.observeProfile()
            .map { profile ->
                if (profile != null && profile.name.isNotBlank()) {
                    OnboardingStatus.Completed
                } else {
                    OnboardingStatus.Required
                }
            }
            .distinctUntilChanged()
}
