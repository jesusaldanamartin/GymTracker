package com.gymtracker.domain.repository

import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow

interface WorkoutRepository {
    fun getSessions(): Flow<List<Session>>
    suspend fun saveSession(session: Session)
    suspend fun deleteSession(date: String)
    suspend fun deleteSetFromSession(date: String, set: WorkoutSet)
    fun getPendingSession(): Flow<Pair<String, List<WorkoutSet>>?>
    suspend fun savePendingSession(sets: List<WorkoutSet>, date: String)
    suspend fun clearPendingSession()
}