package com.gymtracker.domain.model

fun estimatedOneRM(weightKg: Float, reps: Int): Float {
    if (weightKg == 0f || reps == 0) return 0f
    return weightKg * (1f + reps / 30f)
}

fun bestE1RM(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { estimatedOneRM(it.weightKg, it.reps) } ?: 0f

fun bestHypertrophyScore(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { it.reps * it.weightKg } ?: 0f