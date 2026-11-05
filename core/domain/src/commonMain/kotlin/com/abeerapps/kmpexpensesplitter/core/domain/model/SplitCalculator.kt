package com.abeerapps.kmpexpensesplitter.core.domain.model

/** What the user entered to divide an expense. Values are in hundredths (see [DecimalInput]). */
sealed interface SplitInput {
    val type: SplitType

    /** Divide equally between [participantIds]. */
    data class Equal(val participantIds: List<String>) : SplitInput {
        override val type get() = SplitType.Equal
    }

    /** Participant id → percentage × 100 (`2500` = 25%). Must total `10_000`. */
    data class Percentage(val percentages: Map<String, Long>) : SplitInput {
        override val type get() = SplitType.Percentage
    }

    /** Participant id → amount in minor units. Must total the expense. */
    data class Exact(val amounts: Map<String, Long>) : SplitInput {
        override val type get() = SplitType.Exact
    }
}

/** Why a split cannot be applied. */
enum class SplitError {
    /** Nobody (or only zero values) selected. */
    NoParticipants,

    /** Percentages do not add up to exactly 100%. */
    PercentagesNotHundred,

    /** Exact amounts do not add up to the expense total. */
    AmountsNotMatchingTotal,
}

/**
 * Pure business rule: turns a total and a [SplitInput] into per-person shares
 * that always add up to exactly the total (no cent is created or lost).
 *
 * Rounding: when cents can't be divided evenly, the leftover cents go one each
 * to the people with the largest rounding loss (ties: first in input order),
 * so results are deterministic and as fair as possible.
 */
object SplitCalculator {

    const val FULL_PERCENT = 10_000L

    sealed interface Result {
        data class Success(val shares: List<ExpenseShare>) : Result
        data class Failure(val error: SplitError) : Result
    }

    fun split(total: Money, input: SplitInput): Result = when (input) {
        is SplitInput.Equal -> {
            val ids = input.participantIds.distinct()
            if (ids.isEmpty()) {
                Result.Failure(SplitError.NoParticipants)
            } else {
                Result.Success(proportional(total, ids.associateWith { 1L }))
            }
        }

        is SplitInput.Percentage -> {
            val positive = input.percentages.filterValues { it > 0 }
            when {
                positive.isEmpty() -> Result.Failure(SplitError.NoParticipants)
                positive.values.sum() != FULL_PERCENT -> Result.Failure(SplitError.PercentagesNotHundred)
                else -> Result.Success(proportional(total, positive))
            }
        }

        is SplitInput.Exact -> {
            val positive = input.amounts.filterValues { it > 0 }
            when {
                positive.isEmpty() -> Result.Failure(SplitError.NoParticipants)
                positive.values.sum() != total.minorUnits -> Result.Failure(SplitError.AmountsNotMatchingTotal)
                else -> Result.Success(positive.map { (id, units) -> ExpenseShare(id, Money(units, total.currency)) })
            }
        }
    }

    /** Divides [total] by [weights] (largest-remainder method), keeping input order. */
    private fun proportional(total: Money, weights: Map<String, Long>): List<ExpenseShare> {
        val weightSum = weights.values.sum()
        val exact = weights.mapValues { (_, weight) -> total.minorUnits * weight }
        val base = exact.mapValues { (_, value) -> value / weightSum }.toMutableMap()
        var leftover = total.minorUnits - base.values.sum()

        val byLargestRemainder = weights.keys.withIndex()
            .sortedWith(compareByDescending<IndexedValue<String>> { exact.getValue(it.value) % weightSum }.thenBy { it.index })
            .map { it.value }
        for (id in byLargestRemainder) {
            if (leftover <= 0) break
            base[id] = base.getValue(id) + 1
            leftover--
        }
        return weights.keys.map { id -> ExpenseShare(id, Money(base.getValue(id), total.currency)) }
    }
}
