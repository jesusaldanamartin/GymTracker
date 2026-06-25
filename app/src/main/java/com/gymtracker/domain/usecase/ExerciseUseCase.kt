package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.repository.ExerciseRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCustomExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    operator fun invoke(): Flow<List<Exercise>> = repository.getCustomExercises()
}

class GetImportedExercisesUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    operator fun invoke(): Flow<List<Exercise>> = repository.getImportedExercises()
}

class AddCustomExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    suspend operator fun invoke(exercise: Exercise) =
        repository.addCustomExercise(exercise)
}

class DeleteCustomExerciseUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    suspend operator fun invoke(exercise: Exercise) =
        repository.deleteCustomExercise(exercise)
}

class GetExerciseVariantsUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    operator fun invoke(): Flow<Map<Int, List<String>>> =
        repository.getExerciseVariants()
}

class SaveExerciseVariantsUseCase @Inject constructor(
    private val repository: ExerciseRepository
) {
    suspend operator fun invoke(variants: Map<Int, List<String>>) =
        repository.saveExerciseVariants(variants)
}