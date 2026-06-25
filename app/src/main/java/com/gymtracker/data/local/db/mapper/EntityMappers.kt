package com.gymtracker.data.local.db.mapper

import com.gymtracker.data.local.ExerciseDefaults
import com.gymtracker.data.local.db.ExerciseEntity
import com.gymtracker.data.local.db.SessionEntity
import com.gymtracker.data.local.db.WorkoutSetEntity
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet

// WorkoutSet
fun WorkoutSet.toEntity(sessionDate: String) = WorkoutSetEntity(
    sessionDate   = sessionDate,
    exerciseId    = exerciseId,
    exerciseName  = exerciseName,
    reps          = reps,
    weightKg      = weightKg,
    variant       = variant,
    note          = note
)

fun WorkoutSetEntity.toDomain() = WorkoutSet(
    exerciseId   = exerciseId,
    exerciseName = exerciseName,
    reps         = reps,
    weightKg     = weightKg,
    variant      = variant,
    note         = note
)

// Session
fun SessionEntity.toDomain(sets: List<WorkoutSetEntity>) = Session(
    date = date,
    sets = sets.map { it.toDomain() }
)

// Exercise
fun Exercise.toEntity() = ExerciseEntity(
    id              = id,
    name            = name,
    muscle          = muscle,
    routine         = routine,
    emoji           = emoji,
    isStrengthFocus = isStrengthFocus,
    isCustom        = isCustom,
    isCardio        = isCardio
)

fun ExerciseEntity.toDomain() = Exercise(
    id              = id,
    name            = name,
    muscle          = muscle,
    routine         = routine,
    emoji           = emoji,
    color           = ExerciseDefaults.MUSCLE_COLORS[muscle]
        ?: androidx.compose.ui.graphics.Color(0xFF8E8E93),
    isStrengthFocus = isStrengthFocus,
    isCustom        = isCustom,
    isCardio        = isCardio
)