package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.model.Transfer
import com.abeerapps.kmpexpensesplitter.core.domain.testutil.FakeSettlementRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettleUpUseCaseTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)
    private val repository = FakeSettlementRepository()
    private var nextId = 0
    private val settleUp = SettleUpUseCase(repository, DateProvider { today }, newId = { "s${nextId++}" })

    private val transfers = listOf(
        Transfer("o", "Omar", "a", "Ahmad", Money(45_00, jod)),
        Transfer("l", "Lina", "a", "Ahmad", Money(52_75, jod)),
    )

    @Test
    fun `records every transfer as paid today`() = runTest {
        assertEquals(SettleUpResult.Settled, settleUp("trip", transfers))
        assertEquals(
            listOf(
                Settlement("s0", "trip", "o", "a", Money(45_00, jod), today),
                Settlement("s1", "trip", "l", "a", Money(52_75, jod), today),
            ),
            repository.settlements.value,
        )
    }

    @Test
    fun `a storage failure is reported and nothing is recorded`() = runTest {
        repository.failNext = true
        assertEquals(SettleUpResult.StorageError, settleUp("trip", transfers))
        assertTrue(repository.settlements.value.isEmpty())
    }

    @Test
    fun `an already settled group needs no write`() = runTest {
        assertEquals(SettleUpResult.Settled, settleUp("trip", emptyList()))
        assertTrue(repository.settlements.value.isEmpty())
    }
}
