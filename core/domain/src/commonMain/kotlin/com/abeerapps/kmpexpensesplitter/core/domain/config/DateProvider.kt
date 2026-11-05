package com.abeerapps.kmpexpensesplitter.core.domain.config

import kotlinx.datetime.LocalDate

/** Today's date in the user's time zone. An interface so tests can fix "today". */
fun interface DateProvider {
    fun today(): LocalDate
}
