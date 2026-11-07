package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.Settlement
import com.abeerapps.kmpexpensesplitter.core.domain.model.Transfer
import com.abeerapps.kmpexpensesplitter.core.domain.repository.SettlementRepository
import kotlinx.coroutines.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

sealed interface SettleUpResult {
    data object Settled : SettleUpResult
    data object StorageError : SettleUpResult
}

/**
 * "Mark as settled": records every [Transfer] of the settle-up plan as paid today,
 * which brings every balance in the expense group to zero.
 */
class SettleUpUseCase(
    private val repository: SettlementRepository,
    private val dateProvider: DateProvider,
    private val newId: () -> String = ::randomId,
) {
    suspend operator fun invoke(expenseGroupId: String, transfers: List<Transfer>): SettleUpResult {
        if (transfers.isEmpty()) return SettleUpResult.Settled
        val today = dateProvider.today()
        val settlements = transfers.map {
            Settlement(newId(), expenseGroupId, fromId = it.fromId, toId = it.toId, amount = it.amount, date = today)
        }
        return try {
            repository.addSettlements(settlements)
            SettleUpResult.Settled
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            SettleUpResult.StorageError
        }
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
