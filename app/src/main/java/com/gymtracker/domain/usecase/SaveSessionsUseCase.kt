package com.gymtracker.domain.usecase

import com.gymtracker.domain.model.Session
import com.gymtracker.domain.repository.WorkoutRepository
import javax.inject.Inject

class SaveSessionUseCase @Inject constructor(
    private val repository: WorkoutRepository
) {
    suspend operator fun invoke(session: Session) = repository.saveSession(session)
}