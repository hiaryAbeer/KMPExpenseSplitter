package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroup
import com.abeerapps.kmpexpensesplitter.core.domain.model.Expense
import com.abeerapps.kmpexpensesplitter.core.domain.model.ExpenseGroupDetails
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.PeopleGroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

/** Expense group names are 1–50 characters (after trimming); they may repeat. */
fun validateExpenseGroupName(name: String): ExpenseGroupNameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> ExpenseGroupNameError.Blank
        trimmed.length > ExpenseGroup.MAX_NAME_LENGTH -> ExpenseGroupNameError.TooLong
        else -> null
    }
}

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
    fun validate(name: String): ExpenseGroupNameError? = validateExpenseGroupName(name)

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

sealed interface RenameExpenseGroupResult {
    data class Success(val group: ExpenseGroup) : RenameExpenseGroupResult
    data class Invalid(val error: ExpenseGroupNameError) : RenameExpenseGroupResult
    data object NotFound : RenameExpenseGroupResult
    data object StorageError : RenameExpenseGroupResult
}

/** Renames an expense group (same name rules as creating one). Its id never changes. */
class RenameExpenseGroupUseCase(private val repository: ExpenseGroupRepository) {
    suspend operator fun invoke(groupId: String, name: String): RenameExpenseGroupResult {
        validateExpenseGroupName(name)?.let { return RenameExpenseGroupResult.Invalid(it) }
        val current = repository.observeExpenseGroups().first().firstOrNull { it.id == groupId }
            ?: return RenameExpenseGroupResult.NotFound
        val renamed = current.copy(name = name.trim())
        return try {
            repository.updateExpenseGroup(renamed)
            RenameExpenseGroupResult.Success(renamed)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            RenameExpenseGroupResult.StorageError
        }
    }
}

/**
 * Deletes an expense group *and its expenses* (the UI asks for confirmation first).
 * Expenses go first, so a failure never leaves expenses pointing at a missing group.
 */
class DeleteExpenseGroupUseCase(
    private val repository: ExpenseGroupRepository,
    private val expenses: ExpenseRepository,
) {
    suspend operator fun invoke(groupId: String): DeleteResult = try {
        expenses.deleteExpensesIn(groupId)
        repository.deleteExpenseGroup(groupId)
        DeleteResult.Deleted
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        DeleteResult.StorageError
    }
}

/** Emits every expense (used by the manage screens to show usage). */
class ObserveExpensesUseCase(private val repository: ExpenseRepository) {
    operator fun invoke(): Flow<List<Expense>> = repository.observeExpenses()
}

/**
 * Emits the details of one expense group (expenses, balances, stats), or `null`
 * once it no longer exists (e.g. it was deleted). Recomputed on any change.
 */
class ObserveExpenseGroupDetailsUseCase(
    private val expenseGroups: ExpenseGroupRepository,
    private val peopleGroups: PeopleGroupRepository,
    private val expenses: ExpenseRepository,
    private val profiles: UserProfileRepository,
) {
    operator fun invoke(expenseGroupId: String): Flow<ExpenseGroupDetails?> =
        combine(
            expenseGroups.observeExpenseGroups(),
            peopleGroups.observeGroups(),
            expenses.observeExpenses(),
            profiles.observeProfile(),
        ) { trips, people, spent, profile ->
            trips.firstOrNull { it.id == expenseGroupId }?.let { group ->
                ExpenseGroupDetails.build(
                    group = group,
                    peopleGroups = people.withYourName(profile?.name),
                    allExpenses = spent,
                    currency = profile?.defaultCurrency ?: SupportedCurrencies.default.code,
                )
            }
        }
}
