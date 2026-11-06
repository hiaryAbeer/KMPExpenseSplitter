package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Emits every expense group, newest first (for pickers such as "Add Expense"). */
class ObserveExpenseGroupsUseCase(private val repository: ExpenseGroupRepository) {
    operator fun invoke(): Flow<List<ExpenseGroup>> = repository.observeExpenseGroups()

    companion object {
        /** Case-insensitive name search; a blank query matches everything. */
        fun search(groups: List<ExpenseGroup>, query: String): List<ExpenseGroup> {
            val q = query.trim()
            return if (q.isEmpty()) groups else groups.filter { it.name.contains(q, ignoreCase = true) }
        }
    }
}

enum class ExpenseGroupNameError { Blank, TooLong }

sealed interface CreateExpenseGroupResult {
    data class Success(val group: ExpenseGroup) : CreateExpenseGroupResult
    data class Invalid(val error: ExpenseGroupNameError) : CreateExpenseGroupResult
    data object StorageError : CreateExpenseGroupResult
}

/**
 * Creates an expense group with a generated unique id. Names are 1–50 characters
 * and may repeat: the id, not the name, identifies the group.
 */
class CreateExpenseGroupUseCase(
    private val repository: ExpenseGroupRepository,
    private val dateProvider: DateProvider,
    private val newId: () -> String = ::randomId,
) {
    fun validate(name: String): ExpenseGroupNameError? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ExpenseGroupNameError.Blank
            trimmed.length > ExpenseGroup.MAX_NAME_LENGTH -> ExpenseGroupNameError.TooLong
            else -> null
        }
    }

    suspend operator fun invoke(name: String): CreateExpenseGroupResult {
        validate(name)?.let { return CreateExpenseGroupResult.Invalid(it) }
        val group = ExpenseGroup(id = newId(), name = name.trim(), createdOn = dateProvider.today())
        return try {
            repository.createExpenseGroup(group)
            CreateExpenseGroupResult.Success(group)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            CreateExpenseGroupResult.StorageError
        }
    }

    private companion object {
        @OptIn(ExperimentalUuidApi::class)
        fun randomId(): String = Uuid.random().toString()
    }
}
