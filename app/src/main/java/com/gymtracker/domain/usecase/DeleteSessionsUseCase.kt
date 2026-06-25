package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.repository.WorkoutRepository
import javax.inject.Inject

class DeleteSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(date: String) = repository.deleteSession(date)
}

class DeleteSetFromSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(date: String, set: WorkoutSet) =
        repository.deleteSetFromSession(date, set)
}