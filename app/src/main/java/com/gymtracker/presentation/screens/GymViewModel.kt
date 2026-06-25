package com.gymtracker.presentation.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymtracker.data.local.DataMigration
import com.gymtracker.data.local.ExerciseDefaults
import com.gymtracker.data.local.SeedData
import com.gymtracker.data.local.currentWeekDates
import com.gymtracker.data.local.currentWeekMonday
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.ExerciseTrend
import com.gymtracker.domain.model.ImportResult
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.TrendState
import com.gymtracker.domain.model.WeekStats
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.bestE1RM
import com.gymtracker.domain.model.bestHypertrophyScore
import com.gymtracker.domain.model.estimatedOneRM
import com.gymtracker.domain.repository.ExerciseRepository
import com.gymtracker.domain.repository.WorkoutRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class GymViewModel @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) : ViewModel() {

    var sets              = mutableStateListOf<WorkoutSet>();  private set
    var savedSessions     = mutableStateListOf<Session>();     private set
    var customExercises   = mutableStateListOf<Exercise>();    private set
    var importedExercises = mutableStateListOf<Exercise>();    private set

    val allExercises: List<Exercise>
        get() = SeedData.EXERCISES + importedExercises + customExercises

    var muscleFilter          by mutableStateOf("Todos")
    var routineFilter         by mutableStateOf("Todas")
    var filterTab             by mutableStateOf(0)
    var searchQuery           by mutableStateOf("")
    var progressRoutineFilter by mutableStateOf("Todas")

    val exerciseVariants = mutableStateMapOf<Int, List<String>>()

    fun variantsFor(exerciseId: Int): List<String> = exerciseVariants[exerciseId] ?: emptyList()
    fun hasVariants(exerciseId: Int): Boolean = exerciseVariants[exerciseId]?.isNotEmpty() == true

    fun setVariants(exerciseId: Int, variants: List<String>, context: Context) {
        if (variants.isEmpty()) exerciseVariants.remove(exerciseId)
        else exerciseVariants[exerciseId] = variants
        viewModelScope.launch {
            exerciseRepository.saveExerciseVariants(exerciseVariants.toMap())
        }
    }

    var sessionDate by mutableStateOf(LocalDate.now().toString())

    val filteredExercises: List<Exercise>
        get() {
            val byFilter = allExercises.filter { ex ->
                if (filterTab == 0) muscleFilter == "Todos" || ex.muscle == muscleFilter
                else routineFilter == "Todas" || ex.routine == routineFilter
            }
            return if (searchQuery.isBlank()) byFilter
            else byFilter.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }

    val progressExercises: List<Exercise>
        get() {
            val trained = allExercises.filter { ex ->
                savedSessions.any { s -> s.sets.any { it.exerciseId == ex.id } }
            }
            return if (progressRoutineFilter == "Todas") trained
            else trained.filter { it.routine == progressRoutineFilter }
        }

    fun setsFor(exerciseId: Int) = sets.filter { it.exerciseId == exerciseId }
    val totalReps   get() = sets.sumOf { it.reps }
    val totalVolume get() = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
    val groupedSets get() = sets.groupBy { it.exerciseName }
    val trainedDates: Set<String> get() = savedSessions.map { it.date }.toSet()

    fun knownVariantsFor(exerciseId: Int): List<String> {
        val fromHistory = savedSessions
            .flatMap { s -> s.sets.filter { it.exerciseId == exerciseId }.map { it.variant } }
        val fromCurrent = sets.filter { it.exerciseId == exerciseId }.map { it.variant }
        return (fromHistory + fromCurrent).filter { it.isNotBlank() }.distinct().sorted()
    }

    val weekStats: WeekStats
        get() {
            val weekDates    = currentWeekDates().toSet()
            val weekSessions = savedSessions.filter { it.date in weekDates }
            val allSets      = weekSessions.flatMap { it.sets }
            return WeekStats(
                daysTrainedThisWeek = weekSessions.map { it.date }.toSet().size,
                sessionsThisWeek    = weekSessions.size,
                setsThisWeek        = allSets.size,
                repsThisWeek        = allSets.sumOf { it.reps },
                volumeThisWeek      = allSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
            )
        }

    val weekDayIndicator: List<Pair<String, Boolean>>
        get() {
            val labels = listOf("L", "M", "X", "J", "V", "S", "D")
            val monday = currentWeekMonday()
            return (0..6).map { i ->
                val date = monday.plusDays(i.toLong()).toString()
                labels[i] to (date in trainedDates)
            }
        }

    fun logSet(exercise: Exercise, reps: Int, weightKg: Float, variant: String, note: String, context: Context) {
        sets.add(WorkoutSet(exercise.id, exercise.name, reps, weightKg, variant, note))
        viewModelScope.launch {
            workoutRepository.savePendingSession(sets.toList(), sessionDate)
        }
    }

    fun editSet(oldSet: WorkoutSet, newReps: Int, newWeightKg: Float, newVariant: String, newNote: String, context: Context) {
        val idx = sets.indexOf(oldSet); if (idx < 0) return
        sets[idx] = oldSet.copy(reps = newReps, weightKg = newWeightKg, variant = newVariant, note = newNote)
        viewModelScope.launch {
            workoutRepository.savePendingSession(sets.toList(), sessionDate)
        }
    }

    fun deleteSet(set: WorkoutSet, context: Context? = null) {
        sets.remove(set)
        viewModelScope.launch {
            if (sets.isEmpty()) workoutRepository.clearPendingSession()
            else workoutRepository.savePendingSession(sets.toList(), sessionDate)
        }
    }

    fun saveSession(context: Context) {
        if (sets.isEmpty()) return
        viewModelScope.launch {
            val existingSession = savedSessions.find { it.date == sessionDate }
            val newSession = if (existingSession != null) {
                Session(sessionDate, existingSession.sets + sets.toList())
            } else {
                Session(sessionDate, sets.toList())
            }
            workoutRepository.saveSession(newSession)
            workoutRepository.clearPendingSession()
            sets.clear()
            sessionDate = LocalDate.now().toString()
            loadSessions()
        }
    }

    fun loadAll(context: Context) {
        viewModelScope.launch {
            // Espera a que la migración termine si está en curso
            if (!DataMigration.isMigrationDone(context)) {
                kotlinx.coroutines.delay(500)
            }
            loadSessions()
            loadExercises()
            loadVariants()
            loadPendingSession()
        }
    }

    private fun loadSessions() {
        viewModelScope.launch {
            workoutRepository.getSessions().collect { sessions ->
                savedSessions.clear()
                savedSessions.addAll(sessions)
            }
        }
    }

    private fun loadExercises() {
        viewModelScope.launch {
            exerciseRepository.getCustomExercises().collect { exercises ->
                customExercises.clear()
                customExercises.addAll(exercises)
            }
        }
        viewModelScope.launch {
            exerciseRepository.getImportedExercises().collect { exercises ->
                importedExercises.clear()
                importedExercises.addAll(exercises)
            }
        }
    }

    private fun loadVariants() {
        viewModelScope.launch {
            exerciseRepository.getExerciseVariants().collect { variants ->
                exerciseVariants.clear()
                if (variants.isEmpty()) {
                    ExerciseDefaults.VARIANT_SUGGESTIONS_BY_EXERCISE
                        .forEach { (id, list) -> exerciseVariants[id] = list }
                    exerciseRepository.saveExerciseVariants(exerciseVariants.toMap())
                } else {
                    exerciseVariants.putAll(variants)
                }
            }
        }
    }

    private fun loadPendingSession() {
        viewModelScope.launch {
            workoutRepository.getPendingSession().collect { pending ->
                if (pending != null) {
                    sets.clear()
                    sets.addAll(pending.second)
                    sessionDate = pending.first
                }
            }
        }
    }

    fun discardPendingSession(context: Context) {
        sets.clear()
        sessionDate = LocalDate.now().toString()
        viewModelScope.launch {
            workoutRepository.clearPendingSession()
        }
    }

    fun deleteSetFromSession(date: String, set: WorkoutSet, context: Context) {
        viewModelScope.launch {
            workoutRepository.deleteSetFromSession(date, set)
            loadSessions()
        }
    }

    fun deleteSession(date: String, context: Context) {
        viewModelScope.launch {
            workoutRepository.deleteSession(date)
            loadSessions()
        }
    }

    fun addCustomExercise(ex: Exercise, context: Context) {
        viewModelScope.launch {
            exerciseRepository.addCustomExercise(ex)
        }
    }

    fun deleteCustomExercise(ex: Exercise, context: Context) {
        viewModelScope.launch {
            exerciseRepository.deleteCustomExercise(ex)
        }
    }

    fun nextCustomId() = (allExercises.maxOfOrNull { it.id } ?: 100) + 1

    fun importSessions(result: ImportResult.Success, context: Context) {
        viewModelScope.launch {
            result.newCustomExercises.forEach { newEx ->
                if (newEx.isCustom) {
                    if (customExercises.none { it.name.equals(newEx.name, ignoreCase = true) })
                        exerciseRepository.addCustomExercise(newEx)
                } else {
                    if (importedExercises.none { it.name.equals(newEx.name, ignoreCase = true) } &&
                        SeedData.EXERCISES.none { it.name.equals(newEx.name, ignoreCase = true) })
                        exerciseRepository.saveImportedExercises(listOf(newEx))
                }
            }
            result.mergedSessions.forEach { session ->
                workoutRepository.saveSession(session)
            }
            loadSessions()
            loadExercises()
        }
    }

    fun historyFor(exerciseId: Int): List<Pair<String, WorkoutSet>> =
        savedSessions
            .flatMap { s -> s.sets.filter { it.exerciseId == exerciseId }.map { s.date to it } }
            .sortedBy { it.first }

    fun historyForVariant(exerciseId: Int, variant: String): List<Pair<String, WorkoutSet>> {
        val all = historyFor(exerciseId)
        return if (variant.isBlank()) all else all.filter { it.second.variant == variant }
    }

    fun prFor(exerciseId: Int): Float =
        historyFor(exerciseId).maxOfOrNull { it.second.weightKg } ?: 0f

    fun allTimeE1RMFor(exerciseId: Int): Float =
        e1rmProgressionFor(exerciseId).maxOfOrNull { it.second } ?: 0f

    fun maxRepsFor(exerciseId: Int): Int =
        historyFor(exerciseId).maxOfOrNull { it.second.reps } ?: 0

    fun e1rmProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to bestE1RM(filtered)
            }.filter { it.second > 0f }

    fun hypertrophyProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to bestHypertrophyScore(filtered)
            }.filter { it.second > 0f }

    fun weightProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to (filtered.maxOfOrNull { it.weightKg } ?: 0f)
            }.filter { it.second > 0f }

    fun volumeProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to filtered.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
            }.filter { it.second > 0f }

    fun repsProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to filtered.sumOf { it.reps }.toFloat()
            }.filter { it.second > 0f }

    fun durationProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to filtered.sumOf { it.reps }.toFloat()
            }.filter { it.second > 0f }

    fun intensityProgressionFor(exerciseId: Int, variant: String = ""): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val filtered = s.sets.filter {
                    it.exerciseId == exerciseId && (variant.isBlank() || it.variant == variant)
                }
                s.date to (filtered.maxOfOrNull { it.weightKg } ?: 0f)
            }.filter { it.second > 0f }

    fun trendFor(exerciseId: Int, variant: String = ""): ExerciseTrend? {
        val ex = allExercises.find { it.id == exerciseId } ?: return null
        val metricData = when {
            ex.isCardio        -> durationProgressionFor(exerciseId, variant)
            ex.isStrengthFocus -> e1rmProgressionFor(exerciseId, variant)
            else               -> hypertrophyProgressionFor(exerciseId, variant)
        }
        if (metricData.isEmpty()) return null
        val allTime = metricData.maxOf { it.second }
        val last3   = metricData.takeLast(3)
        val latest  = last3.last().second
        val prev2   = last3.dropLast(1)
        val avg3    = if (prev2.isEmpty()) latest
        else prev2.map { it.second }.average().toFloat()
        val trend = when {
            last3.size >= 2 && latest > avg3 + 0.5f -> TrendState.PROGRESSING
            last3.size >= 3 && last3.zipWithNext().all { (a, b) -> b.second < a.second } -> TrendState.FATIGUE
            avg3 != 0f && ((latest - avg3) / avg3) * 100f < -3f -> TrendState.FATIGUE
            else -> TrendState.STAGNANT
        }
        val last5 = metricData.dropLast(1).takeLast(5)
        val avg5  = if (last5.isEmpty()) latest
        else last5.map { it.second }.average().toFloat()
        val pct5  = if (avg5 == 0f) 0f else ((latest - avg5) / avg5) * 100f
        return ExerciseTrend(ex, latest, avg3, pct5, trend, allTime, ex.isStrengthFocus)
    }

    val totalVolumeAllTime: Long
        get() = savedSessions.sumOf { s ->
            s.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
        }

    fun dominantRoutineForSession(date: String): String? {
        val session = savedSessions.find { it.date == date } ?: return null
        val routines = session.sets.mapNotNull { set ->
            allExercises.find { it.id == set.exerciseId }?.routine
        }
        return routines.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }


    fun exportToDownloads(context: Context): String? =
        com.gymtracker.data.local.Storage.exportToDownloads(
            context,
            savedSessions.toList(),
            allExercises
        )

    fun exportForShare(context: Context): android.net.Uri? =
        com.gymtracker.data.local.Storage.exportForShare(
            context,
            savedSessions.toList(),
            allExercises
        )

    fun importFromCsv(context: Context, uri: android.net.Uri): ImportResult =
        com.gymtracker.data.local.Storage.importFromCsv(
            context,
            uri,
            savedSessions.toList(),
            allExercises,
            allExercises.maxOfOrNull { it.id } ?: 100
        )
}
