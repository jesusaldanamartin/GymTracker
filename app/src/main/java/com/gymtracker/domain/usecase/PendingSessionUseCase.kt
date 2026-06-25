package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPendingSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    operator fun invoke(): Flow<Pair<String, List<WorkoutSet>>?> =
        repository.getPendingSession()
}

class SavePendingSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(sets: List<WorkoutSet>, date: String) =
        repository.savePendingSession(sets, date)
}

class ClearPendingSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke() = repository.clearPendingSession()
}