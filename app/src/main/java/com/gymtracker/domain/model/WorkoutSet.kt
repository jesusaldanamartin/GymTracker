package com.gymtracker.domain.model

data class WorkoutSet(
    val exerciseId: Int,
    val exerciseName: String,
    val reps: Int,
    val weightKg: Float,
    val variant: String = "",
    val note: String = ""
)