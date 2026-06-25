package com.gymtracker.domain.model

data class Session(
    val date: String,
    val sets: List<WorkoutSet>
)