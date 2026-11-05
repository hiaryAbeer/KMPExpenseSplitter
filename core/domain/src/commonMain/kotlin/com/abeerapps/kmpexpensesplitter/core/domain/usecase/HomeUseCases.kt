package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.BalanceSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow

/** Emits the registered user's profile (or `null` if none). */
class ObserveUserProfileUseCase(private val repository: UserProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}

/** Emits the user's groups, most recently active first. */
class ObserveGroupsUseCase(private val repository: GroupRepository) {
    operator fun invoke(): Flow<List<GroupSummary>> = repository.observeGroups()
}

/**
 * Business rule for the Home totals: sum what others owe you and what you owe
 * across all groups, in the user's default [currency].
 *
 * Groups in other currencies are skipped until currency conversion exists, so
 * totals never mix currencies.
 */
class CalculateBalanceSummaryUseCase {
    operator fun invoke(groups: List<GroupSummary>, currency: CurrencyCode): BalanceSummary {
        var owedToYou = Money.zero(currency)
        var youOwe = Money.zero(currency)
        groups.map { it.yourBalance }
            .filter { it.currency == currency }
            .forEach { balance ->
                when {
                    balance.isPositive -> owedToYou += balance
                    balance.isNegative -> youOwe += -balance
                }
            }
        return BalanceSummary(owedToYou = owedToYou, youOwe = youOwe)
    }
}
