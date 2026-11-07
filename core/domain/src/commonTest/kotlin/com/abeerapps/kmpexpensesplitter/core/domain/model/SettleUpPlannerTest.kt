package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettleUpPlannerTest {

    private val jod = CurrencyCode.of("JOD")!!

    private fun balances(vararg people: Pair<String, Long>) =
        people.map { (name, units) -> PersonBalance(name.lowercase(), name, Money(units, jod)) }

    private fun List<Transfer>.described() = map { "${it.fromName}→${it.toName}:${it.amount.minorUnits}" }

    @Test
    fun `the largest debt pays the largest credit first`() {
        // The design's balances: Ahmad +85.25, Sara +12.50, Omar -45.00, Lina -52.75.
        val plan = SettleUpPlanner.plan(balances("Ahmad" to 85_25, "Sara" to 12_50, "Omar" to -45_00, "Lina" to -52_75))

        assertEquals(listOf("Lina→Ahmad:5275", "Omar→Ahmad:3250", "Omar→Sara:1250"), plan.described())
    }

    @Test
    fun `following the plan leaves everyone at zero`() {
        val start = balances("A" to 70_01, "B" to 10_00, "C" to -33_34, "D" to -33_33, "E" to -13_34)
        val net = start.associate { it.participantId to it.balance.minorUnits }.toMutableMap()

        val plan = SettleUpPlanner.plan(start)
        plan.forEach {
            net[it.fromId] = net.getValue(it.fromId) + it.amount.minorUnits
            net[it.toId] = net.getValue(it.toId) - it.amount.minorUnits
        }

        assertTrue(net.values.all { it == 0L })
        assertTrue(plan.size <= start.size - 1)
    }

    @Test
    fun `nothing to pay when everyone is settled`() {
        assertEquals(emptyList(), SettleUpPlanner.plan(balances("A" to 0, "B" to 0)))
        assertEquals(emptyList(), SettleUpPlanner.plan(emptyList()))
    }
}
