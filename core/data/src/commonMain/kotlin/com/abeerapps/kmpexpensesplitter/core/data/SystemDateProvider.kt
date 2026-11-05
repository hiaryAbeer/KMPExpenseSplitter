package com.abeerapps.kmpexpensesplitter.core.data

import com.abeerapps.kmpexpensesplitter.core.domain.config.DateProvider
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Today in the device's current time zone (re-read on every call, so zone changes apply). */
internal class SystemDateProvider : DateProvider {
    @OptIn(ExperimentalTime::class)
    override fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
}
