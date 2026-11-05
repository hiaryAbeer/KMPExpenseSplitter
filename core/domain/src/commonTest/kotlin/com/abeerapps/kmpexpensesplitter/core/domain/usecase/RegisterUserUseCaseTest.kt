package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RegisterUserUseCaseTest {

    private val eur = CurrencyCode.of("EUR")!!
    private val repository = RecordingRepository()
    private val registerUser = RegisterUserUseCase(repository)

    @Test
    fun `valid input saves trimmed profile`() = runTest {
        val result = registerUser("  Abeer ", eur)

        assertEquals(RegisterResult.Success, result)
        assertEquals(UserProfile("Abeer", eur), repository.saved)
    }

    @Test
    fun `blank name is rejected without saving`() = runTest {
        assertEquals(RegisterResult.InvalidName, registerUser("   ", eur))
        assertNull(repository.saved)
    }

    @Test
    fun `name longer than max is rejected`() = runTest {
        val tooLong = "a".repeat(UserProfile.MAX_NAME_LENGTH + 1)
        assertEquals(RegisterResult.InvalidName, registerUser(tooLong, eur))
    }

    @Test
    fun `name of exactly max length is accepted`() = runTest {
        val maxName = "a".repeat(UserProfile.MAX_NAME_LENGTH)
        assertEquals(RegisterResult.Success, registerUser(maxName, eur))
    }

    @Test
    fun `storage failure is reported instead of thrown`() = runTest {
        repository.failure = IllegalStateException("disk full")
        assertEquals(RegisterResult.StorageError, registerUser("Abeer", eur))
    }

    private class RecordingRepository : UserProfileRepository {
        var saved: UserProfile? = null
        var failure: Exception? = null
        override fun observeProfile(): Flow<UserProfile?> = flowOf(saved)
        override suspend fun saveProfile(profile: UserProfile) {
            failure?.let { throw it }
            saved = profile
        }
    }
}
