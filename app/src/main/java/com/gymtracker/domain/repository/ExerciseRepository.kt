package com.gymtracker.domain.repository

import com.gymtracker.domain.model.Exercise
import kotlinx.coroutines.flow.Flow

interface ExerciseRepository {
    fun getCustomExercises(): Flow<List<Exercise>>
    fun getImportedExercises(): Flow<List<Exercise>>
    suspend fun addCustomExercise(exercise: Exercise)
    suspend fun deleteCustomExercise(exercise: Exercise)
    fun getExerciseVariants(): Flow<Map<Int, List<String>>>
    suspend fun saveExerciseVariants(variants: Map<Int, List<String>>)
    suspend fun saveImportedExercises(exercises: List<Exercise>)
}