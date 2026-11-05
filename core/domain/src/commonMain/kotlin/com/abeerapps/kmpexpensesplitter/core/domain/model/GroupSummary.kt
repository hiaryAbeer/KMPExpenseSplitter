package com.abeerapps.kmpexpensesplitter.core.domain.model

/**
 * What the Home screen needs to know about a group.
 *
 * @property memberNames display names, used for avatar initials.
 * @property totalSpent everything spent in the group.
 * @property yourBalance positive = others owe you, negative = you owe, zero = settled.
 */
data class GroupSummary(
    val id: String,
    val name: String,
    val memberNames: List<String>,
    val totalSpent: Money,
    val yourBalance: Money,
)

/** Totals shown on the two Home cards, in the user's default currency. */
data class BalanceSummary(
    /** Sum of positive group balances ("You are owed"). Never negative. */
    val owedToYou: Money,
    /** Sum of negative group balances as a positive amount ("You owe"). Never negative. */
    val youOwe: Money,
)
