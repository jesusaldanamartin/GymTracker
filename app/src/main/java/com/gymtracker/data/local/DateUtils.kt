package com.gymtracker.data.local

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

fun currentWeekMonday(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun currentWeekSunday(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

fun currentWeekDates(): List<String> {
    val monday = currentWeekMonday()
    return (0..6).map { monday.plusDays(it.toLong()).toString() }
}