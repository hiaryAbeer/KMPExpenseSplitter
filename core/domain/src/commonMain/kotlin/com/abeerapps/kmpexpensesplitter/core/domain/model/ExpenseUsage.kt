package com.abeerapps.kmpexpensesplitter.core.domain.model

/** Number of expenses in each expense group (missing key = none). */
fun List<Expense>.countByExpenseGroup(): Map<String, Int> = groupingBy { it.expenseGroupId }.eachCount()

/**
 * For each people group used by an expense: the ids of the people its expenses
 * reference (payers and shares). A group missing from the map is unused.
 */
fun List<Expense>.peopleGroupUsage(): Map<String, Set<String>> =
    groupBy { it.peopleGroupId }.mapValues { (_, expenses) ->
        expenses.flatMapTo(HashSet()) { expense -> expense.shares.map { it.participantId } + expense.paidBy }
    }
