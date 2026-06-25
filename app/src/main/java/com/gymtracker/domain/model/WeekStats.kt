package com.gymtracker.domain.model

data class WeekStats(
    val daysTrainedThisWeek: Int,
    val sessionsThisWeek: Int,
    val setsThisWeek: Int,
    val repsThisWeek: Int,
    val volumeThisWeek: Long
)