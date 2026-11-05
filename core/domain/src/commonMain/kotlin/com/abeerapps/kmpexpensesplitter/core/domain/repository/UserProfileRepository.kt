package com.abeerapps.kmpexpensesplitter.core.domain.repository

import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/**
 * Source of truth for the local [UserProfile].
 *
 * Declared in the domain layer and implemented in `:core:data` (dependency
 * inversion), so use cases never know how or where the profile is stored.
 */
interface UserProfileRepository {

    /**
     * Emits the stored profile, or `null` when none exists or the stored data is
     * incomplete/invalid. Emits again on every change.
     */
    fun observeProfile(): Flow<UserProfile?>

    /** Persists [profile], replacing any existing one. */
    suspend fun saveProfile(profile: UserProfile)
}
