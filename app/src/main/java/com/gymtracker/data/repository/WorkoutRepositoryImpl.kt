package com.gymtracker.data.repository

import com.gymtracker.data.local.db.SessionDao
import com.gymtracker.data.local.db.SessionEntity
import com.gymtracker.data.local.db.WorkoutSetDao
import com.gymtracker.data.local.db.mapper.toDomain
import com.gymtracker.data.local.db.mapper.toEntity
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.repository.WorkoutRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkoutRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao,
    private val workoutSetDao: WorkoutSetDao
) : WorkoutRepository {

    override fun getSessions(): Flow<List<Session>> =
        combine(
            sessionDao.observeAll(),
            workoutSetDao.observeAll()
        ) { sessions, allSets ->
            sessions.map { sessionEntity ->
                val setsForSession = allSets.filter { it.sessionDate == sessionEntity.date }
                sessionEntity.toDomain(setsForSession)
            }
        }

    override suspend fun saveSession(session: Session) {
        sessionDao.insertSession(SessionEntity(session.date))
        val entities = session.sets.map { it.toEntity(session.date) }
        workoutSetDao.insertSets(entities)
    }

    override suspend fun deleteSession(date: String) {
        sessionDao.deleteSession(date)
    }

    override suspend fun deleteSetFromSession(date: String, set: WorkoutSet) {
        workoutSetDao.deleteSpecificSet(
            date        = date,
            exerciseId  = set.exerciseId,
            reps        = set.reps,
            weightKg    = set.weightKg,
            variant     = set.variant
        )
        // Si no quedan sets, borra la sesión también
        val remaining = workoutSetDao.observeSetsForDate(date)
        remaining.collect { sets ->
            if (sets.isEmpty()) sessionDao.deleteSession(date)
            return@collect
        }
    }

    // Pending session usando DataStore — por ahora usamos un Flow en memoria
    // Se reemplazará por DataStore en la Fase 10
    private var _pendingSession: Pair<String, List<WorkoutSet>>? = null

    override fun getPendingSession(): Flow<Pair<String, List<WorkoutSet>>?> =
        kotlinx.coroutines.flow.flow { emit(_pendingSession) }

    override suspend fun savePendingSession(sets: List<WorkoutSet>, date: String) {
        _pendingSession = date to sets
    }

    override suspend fun clearPendingSession() {
        _pendingSession = null
    }
}