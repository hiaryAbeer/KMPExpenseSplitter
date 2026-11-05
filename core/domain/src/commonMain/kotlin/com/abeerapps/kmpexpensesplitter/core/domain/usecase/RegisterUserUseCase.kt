package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of [RegisterUserUseCase]. */
sealed interface RegisterResult {
    data object Success : RegisterResult

    /** Name is blank or longer than [UserProfile.MAX_NAME_LENGTH]. */
    data object InvalidName : RegisterResult

    /** The profile could not be persisted (e.g. disk full). Safe to retry. */
    data object StorageError : RegisterResult
}

/**
 * Registers the local user: validates input and stores the [UserProfile].
 *
 * Once this succeeds, [ObserveOnboardingStatusUseCase] reports `Completed`, so
 * onboarding is not shown on the next launch.
 */
class RegisterUserUseCase(
    private val userProfileRepository: UserProfileRepository,
) {
    suspend operator fun invoke(name: String, defaultCurrency: CurrencyCode): RegisterResult {
        if (!UserProfile.isValidName(name)) return RegisterResult.InvalidName

        return try {
            userProfileRepository.saveProfile(
                UserProfile(name = name.trim(), defaultCurrency = defaultCurrency),
            )
            RegisterResult.Success
        } catch (e: CancellationException) {
            throw e // Never swallow coroutine cancellation.
        } catch (e: Exception) {
            RegisterResult.StorageError
        }
    }
}
