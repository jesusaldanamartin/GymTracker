package com.gymtracker.data.local.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_sets",
    foreignKeys = [ForeignKey(
        entity = SessionEntity::class,
        parentColumns = ["date"],
        childColumns = ["sessionDate"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("sessionDate")]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionDate: String,
    val exerciseId: Int,
    val exerciseName: String,
    val reps: Int,
    val weightKg: Float,
    val variant: String = "",
    val note: String = ""
)