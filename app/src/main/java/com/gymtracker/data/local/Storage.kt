package com.gymtracker.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.gymtracker.data.local.ExerciseDefaults
import com.gymtracker.data.local.SeedData
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.ImportResult
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.estimatedOneRM
import androidx.compose.ui.graphics.Color
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

object Storage {
    private const val PREFS            = "gym_data"
    private const val KEY_SESSIONS     = "sessions_v4"
    private const val KEY_CUSTOM_EX    = "custom_exercises"
    private const val KEY_PENDING_SETS = "pending_sets_v4"
    private const val KEY_PENDING_DATE = "pending_date"
    private const val KEY_IMPORTED_EX  = "imported_exercises"
    private const val KEY_EX_VARIANTS  = "exercise_variants"

    fun saveImportedExercises(context: Context, list: List<Exercise>) {
        val arr = JSONArray()
        list.filter { !it.isCustom }.forEach { ex ->
            arr.put(JSONObject().apply {
                put("id", ex.id); put("name", ex.name); put("muscle", ex.muscle)
                put("routine", ex.routine); put("emoji", ex.emoji)
                put("strength", ex.isStrengthFocus); put("cardio", ex.isCardio)
            })
        }
        prefs(context).edit().putString(KEY_IMPORTED_EX, arr.toString()).apply()
    }

    fun loadImportedExercises(context: Context): List<Exercise> {
        val raw = prefs(context).getString(KEY_IMPORTED_EX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o      = arr.getJSONObject(i)
                val muscle = o.getString("muscle")
                Exercise(
                    o.getInt("id"), o.getString("name"), muscle, o.getString("routine"),
                    o.getString("emoji"),
                    ExerciseDefaults.MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93),
                    o.getBoolean("strength"), isCustom = false,
                    isCardio = o.optBoolean("cardio", false)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun save(context: Context, sessions: List<Session>) {
        val arr = JSONArray()
        for (s in sessions) {
            val setsArr = JSONArray()
            for (ws in s.sets) setsArr.put(JSONObject().apply {
                put("eid", ws.exerciseId); put("ename", ws.exerciseName)
                put("reps", ws.reps); put("weight", ws.weightKg.toDouble())
                put("variant", ws.variant); put("note", ws.note)
            })
            arr.put(JSONObject().apply { put("date", s.date); put("sets", setsArr) })
        }
        prefs(context).edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun load(context: Context): List<Session> {
        val raw = prefs(context).getString(KEY_SESSIONS, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val obj  = arr.getJSONObject(i)
                val sArr = obj.getJSONArray("sets")
                val sets = (0 until sArr.length()).map { j ->
                    val ws = sArr.getJSONObject(j)
                    WorkoutSet(
                        ws.getInt("eid"), ws.getString("ename"),
                        ws.getInt("reps"), ws.getDouble("weight").toFloat(),
                        ws.optString("variant", ""), ws.optString("note", "")
                    )
                }
                Session(obj.getString("date"), sets)
            }
        } catch (e: Exception) { emptyList() }
    }

    fun savePendingSession(context: Context, sets: List<WorkoutSet>, date: String) {
        val arr = JSONArray()
        sets.forEach { ws ->
            arr.put(JSONObject().apply {
                put("eid", ws.exerciseId); put("ename", ws.exerciseName)
                put("reps", ws.reps); put("weight", ws.weightKg.toDouble())
                put("variant", ws.variant); put("note", ws.note)
            })
        }
        prefs(context).edit()
            .putString(KEY_PENDING_SETS, arr.toString())
            .putString(KEY_PENDING_DATE, date)
            .apply()
    }

    fun loadPendingSession(context: Context): Pair<String, List<WorkoutSet>>? {
        val raw  = prefs(context).getString(KEY_PENDING_SETS, null) ?: return null
        val date = prefs(context).getString(KEY_PENDING_DATE, null) ?: return null
        return try {
            val arr  = JSONArray(raw)
            val sets = (0 until arr.length()).map { i ->
                val ws = arr.getJSONObject(i)
                WorkoutSet(
                    ws.getInt("eid"), ws.getString("ename"),
                    ws.getInt("reps"), ws.getDouble("weight").toFloat(),
                    ws.optString("variant", ""), ws.optString("note", "")
                )
            }
            if (sets.isEmpty()) null else date to sets
        } catch (e: Exception) { null }
    }

    fun clearPendingSession(context: Context) {
        prefs(context).edit()
            .remove(KEY_PENDING_SETS)
            .remove(KEY_PENDING_DATE)
            .apply()
    }

    fun saveCustomExercises(context: Context, list: List<Exercise>) {
        val arr = JSONArray()
        list.filter { it.isCustom }.forEach { ex ->
            arr.put(JSONObject().apply {
                put("id", ex.id); put("name", ex.name); put("muscle", ex.muscle)
                put("routine", ex.routine); put("emoji", ex.emoji)
                put("strength", ex.isStrengthFocus); put("cardio", ex.isCardio)
            })
        }
        prefs(context).edit().putString(KEY_CUSTOM_EX, arr.toString()).apply()
    }

    fun loadCustomExercises(context: Context): List<Exercise> {
        val raw = prefs(context).getString(KEY_CUSTOM_EX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o      = arr.getJSONObject(i)
                val muscle = o.getString("muscle")
                Exercise(
                    o.getInt("id"), o.getString("name"), muscle, o.getString("routine"),
                    o.getString("emoji"),
                    ExerciseDefaults.MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93),
                    o.getBoolean("strength"), isCustom = true,
                    isCardio = o.optBoolean("cardio", false)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun exportToDownloads(context: Context, sessions: List<Session>, allEx: List<Exercise>): String? = try {
        val name = "gymtracker_${LocalDate.now()}.csv"
        val csv  = buildCSV(sessions, allEx)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            uri?.let { context.contentResolver.openOutputStream(it)?.use { os -> os.write(csv.toByteArray()) } }
            name
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs(); File(dir, name).writeText(csv); name
        }
    } catch (e: Exception) { null }

    fun exportForShare(context: Context, sessions: List<Session>, allEx: List<Exercise>): android.net.Uri? = try {
        val file = File(context.cacheDir, "gymtracker_export.csv")
        file.writeText(buildCSV(sessions, allEx))
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    } catch (e: Exception) { null }

    private fun buildCSV(sessions: List<Session>, allEx: List<Exercise>): String {
        val sb = StringBuilder()
        sb.appendLine("fecha,ejercicio,musculo,rutina,tipo,variante,serie,reps,peso_kg,e1rm,score_hipertrofia,nota")
        sessions.sortedBy { it.date }.forEach { s ->
            s.sets.groupBy { it.exerciseName }.forEach { (_, exSets) ->
                exSets.forEachIndexed { idx, set ->
                    val ex   = allEx.find { it.id == set.exerciseId }
                    val tipo = when {
                        ex?.isCardio == true        -> "cardio"
                        ex?.isStrengthFocus == true -> "fuerza"
                        else                        -> "hipertrofia"
                    }
                    val e1rm = if (ex?.isStrengthFocus == true && ex.isCardio != true)
                        "%.1f".format(estimatedOneRM(set.weightKg, set.reps)) else ""
                    val hy   = if (ex?.isStrengthFocus == false && ex?.isCardio != true)
                        "%.1f".format(set.reps * set.weightKg) else ""
                    val note = set.note.replace("\"", "\"\"")
                    sb.appendLine("${s.date},\"${set.exerciseName}\",${ex?.muscle ?: ""},${ex?.routine ?: ""},$tipo,\"${set.variant}\",${idx + 1},${set.reps},${set.weightKg},$e1rm,$hy,\"$note\"")
                }
            }
        }
        return sb.toString()
    }

    fun saveExerciseVariants(context: Context, map: Map<Int, List<String>>) {
        val obj = JSONObject()
        map.forEach { (id, list) ->
            val arr = JSONArray(); list.forEach { arr.put(it) }
            obj.put(id.toString(), arr)
        }
        prefs(context).edit().putString(KEY_EX_VARIANTS, obj.toString()).apply()
    }

    fun loadExerciseVariants(context: Context): Map<Int, List<String>> {
        val raw = prefs(context).getString(KEY_EX_VARIANTS, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key ->
                    val arr = obj.getJSONArray(key)
                    put(key.toInt(), (0 until arr.length()).map { arr.getString(it) })
                }
            }
        } catch (e: Exception) { emptyMap() }
    }

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun importFromCsv(
        context: Context,
        uri: android.net.Uri,
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
                val cols = com.gymtracker.data.local.parseCsvLine(line)
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
                            ?: androidx.compose.ui.graphics.Color(0xFF8E8E93)
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
                mergedSessions     = finalSessions,
                newSessions        = newSessions,
                updatedSessions    = mergedSessions,
                newSets            = newSets,
                skippedRows        = skipped,
                newCustomExercises = newCustomExercises
            )
        } catch (e: Exception) {
            ImportResult.Error("Error al procesar el archivo: ${e.message}")
        }
    }
}

private fun parseCsvLine(line: String): List<String> {
    val result   = mutableListOf<String>()
    var inQuotes = false
    val current  = StringBuilder()
    for (char in line) {
        when {
            char == '"'              -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> { result.add(current.toString()); current.clear() }
            else                     -> current.append(char)
        }
    }
    result.add(current.toString())
    return result
}