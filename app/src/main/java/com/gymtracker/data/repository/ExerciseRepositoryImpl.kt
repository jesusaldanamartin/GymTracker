package com.gymtracker.data.repository

import com.gymtracker.data.local.db.ExerciseDao
import com.gymtracker.data.local.db.mapper.toDomain
import com.gymtracker.data.local.db.mapper.toEntity
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExerciseRepositoryImpl @Inject constructor(
    private val exerciseDao: ExerciseDao
) : ExerciseRepository {

    override fun getCustomExercises(): Flow<List<Exercise>> =
        exerciseDao.observeCustomExercises().map { entities ->
            entities.map { it.toDomain() }
        }

    override fun getImportedExercises(): Flow<List<Exercise>> =
        exerciseDao.observeImportedExercises().map { entities ->
            entities.map { it.toDomain() }
        }

    override suspend fun addCustomExercise(exercise: Exercise) {
        exerciseDao.insertExercise(exercise.toEntity())
    }

    override suspend fun deleteCustomExercise(exercise: Exercise) {
        exerciseDao.deleteExercise(exercise.id)
    }

    override fun getExerciseVariants(): Flow<Map<Int, List<String>>> =
        kotlinx.coroutines.flow.flow { emit(emptyMap()) }
    // Se implementará con DataStore en Fase 10

    override suspend fun saveExerciseVariants(variants: Map<Int, List<String>>) {
        // Se implementará con DataStore en Fase 10
    }

    override suspend fun saveImportedExercises(exercises: List<Exercise>) {
        exerciseDao.insertExercises(exercises.map { it.toEntity() })
    }
}