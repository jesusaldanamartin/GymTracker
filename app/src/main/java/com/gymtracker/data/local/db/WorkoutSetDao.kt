package com.gymtracker.data.local.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {

    @Query("SELECT * FROM workout_sets WHERE sessionDate = :date ORDER BY id ASC")
    fun observeSetsForDate(date: String): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets ORDER BY sessionDate ASC, id ASC")
    fun observeAll(): Flow<List<WorkoutSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>)

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity)

    @Query("DELETE FROM workout_sets WHERE sessionDate = :date")
    suspend fun deleteSetsForSession(date: String)

    @Query("""
        DELETE FROM workout_sets 
        WHERE sessionDate = :date 
        AND exerciseId = :exerciseId 
        AND reps = :reps 
        AND weightKg = :weightKg 
        AND variant = :variant
        AND id = (
            SELECT id FROM workout_sets 
            WHERE sessionDate = :date 
            AND exerciseId = :exerciseId 
            AND reps = :reps 
            AND weightKg = :weightKg 
            AND variant = :variant
            LIMIT 1
        )
    """)
    suspend fun deleteSpecificSet(
        date: String,
        exerciseId: Int,
        reps: Int,
        weightKg: Float,
        variant: String
    )
}