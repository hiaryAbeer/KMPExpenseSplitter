package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/**
 * What the Home screen needs to know about a group.
 *
 * @property memberNames display names, used for avatar initials.
 * @property totalSpent everything spent in the group.
 * @property yourBalance positive = others owe you, negative = you owe, zero = settled.
 * @property lastActivity latest expense end date, or the creation date without expenses.
 */
data class GroupSummary(
    val id: String,
    val name: String,
    val memberNames: List<String>,
    val totalSpent: Money,
    val yourBalance: Money,
    val lastActivity: LocalDate,
)

/** The filter chips on the Groups tab. */
enum class GroupFilter {
    All,

    /** Your balance is negative. */
    YouOwe,

    /** Your balance is positive. */
    YouAreOwed,

    /** Your balance is zero (including groups without expenses yet). */
    Settled,

    /** Active (an expense, or created) within the last [RECENT_DAYS] days. */
    RecentlyActive;

    companion object {
        const val RECENT_DAYS = 30
    }
}

/**
 * Applies a [GroupFilter] and a name search (case-insensitive, trimmed) to
 * summaries, keeping their order. Pure, so the Groups tab rules are unit-tested.
 */
fun List<GroupSummary>.filterGroups(filter: GroupFilter, query: String, today: LocalDate): List<GroupSummary> {
    val q = query.trim()
    val recentSince = today.minus(DatePeriod(days = GroupFilter.RECENT_DAYS))
    return filter { group ->
        val matchesFilter = when (filter) {
            GroupFilter.All -> true
            GroupFilter.YouOwe -> group.yourBalance.isNegative
            GroupFilter.YouAreOwed -> group.yourBalance.isPositive
            GroupFilter.Settled -> group.yourBalance.isZero
            GroupFilter.RecentlyActive -> group.lastActivity >= recentSince
        }
        matchesFilter && (q.isEmpty() || group.name.contains(q, ignoreCase = true))
    }
}

/** Totals shown on the two Home cards, in the user's default currency. */
data class BalanceSummary(
    /** Sum of positive group balances ("You are owed"). Never negative. */
    val owedToYou: Money,
    /** Sum of negative group balances as a positive amount ("You owe"). Never negative. */
    val youOwe: Money,
)
