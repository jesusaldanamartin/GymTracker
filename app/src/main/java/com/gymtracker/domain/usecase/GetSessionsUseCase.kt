package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.Session
import com.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSessionsUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    operator fun invoke(): Flow<List<Session>> = repository.getSessions()
}