package com.gymtracker.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val muscle: String,
    val routine: String,
    val emoji: String,
    val isStrengthFocus: Boolean,
    val isCustom: Boolean,
    val isCardio: Boolean
)