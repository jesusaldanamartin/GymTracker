package com.gymtracker.presentation.screens

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
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
import java.time.LocalDate
import com.gymtracker.data.local.Storage

class GymViewModel : ViewModel() {

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
        Storage.saveExerciseVariants(context, exerciseVariants.toMap())
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
            val weekDates = currentWeekDates().toSet()
            val weekSessions = savedSessions.filter { it.date in weekDates }
            val allSets = weekSessions.flatMap { it.sets }
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
        Storage.savePendingSession(context, sets.toList(), sessionDate)
    }

    fun editSet(oldSet: WorkoutSet, newReps: Int, newWeightKg: Float, newVariant: String, newNote: String, context: Context) {
        val idx = sets.indexOf(oldSet); if (idx < 0) return
        sets[idx] = oldSet.copy(reps = newReps, weightKg = newWeightKg, variant = newVariant, note = newNote)
        Storage.savePendingSession(context, sets.toList(), sessionDate)
    }

    fun deleteSet(set: WorkoutSet, context: Context? = null) {
        sets.remove(set)
        if (context != null) {
            if (sets.isEmpty()) Storage.clearPendingSession(context)
            else Storage.savePendingSession(context, sets.toList(), sessionDate)
        }
    }

    fun saveSession(context: Context) {
        if (sets.isEmpty()) return
        val idx = savedSessions.indexOfFirst { it.date == sessionDate }
        if (idx >= 0) savedSessions[idx] = Session(sessionDate, savedSessions[idx].sets + sets.toList())
        else savedSessions.add(Session(sessionDate, sets.toList()))
        Storage.save(context, savedSessions.toList())
        Storage.clearPendingSession(context)
        sets.clear(); sessionDate = LocalDate.now().toString()
    }

    fun loadAll(context: Context) {
        savedSessions.clear()
        savedSessions.addAll(Storage.load(context))
        customExercises.clear()
        customExercises.addAll(Storage.loadCustomExercises(context))
        importedExercises.clear()
        importedExercises.addAll(Storage.loadImportedExercises(context))

        val savedVariants = Storage.loadExerciseVariants(context)
        exerciseVariants.clear()
        if (savedVariants.isEmpty()) {
            ExerciseDefaults.VARIANT_SUGGESTIONS_BY_EXERCISE
                .forEach { (id, list) -> exerciseVariants[id] = list }
            Storage.saveExerciseVariants(context, exerciseVariants.toMap())
        } else {
            exerciseVariants.putAll(savedVariants)
        }

        Storage.loadPendingSession(context)?.let { (date, pendingSets) ->
            sets.clear(); sets.addAll(pendingSets); sessionDate = date
        }
    }

    fun discardPendingSession(context: Context) {
        sets.clear(); sessionDate = LocalDate.now().toString()
        Storage.clearPendingSession(context)
    }

    fun deleteSetFromSession(date: String, set: WorkoutSet, context: Context) {
        val idx = savedSessions.indexOfFirst { it.date == date }; if (idx < 0) return
        val rem = savedSessions[idx].sets.toMutableList().also { it.remove(set) }
        if (rem.isEmpty()) savedSessions.removeAt(idx)
        else savedSessions[idx] = Session(date, rem)
        Storage.save(context, savedSessions.toList())
    }

    fun deleteSession(date: String, context: Context) {
        savedSessions.removeAll { it.date == date }
        Storage.save(context, savedSessions.toList())
    }

    fun addCustomExercise(ex: Exercise, context: Context) {
        customExercises.add(ex)
        Storage.saveCustomExercises(context, customExercises.toList())
    }

    fun deleteCustomExercise(ex: Exercise, context: Context) {
        customExercises.remove(ex)
        Storage.saveCustomExercises(context, customExercises.toList())
    }

    fun nextCustomId() = (allExercises.maxOfOrNull { it.id } ?: 100) + 1

    fun importSessions(result: ImportResult.Success, context: Context) {
        result.newCustomExercises.forEach { newEx ->
            if (newEx.isCustom) {
                if (customExercises.none { it.name.equals(newEx.name, ignoreCase = true) })
                    customExercises.add(newEx)
            } else {
                if (importedExercises.none { it.name.equals(newEx.name, ignoreCase = true) } &&
                    SeedData.EXERCISES.none { it.name.equals(newEx.name, ignoreCase = true) })
                    importedExercises.add(newEx)
            }
        }
        if (result.newCustomExercises.any { it.isCustom })
            Storage.saveCustomExercises(context, customExercises.toList())
        if (result.newCustomExercises.any { !it.isCustom })
            Storage.saveImportedExercises(context, importedExercises.toList())
        savedSessions.clear()
        savedSessions.addAll(result.mergedSessions)
        Storage.save(context, savedSessions.toList())
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
}