package com.gymtracker.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        SessionEntity::class,
        WorkoutSetEntity::class,
        ExerciseEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun workoutSetDao(): WorkoutSetDao
    abstract fun exerciseDao(): ExerciseDao
}