package com.gymtracker.domain.usecase

import android.content.Context
import android.net.Uri
import com.gymtracker.data.local.ExerciseDefaults
import com.gymtracker.data.local.SeedData
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.ImportResult
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.repository.ExerciseRepository
import com.gymtracker.domain.repository.WorkoutRepository
import java.time.LocalDate
import javax.inject.Inject
import androidx.compose.ui.graphics.Color

class ImportCsvUseCase @Inject constructor(
    private val workoutRepository: WorkoutRepository,
    private val exerciseRepository: ExerciseRepository
) {
    suspend operator fun invoke(
        context: Context,
        uri: Uri,
        existingSessions: List<Session>,
        allExercises: List<Exercise>,
        maxExistingId: Int
    ): ImportResult {
        return try {
            val lines = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.readLines()
                ?: return ImportResult.Error("No se pudo leer el archivo")

            if (lines.size < 2)
                return ImportResult.Error("El archivo está vacío o no tiene datos")

            val header = lines.first().trim().lowercase()
            if (!header.startsWith("fecha,ejercicio"))
                return ImportResult.Error("Formato no válido.\nAsegúrate de importar un CSV exportado por GymTracker.")

            val hasVariant = header.contains("variante")
            val hasNote    = header.contains("nota")

            val knownByName = allExercises.associateBy { it.name.lowercase() }.toMutableMap()
            val newCustomExercises = mutableListOf<Exercise>()
            var nextId = maxExistingId + 1

            val parsed  = mutableMapOf<String, MutableList<WorkoutSet>>()
            var skipped = 0

            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = parseCsvLine(line)
                if (cols.size < 8) { skipped++; return@forEach }
                try {
                    val date    = cols[0].trim()
                    val exName  = cols[1].trim().removeSurrounding("\"")
                    val muscle  = cols[2].trim().ifBlank { "Core" }
                    val routine = cols[3].trim().ifBlank { "Full" }
                    val tipo    = cols[4].trim().lowercase()

                    val variantCol: String
                    val serieIdx: Int
                    if (hasVariant) {
                        variantCol = cols.getOrElse(5) { "" }.trim().removeSurrounding("\"")
                        serieIdx   = 6
                    } else {
                        variantCol = ""
                        serieIdx   = 5
                    }

                    val reps     = cols.getOrNull(serieIdx + 1)?.trim()?.toIntOrNull()
                        ?: run { skipped++; return@forEach }
                    val weightKg = cols.getOrNull(serieIdx + 2)?.trim()?.toFloatOrNull()
                        ?: run { skipped++; return@forEach }
                    val noteCol  = if (hasNote)
                        cols.lastOrNull()?.trim()?.removeSurrounding("\"") ?: "" else ""

                    LocalDate.parse(date)

                    val exercise = knownByName[exName.lowercase()] ?: run {
                        val seedMatch = SeedData.EXERCISES
                            .find { it.name.equals(exName, ignoreCase = true) }
                        if (seedMatch != null) {
                            knownByName[exName.lowercase()] = seedMatch
                            return@run seedMatch
                        }
                        val isStrength = tipo == "fuerza"
                        val isCardio   = tipo == "cardio"
                        val color      = ExerciseDefaults.MUSCLE_COLORS[muscle]
                            ?: Color(0xFF8E8E93)
                        val emoji = when (muscle) {
                            "Pecho"   -> "💪"; "Hombros" -> "🏋️"
                            "Triceps" -> "💪"; "Espalda" -> "🔙"
                            "Biceps"  -> "💪"; "Piernas" -> "🦵"
                            "Gluteos" -> "🍑"; "Core"    -> "🎯"
                            "Cardio"  -> "❤️"; else      -> "🎯"
                        }
                        val newEx = Exercise(
                            id              = nextId++,
                            name            = exName,
                            muscle          = muscle,
                            routine         = routine,
                            emoji           = emoji,
                            color           = color,
                            isStrengthFocus = isStrength,
                            isCustom        = false,
                            isCardio        = isCardio
                        )
                        knownByName[exName.lowercase()] = newEx
                        newCustomExercises.add(newEx)
                        newEx
                    }

                    parsed.getOrPut(date) { mutableListOf() }
                        .add(WorkoutSet(exercise.id, exercise.name, reps, weightKg, variantCol, noteCol))

                } catch (e: Exception) { skipped++ }
            }

            if (parsed.isEmpty())
                return ImportResult.Error("No se encontraron sesiones válidas en el archivo")

            val existingMap    = existingSessions.associateBy { it.date }.toMutableMap()
            var newSessions    = 0
            var mergedSessions = 0
            var newSets        = 0

            parsed.forEach { (date, importedSets) ->
                val existing = existingMap[date]
                if (existing == null) {
                    existingMap[date] = Session(date, importedSets)
                    newSessions++
                    newSets += importedSets.size
                } else {
                    val existingSets = existing.sets.toMutableList()
                    val toAdd = importedSets.filter { imp ->
                        existingSets.none { ex ->
                            ex.exerciseName == imp.exerciseName &&
                                    ex.reps == imp.reps &&
                                    ex.weightKg == imp.weightKg &&
                                    ex.variant == imp.variant
                        }
                    }
                    if (toAdd.isNotEmpty()) {
                        existingMap[date] = Session(date, existingSets + toAdd)
                        mergedSessions++
                        newSets += toAdd.size
                    }
                }
            }

            val finalSessions = existingMap.values.sortedBy { it.date }
            ImportResult.Success(
                mergedSessions      = finalSessions,
                newSessions         = newSessions,
                updatedSessions     = mergedSessions,
                newSets             = newSets,
                skippedRows         = skipped,
                newCustomExercises  = newCustomExercises
            )
        } catch (e: Exception) {
            ImportResult.Error("Error al procesar el archivo: ${e.message}")
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result   = mutableListOf<String>()
        var inQuotes = false
        val current  = StringBuilder()
        for (char in line) {
            when {
                char == '"'               -> inQuotes = !inQuotes
                char == ',' && !inQuotes  -> { result.add(current.toString()); current.clear() }
                else                      -> current.append(char)
            }
        }
        result.add(current.toString())
        return result
    }
}