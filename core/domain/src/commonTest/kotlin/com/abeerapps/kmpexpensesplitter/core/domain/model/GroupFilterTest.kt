package com.abeerapps.kmpexpensesplitter.core.domain.model

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class GroupFilterTest {

    private val jod = CurrencyCode.of("JOD")!!
    private val today = LocalDate(2026, 4, 29)

    private fun group(name: String, balance: Long, lastActivity: LocalDate = today) =
        GroupSummary(name, name, emptyList(), Money(0, jod), Money(balance, jod), lastActivity)

    private val owe = group("Apartment Bills", -45_00)
    private val owed = group("Aqaba Trip", 85_25, LocalDate(2026, 4, 1))
    private val settled = group("Friends Dinner", 0, LocalDate(2026, 3, 29))
    private val all = listOf(owe, owed, settled)

    private fun names(filter: GroupFilter, query: String = "") = all.filterGroups(filter, query, today).map { it.name }

    @Test
    fun `all keeps every group in order`() {
        assertEquals(listOf("Apartment Bills", "Aqaba Trip", "Friends Dinner"), names(GroupFilter.All))
    }

    @Test
    fun `balance filters split by the sign of your balance`() {
        assertEquals(listOf("Apartment Bills"), names(GroupFilter.YouOwe))
        assertEquals(listOf("Aqaba Trip"), names(GroupFilter.YouAreOwed))
        assertEquals(listOf("Friends Dinner"), names(GroupFilter.Settled))
    }

    @Test
    fun `recently active means within the last 30 days`() {
        // Apr 1 is 28 days ago (in), Mar 29 is 31 days ago (out).
        assertEquals(listOf("Apartment Bills", "Aqaba Trip"), names(GroupFilter.RecentlyActive))
    }

    @Test
    fun `search matches the expense group name and combines with the filter`() {
        assertEquals(listOf("Aqaba Trip"), names(GroupFilter.All, " AQABA "))
        assertEquals(listOf("Aqaba Trip"), names(GroupFilter.YouAreOwed, "a"))
        assertEquals(emptyList(), names(GroupFilter.YouOwe, "aqaba"))
    }
}
