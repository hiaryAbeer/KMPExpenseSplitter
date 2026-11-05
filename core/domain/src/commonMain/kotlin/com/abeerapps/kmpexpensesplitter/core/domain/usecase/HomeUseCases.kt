package com.abeerapps.kmpexpensesplitter.core.domain.usecase

import com.abeerapps.kmpexpensesplitter.core.domain.model.BalanceSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.CurrencyCode
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummary
import com.abeerapps.kmpexpensesplitter.core.domain.model.Money
import com.abeerapps.kmpexpensesplitter.core.domain.model.UserProfile
import com.abeerapps.kmpexpensesplitter.core.domain.model.GroupSummaries
import com.abeerapps.kmpexpensesplitter.core.domain.model.SupportedCurrencies
import com.abeerapps.kmpexpensesplitter.core.domain.repository.ExpenseRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.GroupRepository
import com.abeerapps.kmpexpensesplitter.core.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Emits the registered user's profile (or `null` if none). */
class ObserveUserProfileUseCase(private val repository: UserProfileRepository) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeProfile()
}

/**
 * Emits a summary of each group (members, total spent, your balance) in the user's
 * default currency, most recently active first. Recomputed whenever groups,
 * expenses or the profile change.
 */
class ObserveGroupsUseCase(
    private val groups: GroupRepository,
    private val expenses: ExpenseRepository,
    private val profiles: UserProfileRepository,
) {
    operator fun invoke(): Flow<List<GroupSummary>> =
        combine(groups.observeGroups(), expenses.observeExpenses(), profiles.observeProfile()) { all, spent, profile ->
            val currency = profile?.defaultCurrency ?: SupportedCurrencies.default.code
            GroupSummaries.build(all.withYourName(profile?.name), spent, currency)
        }
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
