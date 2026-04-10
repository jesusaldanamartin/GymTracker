package com.gymtracker

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// MODELS
// ─────────────────────────────────────────────────────────────────────────────

data class Exercise(
    val id: Int,
    val name: String,
    val muscle: String,
    val routine: String,
    val emoji: String,
    val color: Color,
    val isStrengthFocus: Boolean = false,
    val isCustom: Boolean = false
)

data class WorkoutSet(
    val exerciseId: Int,
    val exerciseName: String,
    val reps: Int,
    val weightKg: Float
)

data class Session(
    val date: String,
    val sets: List<WorkoutSet>
)

fun estimatedOneRM(weightKg: Float, reps: Int): Float {
    if (weightKg == 0f || reps == 0) return 0f
    return weightKg * (1f + reps / 30f)
}

fun bestE1RM(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { estimatedOneRM(it.weightKg, it.reps) } ?: 0f

fun bestHypertrophyScore(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { it.reps * it.weightKg } ?: 0f

// ─────────────────────────────────────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────────────────────────────────────

val Black        = Color(0xFF000000)
val Surface0     = Color(0xFF0A0A0A)
val Surface1     = Color(0xFF141414)
val Surface2     = Color(0xFF1E1E20)
val Surface3     = Color(0xFF28282C)
val Border       = Color(0xFF2C2C2E)
val BorderLight  = Color(0xFF3A3A3C)
val Accent       = Color(0xFFE8FF47)
val AccentMuted  = Color(0xFF9CAA2E)
val TextPrim     = Color(0xFFF2F2F7)
val TextSec      = Color(0xFF8E8E93)
val TextTert     = Color(0xFF48484A)
val GreenOk      = Color(0xFF30D158)
val YellowWarn   = Color(0xFFFFD60A)
val RedBad       = Color(0xFFFF453A)
val OrangeStk    = Color(0xFFFF9500)
val Blue         = Color(0xFF0A84FF)

val MUSCLE_COLORS = mapOf(
    "Pecho"    to Color(0xFFFD2D87),
    "Hombros"  to Color(0xFFFFBE0B),
    "Triceps"  to Color(0xFF8338EC),
    "Espalda"  to Color(0xFF4ECDC4),
    "Biceps"   to Color(0xFF3A86FF),
    "Piernas"  to Color(0xFFFF006E),
    "Gluteos"  to Color(0xFFFB5607),
    "Core"     to Color(0xFF06D6A0),
    "Cardio"   to Color(0xFFE63946)
)

// Colores por rutina — usados en el calendario
val ROUTINE_COLORS = mapOf(
    "Push"   to Color(0xFFFD2D87),  // Rosa
    "Pull"   to Color(0xFF4ECDC4),  // Teal
    "Legs"   to Color(0xFFFF9500),  // Naranja
    "Full"   to Color(0xFF06D6A0),  // Verde
    "Cardio" to Color(0xFFE63946),  // Rojo
)

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISE IMAGES
//
// Cómo añadir imágenes propias:
// 1. Copia tus fotos/PNGs/JPGs a:  app/src/main/res/drawable/
//    Usa nombres en minúsculas sin espacios, p.ej: press_banca.png
// 2. Añade la entrada aquí con el R.drawable correspondiente.
//
// Prioridad: drawable local → URL remota → emoji (fallback final)
//
// Formatos soportados en drawable: PNG, JPG, WEBP, GIF
// Tamaño recomendado: 300×300 px o más, fondo blanco o transparente.
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Imagen local desde res/drawable.
 * Clave: exerciseId. Valor: R.drawable.nombre_del_archivo
 *
 * Ejemplo:
 *   1  to R.drawable.press_banca,
 *   18 to R.drawable.sentadilla,
 */
val EXERCISE_DRAWABLES: Map<Int, Int> = mapOf(
    // ── Añade aquí tus imágenes locales ──────────────────────────────────────
    // 1  to R.drawable.bench_press,
    // 12  to R.drawable.dorsales,
    // 34  to R.drawable.dorsales,
    // 10 to R.drawable.dominadas,
    // 18 to R.drawable.sentadilla,
    // 22 to R.drawable.hip_thrust,
    // 28 to R.drawable.cinta_correr,
    // ─────────────────────────────────────────────────────────────────────────
)

/**
 * URL remota como segunda opción (wger, imgur, etc.)
 * Solo se usa si el ejercicio NO tiene entrada en EXERCISE_DRAWABLES.
 *
 * Para wger: https://wger.de/en/exercise/overview/
 */
val EXERCISE_REMOTE_URLS: Map<Int, String> = mapOf(
    // ── Añade aquí URLs remotas ───────────────────────────────────────────────
    // 1  to "https://wger.de/static/images/exercises/bench-press.png",
    // 18 to "https://wger.de/static/images/exercises/squat.png",
    // ─────────────────────────────────────────────────────────────────────────
)

// ─────────────────────────────────────────────────────────────────────────────
// SEED DATA
// ─────────────────────────────────────────────────────────────────────────────

val SEED_EXERCISES = listOf(
    Exercise(1,  "Press Banca",               "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
    Exercise(2,  "Press Inclinado",           "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
    Exercise(42, "Press Inclinado Manc.",     "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), true),
    Exercise(31, "Aperturas Cable",           "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
    Exercise(3,  "Peck Deck",                 "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
    Exercise(4,  "Fondos",                    "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
    Exercise(5,  "Press Militar",             "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B), true),
    Exercise(6,  "Elevaciones Lat.",          "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B)),
    Exercise(7,  "Reversed Peck Deck",        "Hombros", "Pull",   "🏋️", Color(0xFFFFBE0B)),
    Exercise(17, "Facepull",                  "Hombros", "Pull",   "🎯", Color(0xFFFFBE0B)),
    Exercise(8,  "Extensiones Triceps",       "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(9,  "Extensiones Katana",        "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(32, "Extensiones Unilateral",    "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(33, "Extensiones sobre cabeza",  "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(10, "Dominadas",                 "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(11, "Remo en T",                 "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(12, "Jalón al Pecho",            "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(13, "Remo Máquina Unilateral",   "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(34, "Remo Polea Unilateral",     "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(35, "Pull Over",                 "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(14, "Curl Bíceps Unilateral",    "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(15, "Curl Martillo",             "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(16, "Curl Bayesian",             "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(36, "Curl Predicador",           "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(18, "Sentadilla Libre",          "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
    Exercise(23, "Sentadilla MultiPower",     "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
    Exercise(37, "Peso Muerto",               "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
    Exercise(38, "Peso Muerto Sumo",          "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
    Exercise(39, "Peso Muerto Rumano",        "Piernas", "Legs",   "🦵", Color(0xFFFF006E), true),
    Exercise(19, "Prensa de Piernas",         "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(20, "Extensiones Cuad.",         "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(21, "Curl Femoral",              "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(22, "Hip Thrust",                "Gluteos", "Legs",   "🍑", Color(0xFFFB5607)),
    Exercise(24, "Gemelos de Pie",            "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(40, "Aducciones",                "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(41, "Abducciones",               "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(25, "Elevaciones de piernas",    "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(26, "Crunch Polea",              "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(27, "Rueda Abdominal",           "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(28, "Cinta de Correr",           "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(29, "Bicicleta Est.",            "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(30, "Remo Ergómetro",            "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(43, "Máquina de Escalera",       "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
)

val MUSCLES  = listOf("Todos","Pecho","Hombros","Triceps","Espalda","Biceps","Piernas","Gluteos","Core","Cardio")
val ROUTINES = listOf("Todas","Push","Pull","Legs","Full","Cardio")

// ─────────────────────────────────────────────────────────────────────────────
// WEEK HELPERS
// ─────────────────────────────────────────────────────────────────────────────

fun currentWeekMonday(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun currentWeekSunday(): LocalDate =
    LocalDate.now().with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

fun currentWeekDates(): List<String> {
    val monday = currentWeekMonday()
    return (0..6).map { monday.plusDays(it.toLong()).toString() }
}

data class WeekStats(
    val daysTrainedThisWeek: Int,
    val sessionsThisWeek: Int,
    val setsThisWeek: Int,
    val repsThisWeek: Int,
    val volumeThisWeek: Long
)

// ─────────────────────────────────────────────────────────────────────────────
// IMPORT RESULT
// ─────────────────────────────────────────────────────────────────────────────

sealed class ImportResult {
    data class Success(
        val mergedSessions: List<Session>,
        val newSessions: Int,
        val updatedSessions: Int,
        val newSets: Int,
        val skippedRows: Int,
        val newCustomExercises: List<Exercise> = emptyList()
    ) : ImportResult()
    data class Error(val message: String) : ImportResult()
}

// ─────────────────────────────────────────────────────────────────────────────
// PERSISTENCE
// ─────────────────────────────────────────────────────────────────────────────

object Storage {
    private const val PREFS            = "gym_data"
    private const val KEY_SESSIONS     = "sessions_v3"
    private const val KEY_CUSTOM_EX    = "custom_exercises"
    private const val KEY_PENDING_SETS = "pending_sets"
    private const val KEY_PENDING_DATE = "pending_date"

    fun save(context: Context, sessions: List<Session>) {
        val arr = JSONArray()
        for (s in sessions) {
            val setsArr = JSONArray()
            for (ws in s.sets) setsArr.put(JSONObject().apply {
                put("eid", ws.exerciseId); put("ename", ws.exerciseName)
                put("reps", ws.reps); put("weight", ws.weightKg.toDouble())
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
                    WorkoutSet(ws.getInt("eid"), ws.getString("ename"),
                        ws.getInt("reps"), ws.getDouble("weight").toFloat())
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
                WorkoutSet(ws.getInt("eid"), ws.getString("ename"),
                    ws.getInt("reps"), ws.getDouble("weight").toFloat())
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
                put("routine", ex.routine); put("emoji", ex.emoji); put("strength", ex.isStrengthFocus)
            })
        }
        prefs(context).edit().putString(KEY_CUSTOM_EX, arr.toString()).apply()
    }

    fun loadCustomExercises(context: Context): List<Exercise> {
        val raw = prefs(context).getString(KEY_CUSTOM_EX, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i); val muscle = o.getString("muscle")
                Exercise(o.getInt("id"), o.getString("name"), muscle, o.getString("routine"),
                    o.getString("emoji"), MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93),
                    o.getBoolean("strength"), isCustom = true)
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
        sb.appendLine("fecha,ejercicio,musculo,rutina,tipo,serie,reps,peso_kg,e1rm,score_hipertrofia")
        sessions.sortedBy { it.date }.forEach { s ->
            s.sets.groupBy { it.exerciseName }.forEach { (_, exSets) ->
                exSets.forEachIndexed { idx, set ->
                    val ex   = allEx.find { it.id == set.exerciseId }
                    val e1rm = if (ex?.isStrengthFocus == true) "%.1f".format(estimatedOneRM(set.weightKg, set.reps)) else ""
                    val hy   = if (ex?.isStrengthFocus == false) "%.1f".format(set.reps * set.weightKg) else ""
                    val tipo = if (ex?.isStrengthFocus == true) "fuerza" else "hipertrofia"
                    sb.appendLine("${s.date},\"${set.exerciseName}\",${ex?.muscle ?: ""},${ex?.routine ?: ""},$tipo,${idx+1},${set.reps},${set.weightKg},$e1rm,$hy")
                }
            }
        }
        return sb.toString()
    }

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

            val knownByName = allExercises.associateBy { it.name.lowercase() }.toMutableMap()
            val newCustomExercises = mutableListOf<Exercise>()
            var nextId = maxExistingId + 1

            val parsed = mutableMapOf<String, MutableList<WorkoutSet>>()
            var skipped = 0

            lines.drop(1).forEach { line ->
                if (line.isBlank()) return@forEach
                val cols = parseCsvLine(line)
                if (cols.size < 8) { skipped++; return@forEach }
                try {
                    val date     = cols[0].trim()
                    val exName   = cols[1].trim().removeSurrounding("\"")
                    val muscle   = cols[2].trim().ifBlank { "Core" }
                    val routine  = cols[3].trim().ifBlank { "Full" }
                    val tipo     = cols[4].trim().lowercase()
                    val reps     = cols[6].trim().toIntOrNull()   ?: run { skipped++; return@forEach }
                    val weightKg = cols[7].trim().toFloatOrNull() ?: run { skipped++; return@forEach }

                    LocalDate.parse(date)

                    val exercise = knownByName[exName.lowercase()] ?: run {
                        val isStrength = tipo == "fuerza"
                        val color      = MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93)
                        val emoji = when (muscle) {
                            "Pecho"   -> "💪"; "Hombros" -> "🏋️"; "Triceps" -> "💪"
                            "Espalda" -> "🔙"; "Biceps"  -> "💪"; "Piernas" -> "🦵"
                            "Gluteos" -> "🍑"; "Core"    -> "🎯"; "Cardio"  -> "❤️"
                            else      -> "💪"
                        }
                        val newEx = Exercise(
                            id = nextId++, name = exName, muscle = muscle,
                            routine = routine, emoji = emoji, color = color,
                            isStrengthFocus = isStrength, isCustom = true
                        )
                        knownByName[exName.lowercase()] = newEx
                        newCustomExercises.add(newEx)
                        newEx
                    }

                    parsed.getOrPut(date) { mutableListOf() }
                        .add(WorkoutSet(exercise.id, exercise.name, reps, weightKg))

                } catch (e: Exception) { skipped++ }
            }

            if (parsed.isEmpty())
                return ImportResult.Error("No se encontraron sesiones válidas en el archivo")

            val existingMap = existingSessions.associateBy { it.date }.toMutableMap()
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
                                    ex.reps == imp.reps && ex.weightKg == imp.weightKg
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
                mergedSessions = finalSessions, newSessions = newSessions,
                updatedSessions = mergedSessions, newSets = newSets,
                skippedRows = skipped, newCustomExercises = newCustomExercises
            )
        } catch (e: Exception) {
            ImportResult.Error("Error al procesar el archivo: ${e.message}")
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result  = mutableListOf<String>()
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

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

enum class TrendState { PROGRESSING, STAGNANT, FATIGUE }

data class ExerciseTrend(
    val exercise: Exercise,
    val latestMetric: Float,
    val avgLast5Metric: Float,
    val pctChange: Float,
    val trend: TrendState,
    val allTimeMetric: Float,
    val isStrength: Boolean
)

class GymViewModel : ViewModel() {

    var sets            = mutableStateListOf<WorkoutSet>();  private set
    var savedSessions   = mutableStateListOf<Session>();     private set
    var customExercises = mutableStateListOf<Exercise>();    private set

    val allExercises: List<Exercise> get() = SEED_EXERCISES + customExercises

    var muscleFilter          by mutableStateOf("Todos")
    var routineFilter         by mutableStateOf("Todas")
    var filterTab             by mutableStateOf(0)
    var searchQuery           by mutableStateOf("")
    var progressRoutineFilter by mutableStateOf("Todas")

    var sessionDate by mutableStateOf(LocalDate.now().toString())

    val filteredExercises: List<Exercise> get() {
        val byFilter = allExercises.filter { ex ->
            if (filterTab == 0) muscleFilter == "Todos" || ex.muscle == muscleFilter
            else                routineFilter == "Todas" || ex.routine == routineFilter
        }
        return if (searchQuery.isBlank()) byFilter
        else byFilter.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    val progressExercises: List<Exercise> get() {
        val trained = allExercises.filter { ex -> savedSessions.any { s -> s.sets.any { it.exerciseId == ex.id } } }
        return if (progressRoutineFilter == "Todas") trained else trained.filter { it.routine == progressRoutineFilter }
    }

    fun setsFor(exerciseId: Int) = sets.filter { it.exerciseId == exerciseId }
    val totalReps   get() = sets.sumOf { it.reps }
    val totalVolume get() = sets.sumOf { (it.weightKg * it.reps).toDouble() }.toFloat()
    val groupedSets get() = sets.groupBy { it.exerciseName }
    val trainedDates: Set<String> get() = savedSessions.map { it.date }.toSet()

    val weekStats: WeekStats get() {
        val weekDates = currentWeekDates().toSet()
        val weekSessions = savedSessions.filter { it.date in weekDates }
        val daysWithSession = weekSessions.map { it.date }.toSet().size
        val allSets = weekSessions.flatMap { it.sets }
        return WeekStats(
            daysTrainedThisWeek = daysWithSession, sessionsThisWeek = weekSessions.size,
            setsThisWeek = allSets.size, repsThisWeek = allSets.sumOf { it.reps },
            volumeThisWeek = allSets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong()
        )
    }

    val weekDayIndicator: List<Pair<String, Boolean>> get() {
        val labels = listOf("L","M","X","J","V","S","D")
        val monday = currentWeekMonday()
        return (0..6).map { i ->
            val date = monday.plusDays(i.toLong()).toString()
            labels[i] to (date in trainedDates)
        }
    }

    fun logSet(exercise: Exercise, reps: Int, weightKg: Float, context: Context) {
        sets.add(WorkoutSet(exercise.id, exercise.name, reps, weightKg))
        Storage.savePendingSession(context, sets.toList(), sessionDate)
    }

    fun editSet(oldSet: WorkoutSet, newReps: Int, newWeightKg: Float, context: Context) {
        val idx = sets.indexOf(oldSet); if (idx < 0) return
        sets[idx] = oldSet.copy(reps = newReps, weightKg = newWeightKg)
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
        savedSessions.clear();   savedSessions.addAll(Storage.load(context))
        customExercises.clear(); customExercises.addAll(Storage.loadCustomExercises(context))
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
        customExercises.add(ex); Storage.saveCustomExercises(context, customExercises.toList())
    }
    fun deleteCustomExercise(ex: Exercise, context: Context) {
        customExercises.remove(ex); Storage.saveCustomExercises(context, customExercises.toList())
    }
    fun nextCustomId() = (allExercises.maxOfOrNull { it.id } ?: 100) + 1

    fun importSessions(result: ImportResult.Success, context: Context) {
        result.newCustomExercises.forEach { newEx ->
            if (customExercises.none { it.name.equals(newEx.name, ignoreCase = true) })
                customExercises.add(newEx)
        }
        if (result.newCustomExercises.isNotEmpty())
            Storage.saveCustomExercises(context, customExercises.toList())
        savedSessions.clear(); savedSessions.addAll(result.mergedSessions)
        Storage.save(context, savedSessions.toList())
    }

    fun historyFor(exerciseId: Int): List<Pair<String, WorkoutSet>> =
        savedSessions.flatMap { s -> s.sets.filter { it.exerciseId == exerciseId }.map { s.date to it } }
            .sortedBy { it.first }

    fun prFor(exerciseId: Int): Float = historyFor(exerciseId).maxOfOrNull { it.second.weightKg } ?: 0f
    fun allTimeE1RMFor(exerciseId: Int): Float = e1rmProgressionFor(exerciseId).maxOfOrNull { it.second } ?: 0f
    fun maxRepsFor(exerciseId: Int): Int = historyFor(exerciseId).maxOfOrNull { it.second.reps } ?: 0

    fun e1rmProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }.sortedBy { it.date }
            .map { s -> s.date to bestE1RM(s.sets.filter { it.exerciseId == exerciseId }) }

    fun hypertrophyProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }.sortedBy { it.date }
            .map { s -> s.date to bestHypertrophyScore(s.sets.filter { it.exerciseId == exerciseId }) }

    fun weightProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }.sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }.maxOf { it.weightKg } }

    fun volumeProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }.sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat() }

    fun repsProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }.sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }.sumOf { it.reps }.toFloat() }

    fun trendFor(exerciseId: Int): ExerciseTrend? {
        val ex = allExercises.find { it.id == exerciseId } ?: return null
        val metricData = if (ex.isStrengthFocus) e1rmProgressionFor(exerciseId) else hypertrophyProgressionFor(exerciseId)
        if (metricData.isEmpty()) return null
        val allTime = metricData.maxOf { it.second }
        val last3   = metricData.takeLast(3)
        val latest  = last3.last().second
        val prev2   = last3.dropLast(1)
        val avg3    = if (prev2.isEmpty()) latest else prev2.map { it.second }.average().toFloat()
        val trend = when {
            last3.size >= 2 && latest > avg3 + 0.5f -> TrendState.PROGRESSING
            last3.size >= 3 && last3.zipWithNext().all { (a, b) -> b.second < a.second } -> TrendState.FATIGUE
            ((if (avg3 == 0f) 0f else ((latest - avg3) / avg3) * 100f)) < -3f -> TrendState.FATIGUE
            else -> TrendState.STAGNANT
        }
        val last5 = metricData.dropLast(1).takeLast(5)
        val avg5  = if (last5.isEmpty()) latest else last5.map { it.second }.average().toFloat()
        val pct5  = if (avg5 == 0f) 0f else ((latest - avg5) / avg5) * 100f
        return ExerciseTrend(ex, latest, avg3, pct5, trend, allTime, ex.isStrengthFocus)
    }

    val totalVolumeAllTime: Long get() =
        savedSessions.sumOf { s -> s.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toLong() }

    // Calcula la rutina dominante de una sesión (la más frecuente en los sets)
    fun dominantRoutineForSession(date: String): String? {
        val session = savedSessions.find { it.date == date } ?: return null
        val routines = session.sets.mapNotNull { set ->
            allExercises.find { it.id == set.exerciseId }?.routine
        }
        return routines.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY & NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); enableEdgeToEdge(); setContent { GymApp() }
    }
}

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Tab("home",     "Inicio",        Icons.Default.Home)
    object Train    : Tab("train",    "Entrenamiento", Icons.Default.FitnessCenter)
    object Calendar : Tab("calendar", "Historial",     Icons.Default.CalendarMonth)
    object Progress : Tab("progress", "Progreso",      Icons.AutoMirrored.Filled.TrendingUp)
}

val TABS = listOf(Tab.Home, Tab.Train, Tab.Calendar, Tab.Progress)

@Composable
fun GymApp() {
    val context = LocalContext.current
    val vm: GymViewModel = viewModel()

    LaunchedEffect(Unit) { vm.loadAll(context) }

    MaterialTheme(colorScheme = darkColorScheme(
        primary        = Accent,    onPrimary    = Black,
        background     = Surface0,  onBackground = TextPrim,
        surface        = Surface1,  onSurface    = TextPrim,
        surfaceVariant = Surface2,  outline      = Border,
    )) {
        val nav      = rememberNavController()
        val navEntry by nav.currentBackStackEntryAsState()
        val current  = navEntry?.destination?.route
        val innerRoutes = setOf(Tab.Train.route, Tab.Calendar.route, Tab.Progress.route)

        Scaffold(containerColor = Surface0, bottomBar = {
            AnimatedVisibility(visible = current in innerRoutes,
                enter = slideInVertically(initialOffsetY = { it }),
                exit  = slideOutVertically(targetOffsetY = { it })) {
                NavigationBar(containerColor = Surface1, tonalElevation = 0.dp) {
                    TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick  = {
                                if (tab == Tab.Home)
                                    nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true }; launchSingleTop = true }
                                else nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon  = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label, fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Black,   selectedTextColor   = Accent,
                                unselectedIconColor = TextSec, unselectedTextColor = TextSec,
                                indicatorColor      = Accent
                            )
                        )
                    }
                }
            }
        }) { padding ->
            NavHost(navController = nav, startDestination = Tab.Home.route,
                modifier = Modifier.padding(padding)) {
                composable(Tab.Home.route)     { HomeScreen(vm, { nav.navigate(Tab.Train.route) }, { nav.navigate(Tab.Calendar.route) }, { nav.navigate(Tab.Progress.route) }) }
                composable(Tab.Train.route)    { ExercisesScreen(vm) { nav.navigate("session") } }
                composable(Tab.Calendar.route) { CalendarScreen(vm) }
                composable(Tab.Progress.route) { ProgressScreen(vm) }
                composable("session") {
                    SessionScreen(vm, onBack = { nav.popBackStack() }, onSave = {
                        vm.saveSession(context)
                        nav.navigate("summary") { popUpTo(Tab.Train.route) }
                    })
                }
                composable("summary") {
                    SummaryScreen(vm) { nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true } } }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(vm: GymViewModel, onTrain: () -> Unit, onCalendar: () -> Unit, onProgress: () -> Unit) {
    val context  = LocalContext.current
    val week     = vm.weekStats
    val weekDays = vm.weekDayIndicator
    var showDiscardConfirm by remember { mutableStateOf(false) }

    if (showDiscardConfirm) {
        ConfirmDeleteDialog(
            title = "Descartar sesión pendiente",
            body  = "¿Seguro que quieres descartar la sesión en progreso? Se perderán las ${vm.sets.size} series registradas.",
            onConfirm = { vm.discardPendingSession(context); showDiscardConfirm = false },
            onDismiss = { showDiscardConfirm = false }
        )
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Surface0),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp)
    ) {
        item { Spacer(Modifier.height(64.dp)) }

        item {
            Column(Modifier.fillMaxWidth()) {
                Text("GYM",     fontSize = 52.sp, fontWeight = FontWeight.Black, color = Accent,   lineHeight = 52.sp)
                Text("TRACKER", fontSize = 52.sp, fontWeight = FontWeight.Black, color = TextPrim, lineHeight = 52.sp)
            }
            Spacer(Modifier.height(40.dp))
        }

        item {
            AnimatedVisibility(visible = vm.sets.isNotEmpty(),
                enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = OrangeStk.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, OrangeStk.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("⚡", fontSize = 20.sp)
                            Column(Modifier.weight(1f)) {
                                Text("Sesión en progreso", fontSize = 13.sp, color = OrangeStk, fontWeight = FontWeight.Bold)
                                Text("${vm.sets.size} series guardadas · ${formatDate(vm.sessionDate)}", fontSize = 11.sp, color = TextSec)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(onClick = { showDiscardConfirm = true }, shape = RoundedCornerShape(8.dp),
                                    color = RedBad.copy(0.1f), border = BorderStroke(1.dp, RedBad.copy(0.3f))) {
                                    Text("Descartar", fontSize = 11.sp, color = RedBad, fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                                Surface(onClick = onTrain, shape = RoundedCornerShape(8.dp),
                                    color = OrangeStk.copy(0.15f), border = BorderStroke(1.dp, OrangeStk.copy(0.4f))) {
                                    Text("Continuar", fontSize = 11.sp, color = OrangeStk, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        item {
            HomeNavButton(Icons.Default.FitnessCenter, "Entrenar", "Selecciona ejercicios y registra series", onTrain)
            Spacer(Modifier.height(12.dp))
            HomeNavButton(Icons.Default.CalendarMonth, "Historial", "Calendario de entrenamientos guardados", onCalendar)
            Spacer(Modifier.height(12.dp))
            HomeNavButton(Icons.AutoMirrored.Filled.TrendingUp, "Progreso", "E1RM, tendencias y marcas personales", onProgress)
        }

        item {
            Spacer(Modifier.height(36.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(18.dp))

            val monday = currentWeekMonday()
            val sunday = currentWeekSunday()
            val fmt = DateTimeFormatter.ofPattern("d MMM")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ESTA SEMANA", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = TextTert)
                Text("${monday.format(fmt)} – ${sunday.format(fmt)}", fontSize = 10.sp, color = TextTert)
            }
            Spacer(Modifier.height(14.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekDays.forEach { (label, trained) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(30.dp)
                            .background(if (trained) Accent else Surface2, RoundedCornerShape(8.dp))
                            .then(if (!trained) Modifier.border(1.dp, Border, RoundedCornerShape(8.dp)) else Modifier),
                            contentAlignment = Alignment.Center) {
                            if (trained) Text("✓", fontSize = 12.sp, color = Black, fontWeight = FontWeight.Black)
                        }
                        Text(label, fontSize = 9.sp, color = if (trained) Accent else TextTert,
                            fontWeight = if (trained) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                WeekStatItem(
                    value = if (week.daysTrainedThisWeek > 2) "${week.daysTrainedThisWeek}\uD83D\uDD25" else "${week.daysTrainedThisWeek}",
                    label = if (week.daysTrainedThisWeek == 1) "sesión" else "sesiones"
                )
                Box(Modifier.width(1.dp).height(28.dp).background(Border))
                WeekStatItem(value = "${week.setsThisWeek}", label = "series")
                Box(Modifier.width(1.dp).height(28.dp).background(Border))
                WeekStatItem(value = formatVol(week.volumeThisWeek), label = "volumen kg")
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
fun WeekStatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Accent)
        Text(label, fontSize = 10.sp, color = TextSec)
    }
}

fun formatVol(kg: Long): String = when {
    kg >= 1_000_000 -> "${kg / 1_000_000}M"
    kg >= 1_000     -> "${kg / 1_000}K"
    else            -> "$kg"
}

@Composable
fun HomeNavButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = Surface1, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(44.dp).background(Accent.copy(0.08f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(21.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title,    fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrim)
                Text(subtitle, fontSize = 12.sp, color = TextSec, lineHeight = 16.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTert, modifier = Modifier.size(18.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISES SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExercisesScreen(vm: GymViewModel, onGoToSession: () -> Unit) {
    var dialogExercise    by remember { mutableStateOf<Exercise?>(null) }
    var showAddCustom     by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Exercise?>(null) }
    val context = LocalContext.current

    if (showAddCustom) AddCustomExerciseDialog(vm, context) { showAddCustom = false }

    showDeleteConfirm?.let { ex ->
        ConfirmDeleteDialog(
            title = "Eliminar ejercicio",
            body  = "¿Eliminar \"${ex.name}\"? El historial no se perderá.",
            onConfirm = { vm.deleteCustomExercise(ex, context); showDeleteConfirm = null },
            onDismiss = { showDeleteConfirm = null }
        )
    }

    Scaffold(containerColor = Surface0,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SmallFloatingActionButton(onClick = { showAddCustom = true },
                    containerColor = Surface2, contentColor = Accent,
                    shape = RoundedCornerShape(13.dp)) { Icon(Icons.Default.Add, null) }
                AnimatedVisibility(visible = vm.sets.isNotEmpty(),
                    enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                    ExtendedFloatingActionButton(
                        onClick = onGoToSession,
                        containerColor = Accent, contentColor = Black,
                        icon = { Icon(Icons.Default.PlayArrow, null) },
                        text = { Text("Sesión (${vm.sets.size})", fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("Entrenamiento", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrim)
                AnimatedVisibility(visible = vm.sets.isNotEmpty()) {
                    Surface(shape = RoundedCornerShape(50), color = Accent.copy(0.1f)) {
                        Text("${vm.sets.size} series",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 12.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = vm.searchQuery, onValueChange = { vm.searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Buscar ejercicio…", color = TextTert, fontSize = 14.sp) },
                leadingIcon  = { Icon(Icons.Default.Search, null, tint = TextSec, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    AnimatedVisibility(visible = vm.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.searchQuery = "" }) {
                            Icon(Icons.Default.Close, null, tint = TextSec, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true, shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent, unfocusedBorderColor = Border,
                    focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                    cursorColor = Accent,
                    focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
                )
            )
            Spacer(Modifier.height(12.dp))

            AnimatedVisibility(visible = vm.searchQuery.isBlank()) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(Modifier.fillMaxWidth().background(Surface2, RoundedCornerShape(12.dp)).padding(3.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        listOf("Músculo", "Rutina").forEachIndexed { idx, label ->
                            val selected = vm.filterTab == idx
                            Surface(onClick = { vm.filterTab = idx }, modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                color = if (selected) Accent else Color.Transparent) {
                                Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                                    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                        color = if (selected) Black else TextSec)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        val list = if (vm.filterTab == 0) MUSCLES else ROUTINES
                        items(list) { label ->
                            val isAll = label == "Todos" || label == "Todas"
                            val sel = if (vm.filterTab == 0) vm.muscleFilter == label else vm.routineFilter == label
                            FilterChipItem(label = label, selected = sel, isDefault = isAll) {
                                if (vm.filterTab == 0) vm.muscleFilter = if (sel && !isAll) "Todos" else label
                                else vm.routineFilter = if (sel && !isAll) "Todas" else label
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }

            val exercises = vm.filteredExercises
            if (exercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin resultados para \"${vm.searchQuery}\"", color = TextSec, textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 110.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement   = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()) {
                    items(exercises, key = { it.id }) { exercise ->
                        ExerciseCard(exercise = exercise, setCount = vm.setsFor(exercise.id).size,
                            onClick  = { dialogExercise = exercise },
                            onDelete = if (exercise.isCustom) ({ showDeleteConfirm = exercise }) else null)
                    }
                }
            }
        }
    }

    dialogExercise?.let { ex ->
        LogSetDialog(ex, vm.setsFor(ex.id).lastOrNull(), onDismiss = { dialogExercise = null }) { reps, weight ->
            vm.logSet(ex, reps, weight, context); dialogExercise = null
        }
    }
}

@Composable
fun FilterChipItem(label: String, selected: Boolean, isDefault: Boolean, onClick: () -> Unit) {
    val bgColor     = when { selected && isDefault -> Surface3; selected -> Accent; else -> Color.Transparent }
    val textColor   = when { selected && isDefault -> TextSec;  selected -> Black;  else -> TextSec }
    val borderColor = when { selected && !isDefault -> Accent; selected -> Color.Transparent; else -> Border }
    Surface(onClick = onClick, shape = RoundedCornerShape(50), color = bgColor,
        border = BorderStroke(1.dp, borderColor), modifier = Modifier.height(34.dp)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp,
                fontWeight = if (selected && !isDefault) FontWeight.Bold else FontWeight.Normal,
                color = textColor)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CALENDAR SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CalendarScreen(vm: GymViewModel) {
    val context = LocalContext.current
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var displayMonth by remember { mutableStateOf(YearMonth.now()) }
    val session = selectedDate?.let { d -> vm.savedSessions.find { it.date == d } }

    Column(Modifier.fillMaxSize().background(Surface0)) {
        Text("Historial", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrim,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton({ displayMonth = displayMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft, null, tint = TextPrim) }
            Text(displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).replaceFirstChar { it.uppercase() },
                fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrim)
            IconButton({ displayMonth = displayMonth.plusMonths(1) }) { Icon(Icons.Default.ChevronRight, null, tint = TextPrim) }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("L","M","X","J","V","S","D").forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 11.sp, color = TextSec, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))
        CalendarGrid(displayMonth, vm.savedSessions.toList(), vm.allExercises, selectedDate) { date ->
            selectedDate = if (selectedDate == date) null else date
        }
        // Leyenda de rutinas
        CalendarLegend()
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(4.dp))

        when {
            selectedDate == null -> {
                val monthSessions = vm.savedSessions.filter {
                    try { YearMonth.from(LocalDate.parse(it.date)) == displayMonth } catch (e: Exception) { false }
                }.sortedByDescending { it.date }
                if (monthSessions.isEmpty()) {
                    EmptyState("Sin entrenamientos en ${displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).replaceFirstChar { it.uppercase() }}")
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(monthSessions, key = { it.date }) { s ->
                            SessionHistoryCard(s, vm.allExercises) { selectedDate = s.date }
                        }
                    }
                }
            }
            session == null -> EmptyState("Sin entrenamiento este día")
            else -> {
                var showDelete by remember(selectedDate) { mutableStateOf(false) }
                if (showDelete) ConfirmDeleteDialog(
                    title = "Eliminar sesión",
                    body  = "Se borrará todo el entrenamiento del ${formatDate(session.date)}.",
                    onConfirm = { vm.deleteSession(session.date, context); selectedDate = null; showDelete = false },
                    onDismiss = { showDelete = false }
                )
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(session.date), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                            Surface(onClick = { showDelete = true }, shape = RoundedCornerShape(10.dp),
                                color = RedBad.copy(0.08f), border = BorderStroke(1.dp, RedBad.copy(0.25f))) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = RedBad, modifier = Modifier.size(13.dp))
                                    Text("Borrar", fontSize = 11.sp, color = RedBad, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Series", "${session.sets.size}", Modifier.weight(1f))
                            StatCard("Reps",   "${session.sets.sumOf { it.reps }}", Modifier.weight(1f))
                            StatCard("Vol.", "${session.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()}kg", Modifier.weight(1f))
                        }
                    }
                    session.sets.groupBy { it.exerciseName }.forEach { (name, sets) ->
                        item(key = name) {
                            ExerciseBlock(name = name, sets = sets,
                                onDelete = { set -> vm.deleteSetFromSession(session.date, set, context) })
                        }
                    }
                }
            }
        }
    }
}

// ── Leyenda de colores de rutinas ──────────────────────────────────────────

@Composable
fun CalendarLegend() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ROUTINE_COLORS.entries.forEach { (routine, color) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    Modifier
                        .size(9.dp)
                        .background(color, CircleShape)
                )
                Text(
                    routine,
                    fontSize = 10.sp,
                    color = TextSec,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// ── CalendarGrid con colores por rutina ────────────────────────────────────

@Composable
fun CalendarGrid(
    month: YearMonth,
    sessions: List<Session>,
    allExercises: List<Exercise>,
    selectedDate: String?,
    onSelect: (String) -> Unit
) {
    val startOffset  = (month.atDay(1).dayOfWeek.value - 1) % 7
    val daysInMonth  = month.lengthOfMonth()
    val trainedDates = sessions.map { it.date }.toSet()

    // Pre-calcula la rutina dominante de cada sesión del mes
    val routineByDate: Map<String, String?> = remember(sessions, allExercises) {
        sessions.associate { session ->
            val routines = session.sets.mapNotNull { set ->
                allExercises.find { it.id == set.exerciseId }?.routine
            }
            val dominant = routines.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
            session.date to dominant
        }
    }

    Column(Modifier.padding(horizontal = 16.dp)) {
        repeat(((startOffset + daysInMonth) + 6) / 7) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayNum = row * 7 + col - startOffset + 1
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayNum in 1..daysInMonth) {
                            val dateStr      = month.atDay(dayNum).toString()
                            val trained      = dateStr in trainedDates
                            val selected     = dateStr == selectedDate
                            val isToday      = dateStr == LocalDate.now().toString()
                            val routine      = routineByDate[dateStr]
                            val routineColor = routine?.let { ROUTINE_COLORS[it] }
                            // Color activo: el de la rutina, o Accent como fallback
                            val activeColor  = routineColor ?: Accent

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .background(
                                            color = when {
                                                selected -> activeColor
                                                trained  -> activeColor.copy(alpha = 0.15f)
                                                else     -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .then(
                                            when {
                                                // Día de hoy + entrenado: borde sólido del color de rutina
                                                isToday && trained && !selected ->
                                                    Modifier.border(2.dp, activeColor, CircleShape)
                                                // Día de hoy sin entrenar: borde del Accent
                                                isToday && !trained && !selected ->
                                                    Modifier.border(1.5.dp, Accent, CircleShape)
                                                // Entrenado pero no hoy: borde tenue de la rutina
                                                trained && !selected ->
                                                    Modifier.border(1.dp, activeColor.copy(alpha = 0.6f), CircleShape)
                                                else -> Modifier
                                            }
                                        )
                                        .clickable { onSelect(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$dayNum",
                                        fontSize = 12.sp,
                                        fontWeight = if (trained || selected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            selected -> Black
                                            isToday  -> activeColor
                                            trained  -> TextPrim
                                            else     -> TextPrim.copy(alpha = 0.6f)
                                        }
                                    )
                                }
                                // Punto indicador de color de rutina (visible cuando no está seleccionado)
                                if (trained && !selected) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(
                                        Modifier
                                            .size(4.dp)
                                            .background(activeColor, CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: Session, allExercises: List<Exercise>, onClick: () -> Unit) {
    // Rutina dominante para mostrar el color
    val routines = session.sets.mapNotNull { set ->
        allExercises.find { it.id == set.exerciseId }?.routine
    }
    val dominantRoutine = routines.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val routineColor    = dominantRoutine?.let { ROUTINE_COLORS[it] } ?: Accent

    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = Surface1,
        border = BorderStroke(1.dp, routineColor.copy(alpha = 0.3f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // Indicador de color de rutina a la izquierda
            Box(
                Modifier
                    .width(3.dp)
                    .height(36.dp)
                    .background(routineColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(formatDate(session.date), fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 14.sp)
                    if (dominantRoutine != null) {
                        Surface(shape = RoundedCornerShape(4.dp), color = routineColor.copy(0.12f)) {
                            Text(dominantRoutine, fontSize = 9.sp, color = routineColor,
                                fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                Text(session.sets.map { it.exerciseName }.distinct().take(3).joinToString(" · "),
                    fontSize = 12.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.sets.size}", fontSize = 20.sp, fontWeight = FontWeight.Black, color = routineColor)
                Text("series", fontSize = 10.sp, color = TextSec)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISE CARD — drawable local → URL remota → emoji (3 niveles)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExerciseCard(
    exercise: Exercise,
    setCount: Int,
    onClick: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current

    Surface(
        onClick = onClick,
        modifier = Modifier.aspectRatio(0.85f),
        shape = RoundedCornerShape(16.dp),
        color = Surface1,
        border = BorderStroke(1.dp, Border)
    ) {
        Box {
            // Barra de color de músculo en la parte superior
            Box(Modifier.fillMaxWidth().height(3.dp).background(exercise.color))

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ── Área de visual ────────────────────────────────────────────
                Box(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(exercise.color.copy(0.06f), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseVisual(exercise = exercise, context = context)
                    // Badge CUSTOM
                    if (exercise.isCustom) {
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Blue.copy(0.15f)) {
                                Text("C", fontSize = 7.sp, color = Blue, fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))

                Text(exercise.name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrim, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(exercise.muscle.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        color = exercise.color, letterSpacing = 0.3.sp)
                    if (setCount > 0) Surface(shape = CircleShape, color = Accent) {
                        Text("$setCount",
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Black, color = Black)
                    }
                }
            }

            // Botón eliminar (solo ejercicios custom)
            if (onDelete != null) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 6.dp)
                ) {
                    Surface(
                        onClick = onDelete,
                        shape = CircleShape,
                        color = RedBad.copy(0.15f),
                        modifier = Modifier.size(18.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, tint = RedBad, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Sistema de visual por prioridad:
 *
 * Nivel 1 — Drawable local (res/drawable)
 *   Añade el archivo a app/src/main/res/drawable/ y regístralo en EXERCISE_DRAWABLES.
 *
 * Nivel 2 — URL remota (wger, imgur, etc.)
 *   Regístrala en EXERCISE_REMOTE_URLS. Requiere internet y Coil.
 *
 * Nivel 3 — Emoji del ejercicio (siempre funciona, sin dependencias)
 */
@Composable
fun ExerciseVisual(exercise: Exercise, context: Context) {

    // ── Nivel 1: drawable local ───────────────────────────────────────────────
    val drawableRes = EXERCISE_DRAWABLES[exercise.id]
    if (drawableRes != null) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = drawableRes),
            contentDescription = exercise.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
        )
        return
    }

    // ── Nivel 2: URL remota con Coil ─────────────────────────────────────────
    val remoteUrl = EXERCISE_REMOTE_URLS[exercise.id]
    if (remoteUrl != null) {
        var remoteFailed by remember(remoteUrl) { mutableStateOf(false) }
        if (!remoteFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(remoteUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = exercise.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                onError = { remoteFailed = true }
            )
            return
        }
        // URL falló → cae al emoji
    }

    // ── Nivel 3: emoji (fallback final, siempre disponible) ──────────────────
    Text(
        text = exercise.emoji,
        fontSize = 30.sp,
        textAlign = TextAlign.Center
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressScreen(vm: GymViewModel) {
    val context = LocalContext.current
    var selectedEx    by remember { mutableStateOf<Exercise?>(null) }
    var exportResult  by remember { mutableStateOf<String?>(null) }
    var importResult  by remember { mutableStateOf<ImportResult?>(null) }
    var importPending by remember { mutableStateOf<ImportResult.Success?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val result = Storage.importFromCsv(
                context, it, vm.savedSessions.toList(), vm.allExercises,
                vm.allExercises.maxOfOrNull { ex -> ex.id } ?: 100
            )
            when (result) {
                is ImportResult.Success -> importPending = result
                is ImportResult.Error   -> importResult  = result
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) exportResult = Storage.exportToDownloads(context, vm.savedSessions.toList(), vm.allExercises) ?: ""
        else {
            Storage.exportForShare(context, vm.savedSessions.toList(), vm.allExercises)?.let { uri ->
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Exportar CSV"))
            }
        }
    }

    fun doDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            exportResult = Storage.exportToDownloads(context, vm.savedSessions.toList(), vm.allExercises) ?: ""
        else {
            val perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED)
                exportResult = Storage.exportToDownloads(context, vm.savedSessions.toList(), vm.allExercises) ?: ""
            else permLauncher.launch(perm)
        }
    }

    fun doShare() {
        Storage.exportForShare(context, vm.savedSessions.toList(), vm.allExercises)?.let { uri ->
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }, "Compartir CSV"))
        }
    }

    exportResult?.let { result ->
        val ok = result.isNotEmpty()
        AlertDialog(onDismissRequest = { exportResult = null }, containerColor = Surface2,
            icon  = { Text(if (ok) "✅" else "❌", fontSize = 26.sp) },
            title = { Text(if (ok) "Exportado" else "Error al exportar", color = TextPrim, fontWeight = FontWeight.Bold) },
            text  = { Text(if (ok) "Guardado en Descargas:\n$result" else "No se pudo guardar. Prueba a compartirlo.", color = TextSec, fontSize = 13.sp) },
            confirmButton = { TextButton(onClick = { exportResult = null }) { Text("OK", color = Accent, fontWeight = FontWeight.Bold) } },
            dismissButton = if (!ok) ({ TextButton(onClick = { exportResult = null; doShare() }) { Text("Compartir", color = TextSec) } }) else null
        )
    }

    (importResult as? ImportResult.Error)?.let { err ->
        AlertDialog(onDismissRequest = { importResult = null }, containerColor = Surface2,
            icon  = { Text("❌", fontSize = 26.sp) },
            title = { Text("Error al importar", color = TextPrim, fontWeight = FontWeight.Bold) },
            text  = { Text(err.message, color = TextSec, fontSize = 13.sp) },
            confirmButton = { TextButton(onClick = { importResult = null }) { Text("OK", color = Accent, fontWeight = FontWeight.Bold) } }
        )
    }

    importPending?.let { pending ->
        AlertDialog(
            onDismissRequest = { importPending = null }, containerColor = Surface2,
            icon  = { Text("📥", fontSize = 26.sp) },
            title = { Text("Confirmar importación", color = TextPrim, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Se van a importar los siguientes datos:", color = TextSec, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    ImportStatRow("Sesiones nuevas",     "${pending.newSessions}")
                    ImportStatRow("Sesiones fusionadas", "${pending.updatedSessions}")
                    ImportStatRow("Series añadidas",     "${pending.newSets}")
                    if (pending.skippedRows > 0)
                        ImportStatRow("Filas omitidas", "${pending.skippedRows}", TextSec)
                    if (pending.newCustomExercises.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = Border)
                        Spacer(Modifier.height(6.dp))
                        Text("Ejercicios nuevos a crear (${pending.newCustomExercises.size}):",
                            color = TextSec, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        pending.newCustomExercises.take(5).forEach { ex ->
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(ex.emoji, fontSize = 12.sp)
                                Text(ex.name, fontSize = 12.sp, color = TextPrim,
                                    modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                TypeBadge(ex.muscle.uppercase(), ex.color)
                                if (ex.isStrengthFocus) TypeBadge("FUERZA", Accent)
                            }
                        }
                        if (pending.newCustomExercises.size > 5)
                            Text("… y ${pending.newCustomExercises.size - 5} más", fontSize = 11.sp, color = TextTert)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Las sesiones ya existentes se fusionarán sin duplicar series idénticas.",
                        color = TextTert, fontSize = 11.sp, lineHeight = 15.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.importSessions(pending, context)
                    importPending = null
                    importResult  = ImportResult.Success(
                        emptyList(), pending.newSessions, pending.updatedSessions,
                        pending.newSets, pending.skippedRows, pending.newCustomExercises
                    )
                }) { Text("Importar", color = Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { importPending = null }) { Text("Cancelar", color = TextSec) } }
        )
    }

    if (importResult is ImportResult.Success) {
        val s = importResult as ImportResult.Success
        AlertDialog(
            onDismissRequest = { importResult = null }, containerColor = Surface2,
            icon  = { Text("✅", fontSize = 26.sp) },
            title = { Text("Importación completada", color = TextPrim, fontWeight = FontWeight.Bold) },
            text  = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ImportStatRow("Sesiones nuevas",     "${s.newSessions}")
                    ImportStatRow("Sesiones fusionadas", "${s.updatedSessions}")
                    ImportStatRow("Series añadidas",     "${s.newSets}")
                    if (s.newCustomExercises.isNotEmpty())
                        ImportStatRow("Ejercicios creados", "${s.newCustomExercises.size}", Blue)
                }
            },
            confirmButton = { TextButton(onClick = { importResult = null }) { Text("OK", color = Accent, fontWeight = FontWeight.Bold) } }
        )
    }

    if (selectedEx == null) {
        Column(Modifier.fillMaxSize().background(Surface0)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Progreso", fontSize = 26.sp, fontWeight = FontWeight.Black, color = TextPrim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = { importLauncher.launch("text/*") },
                        shape   = RoundedCornerShape(11.dp),
                        color   = Accent.copy(0.08f),
                        border  = BorderStroke(1.dp, Accent.copy(0.25f)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Icon(Icons.Default.Upload, null, tint = Accent, modifier = Modifier.size(14.dp))
                            Text("CSV", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (vm.savedSessions.isNotEmpty()) {
                        Surface(onClick = { doShare() }, shape = RoundedCornerShape(11.dp),
                            color = Surface2, border = BorderStroke(1.dp, Border), modifier = Modifier.size(36.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("📤", fontSize = 15.sp) }
                        }
                        Surface(onClick = { doDownload() }, shape = RoundedCornerShape(11.dp),
                            color = Accent.copy(0.08f), border = BorderStroke(1.dp, Accent.copy(0.25f)),
                            modifier = Modifier.height(36.dp)) {
                            Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Icon(Icons.Default.Download, null, tint = Accent, modifier = Modifier.size(14.dp))
                                Text("CSV", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)) {
                items(ROUTINES) { r -> Chip(r, vm.progressRoutineFilter == r) { vm.progressRoutineFilter = r } }
            }
            if (vm.progressExercises.isEmpty()) {
                EmptyState(if (vm.savedSessions.isEmpty()) "Entrena y guarda sesiones\npara ver tu progreso" else "Sin ejercicios para este filtro")
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.progressExercises, key = { it.id }) { ex ->
                        val trend    = vm.trendFor(ex.id)
                        val sessions = vm.e1rmProgressionFor(ex.id).size
                        Surface(onClick = { selectedEx = ex }, shape = RoundedCornerShape(14.dp),
                            color = Surface1, border = BorderStroke(1.dp, Border)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Box(Modifier.size(44.dp).background(ex.color.copy(0.1f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center) { Text(ex.emoji, fontSize = 20.sp) }
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        Text(ex.name, fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 14.sp,
                                            modifier = Modifier.weight(1f, fill = false), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        if (ex.isStrengthFocus) TypeBadge("FUERZA", Accent)
                                        if (ex.isCustom)        TypeBadge("CUSTOM", Blue)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${ex.muscle} · $sessions ses.", fontSize = 12.sp, color = ex.color)
                                        trend?.let { Text(when(it.trend) { TrendState.PROGRESSING -> "🟢"; TrendState.STAGNANT -> "🟡"; TrendState.FATIGUE -> "🟠" }, fontSize = 10.sp) }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex.isStrengthFocus) {
                                        Text("${(trend?.latestMetric ?: 0f).roundToInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 15.sp)
                                        Text("E1RM", fontSize = 10.sp, color = TextSec)
                                    } else {
                                        val ld = vm.savedSessions.lastOrNull { s -> s.sets.any { it.exerciseId == ex.id } }?.date
                                        val bs = vm.historyFor(ex.id).filter { it.first == ld }.maxByOrNull { it.second.reps * it.second.weightKg }?.second
                                        if (bs != null) Text("${bs.reps}r×${bs.weightKg.toInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 13.sp)
                                        Text("mejor set", fontSize = 10.sp, color = TextSec)
                                    }
                                    trend?.let {
                                        if (it.pctChange != 0f) {
                                            val sign = if (it.pctChange > 0) "+" else ""
                                            Text("$sign${it.pctChange.roundToInt()}%", fontSize = 10.sp,
                                                color = if (it.pctChange > 0) GreenOk else RedBad, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        ExerciseDetailScreen(vm, selectedEx!!) { selectedEx = null }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISE DETAIL SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExerciseDetailScreen(vm: GymViewModel, exercise: Exercise, onBack: () -> Unit) {
    val isS       = exercise.isStrengthFocus
    val e1rmData  = vm.e1rmProgressionFor(exercise.id)
    val hyData    = vm.hypertrophyProgressionFor(exercise.id)
    val volData   = vm.volumeProgressionFor(exercise.id)
    val repsData  = vm.repsProgressionFor(exercise.id)
    val wData     = vm.weightProgressionFor(exercise.id)
    val byDate    = vm.historyFor(exercise.id).groupBy { it.first }
    val trend     = vm.trendFor(exercise.id)
    val maxWeight = vm.prFor(exercise.id)
    val maxReps   = vm.maxRepsFor(exercise.id)
    val bestVol   = volData.maxOfOrNull { it.second } ?: 0f
    var chartTab  by remember { mutableStateOf(0) }

    LazyColumn(Modifier.fillMaxSize().background(Surface0), contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrim) }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                        if (isS) TypeBadge("FUERZA", Accent)
                        if (exercise.isCustom) TypeBadge("CUSTOM", Blue)
                    }
                    Text("${exercise.muscle} · ${exercise.routine}", fontSize = 12.sp, color = exercise.color)
                }
            }
        }

        trend?.let { t ->
            item {
                val (bg, borderC, icon, status, sub) = when (t.trend) {
                    TrendState.PROGRESSING -> listOf(GreenOk.copy(0.07f),   GreenOk.copy(0.25f),   "🟢", "Progresando",   "+${t.pctChange.roundToInt()}% vs últimas 5")
                    TrendState.STAGNANT    -> listOf(YellowWarn.copy(0.07f), YellowWarn.copy(0.25f), "🟡", "Mantenimiento", "${t.pctChange.roundToInt()}% vs últimas 5")
                    TrendState.FATIGUE     -> listOf(OrangeStk.copy(0.07f),  OrangeStk.copy(0.25f),  "🟠", "Fatiga",        "${t.pctChange.roundToInt()}% vs últimas 5")
                }
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(14.dp),
                    color = bg as Color, border = BorderStroke(1.dp, borderC as Color)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(icon as String, fontSize = 18.sp)
                        Column(Modifier.weight(1f)) {
                            Text(status as String, fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 13.sp)
                            Text(sub as String, fontSize = 11.sp, color = TextSec)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            val v = if (isS) "${t.latestMetric.roundToInt()}kg" else {
                                val ld = vm.savedSessions.lastOrNull { s -> s.sets.any { it.exerciseId == exercise.id } }?.date
                                val bs = vm.historyFor(exercise.id).filter { it.first == ld }.maxByOrNull { it.second.reps * it.second.weightKg }?.second
                                if (bs != null) "${bs.reps}r×${bs.weightKg.toInt()}kg" else "${t.latestMetric.roundToInt()}"
                            }
                            Text(v, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Accent)
                            Text(if (isS) "E1RM actual" else "mejor set", fontSize = 9.sp, color = TextSec)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        item {
            SectionLabel("RÉCORDS")
            val bsEver = vm.historyFor(exercise.id).maxByOrNull { it.second.reps * it.second.weightKg }?.second
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (isS) PRCard("🏆 E1RM", "${vm.allTimeE1RMFor(exercise.id).roundToInt()} kg", "Histórico", Modifier.weight(1f))
                else     PRCard("🏆 Mejor set", if (bsEver != null) "${bsEver.reps}r×${bsEver.weightKg.toInt()}kg" else "—", "reps × peso", Modifier.weight(1f))
                PRCard("🏋️ Peso", "${maxWeight} kg", "Máximo", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRCard("🔁 Reps", "$maxReps", "Máximo total", Modifier.weight(1f))
                PRCard("📈 Vol.", "${bestVol.roundToInt()} kg", "Mejor sesión", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SectionLabel("GRÁFICAS")
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ChipFlex(if (isS) "E1RM" else "Mejor set", chartTab == 0, Modifier.weight(1f)) { chartTab = 0 }
                ChipFlex("Vol.",  chartTab == 1, Modifier.weight(1f)) { chartTab = 1 }
                ChipFlex("Peso",  chartTab == 2, Modifier.weight(1f)) { chartTab = 2 }
                ChipFlex("Reps",  chartTab == 3, Modifier.weight(1f)) { chartTab = 3 }
            }
            Spacer(Modifier.height(10.dp))
            val data  = when (chartTab) { 0 -> if (isS) e1rmData else hyData; 1 -> volData; 2 -> wData; else -> repsData }
            val unit  = when (chartTab) { 0 -> if (isS) "kg" else ""; 1 -> "kg·r"; 2 -> "kg"; else -> "r" }
            val label = when (chartTab) { 0 -> if (isS) "E1RM estimado por sesión" else "Mejor set por sesión"; 1 -> "Volumen total por sesión"; 2 -> "Peso máximo por sesión"; else -> "Reps totales por sesión" }
            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Surface1) {
                Column(Modifier.padding(16.dp)) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSec, modifier = Modifier.padding(bottom = 10.dp))
                    if (data.size < 2) Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("Necesitas al menos 2 sesiones\npara ver la gráfica", color = TextSec, textAlign = TextAlign.Center, fontSize = 12.sp)
                    } else LineChart(data, exercise.color, unit, Modifier.fillMaxWidth().height(160.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (volData.size >= 2) {
            item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(16.dp), color = Surface1) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Volumen por sesión", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSec, modifier = Modifier.padding(bottom = 10.dp))
                        BarChart(volData, exercise.color, Modifier.fillMaxWidth().height(90.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        item { SectionLabel("HISTORIAL COMPLETO") }
        val sortedHistory = byDate.entries.sortedByDescending { it.key }
        sortedHistory.forEachIndexed { idx, (date, entries) ->
            item(key = date) {
                CollapsibleHistoryEntry(date = date, entries = entries, isS = isS,
                    maxWeight = maxWeight, isInitiallyExpanded = idx == 0)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// COLLAPSIBLE HISTORY ENTRY
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun CollapsibleHistoryEntry(
    date: String,
    entries: List<Pair<String, WorkoutSet>>,
    isS: Boolean,
    maxWeight: Float,
    isInitiallyExpanded: Boolean = false
) {
    var expanded by remember { mutableStateOf(isInitiallyExpanded) }
    val ss  = entries.map { it.second }
    val bE  = bestE1RM(ss)
    val bH  = bestHypertrophyScore(ss)
    val bHS = ss.maxByOrNull { it.reps * it.weightKg }

    Surface(shape = RoundedCornerShape(14.dp), color = Surface1,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(formatDate(date), fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 13.sp)
                    Text("${ss.size} series · Vol. ${ss.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()}kg",
                        fontSize = 10.sp, color = TextSec)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (isS) Text("E1RM ${bE.roundToInt()}kg", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                        else if (bHS != null) Text("${bHS.reps}r×${bHS.weightKg.toInt()}kg", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                    }
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = TextTert, modifier = Modifier.size(18.dp))
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 12.dp)) {
                    HorizontalDivider(color = Border)
                    Spacer(Modifier.height(8.dp))
                    entries.forEachIndexed { i, (_, set) ->
                        val e1  = estimatedOneRM(set.weightKg, set.reps)
                        val hy  = set.reps * set.weightKg
                        val top = if (isS) (e1 == bE && bE > 0f) else (hy == bH && bH > 0f)
                        val pr  = set.weightKg == maxWeight && maxWeight > 0f
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(22.dp).background(Surface3, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                Text("${i+1}", fontSize = 9.sp, color = TextSec, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${set.reps} reps", fontSize = 13.sp, color = TextPrim, modifier = Modifier.weight(1f))
                            Text(if (set.weightKg == 0f) "PC" else "${set.weightKg}kg", fontSize = 13.sp,
                                color = if (pr) Accent else TextSec,
                                fontWeight = if (pr) FontWeight.Black else FontWeight.Normal)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isS) "→${e1.roundToInt()}" else "=${hy.roundToInt()}",
                                fontSize = 10.sp, color = if (top) Accent else TextTert)
                            if (top) { Spacer(Modifier.width(2.dp)); Text("★", fontSize = 9.sp, color = Accent) }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SESSION SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: GymViewModel, onBack: () -> Unit, onSave: () -> Unit) {
    val context        = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    var editingSet     by remember { mutableStateOf<WorkoutSet?>(null) }

    if (showDatePicker) {
        val ps = rememberDatePickerState(
            initialSelectedDateMillis = try { LocalDate.parse(vm.sessionDate).toEpochDay() * 86_400_000L } catch (e: Exception) { System.currentTimeMillis() }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    ps.selectedDateMillis?.let { vm.sessionDate = LocalDate.ofEpochDay(it / 86_400_000L).toString() }
                    showDatePicker = false
                }) { Text("OK", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = TextSec) } },
            colors = DatePickerDefaults.colors(containerColor = Surface2)
        ) {
            DatePicker(state = ps, colors = DatePickerDefaults.colors(
                containerColor = Surface2, titleContentColor = TextPrim, headlineContentColor = Accent,
                weekdayContentColor = TextSec, dayContentColor = TextPrim,
                selectedDayContainerColor = Accent, selectedDayContentColor = Black,
                todayContentColor = Accent, todayDateBorderColor = Accent))
        }
    }

    editingSet?.let { setToEdit ->
        val exercise = vm.allExercises.find { it.id == setToEdit.exerciseId }
        if (exercise != null) {
            EditSetDialog(exercise = exercise, currentSet = setToEdit, onDismiss = { editingSet = null }) { newReps, newWeight ->
                vm.editSet(setToEdit, newReps, newWeight, context); editingSet = null
            }
        }
    }

    Scaffold(containerColor = Surface0,
        topBar = {
            TopAppBar(title = { Text("SESIÓN", fontWeight = FontWeight.Bold, color = TextPrim) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrim) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface0))
        },
        bottomBar = {
            AnimatedVisibility(visible = vm.sets.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }), exit = slideOutVertically(targetOffsetY = { it })) {
                Box(Modifier.padding(16.dp)) {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                        Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                        Text("GUARDAR SESIÓN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(14.dp), color = Surface1,
                    border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Accent, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Fecha", fontSize = 10.sp, color = TextSec)
                                Text(if (vm.sessionDate == LocalDate.now().toString()) "Hoy — ${formatDate(vm.sessionDate)}" else formatDate(vm.sessionDate),
                                    fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 14.sp)
                            }
                        }
                        Text("Cambiar", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (vm.sets.isEmpty()) {
                item { Box(Modifier.fillMaxWidth().padding(top = 50.dp), contentAlignment = Alignment.Center) {
                    Text("Sin series — vuelve a Ejercicios para añadir", color = TextSec, textAlign = TextAlign.Center)
                }}
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("SERIES", "${vm.sets.size}", Modifier.weight(1f))
                        StatCard("REPS",   "${vm.totalReps}", Modifier.weight(1f))
                        StatCard("VOL.",   "${vm.totalVolume.toInt()}kg", Modifier.weight(1f))
                    }
                }
                vm.groupedSets.forEach { (name, sets) ->
                    item(key = name) {
                        ExerciseBlock(name = name, sets = sets,
                            onDelete = { vm.deleteSet(it, context) },
                            onEdit   = { editingSet = it })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SUMMARY SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun SummaryScreen(vm: GymViewModel, onBack: () -> Unit) {
    val lastSession = vm.savedSessions.lastOrNull()
    Scaffold(containerColor = Surface0,
        bottomBar = { Box(Modifier.padding(16.dp)) {
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                Text("VOLVER AL INICIO", fontWeight = FontWeight.Bold)
            }
        }}) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("🏆", fontSize = 48.sp); Spacer(Modifier.height(6.dp))
                Text("¡SESIÓN GUARDADA!", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextPrim, textAlign = TextAlign.Center)
                lastSession?.let { Text(formatDate(it.date), fontSize = 13.sp, color = TextSec) }
            }
            lastSession?.let { s ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigStat("SERIES", "${s.sets.size}", Modifier.weight(1f))
                        BigStat("REPS", "${s.sets.sumOf { it.reps }}", Modifier.weight(1f))
                    }
                }
                item { BigStat("VOLUMEN TOTAL", "${s.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()} KG", Modifier.fillMaxWidth()) }
                item { SectionLabel("POR EJERCICIO") }
                s.sets.groupBy { it.exerciseName }.forEach { (name, exSets) ->
                    item(key = name) {
                        val ex = vm.allExercises.find { it.name == name }
                        Surface(shape = RoundedCornerShape(14.dp), color = Surface1) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 14.sp)
                                    Text("${exSets.size} series · ${exSets.sumOf { it.reps }} reps", fontSize = 12.sp, color = TextSec)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex?.isStrengthFocus == true)
                                        Text("E1RM ${bestE1RM(exSets).roundToInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                    else {
                                        val bs = exSets.maxByOrNull { it.reps * it.weightKg }
                                        if (bs != null) Text("${bs.reps}r×${bs.weightKg.toInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                    }
                                    Text("${exSets.maxOf { it.weightKg }}kg max", fontSize = 11.sp, color = TextSec)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DIALOGS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LogSetDialog(exercise: Exercise, lastSet: WorkoutSet?, onDismiss: () -> Unit, onSave: (Int, Float) -> Unit) {
    var repsText   by remember { mutableStateOf(lastSet?.reps?.toString() ?: "") }
    var weightText by remember { mutableStateOf(lastSet?.weightKg?.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() } ?: "") }
    var repsError  by remember { mutableStateOf(false) }
    val isS = exercise.isStrengthFocus

    val previewE1 = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0; val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && isS) estimatedOneRM(w, r) else null
    }
    val previewHy = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0; val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && !isS) r * w else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                            if (isS) TypeBadge("FUERZA", Accent)
                        }
                        Text(exercise.muscle, fontSize = 12.sp, color = exercise.color)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                }
                if (lastSet != null) {
                    Surface(shape = RoundedCornerShape(10.dp), color = exercise.color.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                        Text(if (isS) "Última: ${lastSet.reps}r × ${lastSet.weightKg}kg → E1RM ${estimatedOneRM(lastSet.weightKg, lastSet.reps).roundToInt()}kg"
                        else "Última: ${lastSet.reps}r × ${lastSet.weightKg}kg (score ${(lastSet.reps * lastSet.weightKg).roundToInt()})",
                            fontSize = 12.sp, color = exercise.color, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("REPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec)
                        OutlinedTextField(value = repsText, onValueChange = { repsText = it; repsError = false },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                            isError = repsError, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                                cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("PESO (KG)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec)
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                                cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                    }
                }
                (previewE1 ?: previewHy)?.let { v ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Accent.copy(0.06f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isS) "E1RM estimado" else "Score reps×peso", fontSize = 12.sp, color = TextSec)
                            Text(if (isS) "${v.roundToInt()} kg" else "${v.roundToInt()}", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (repsError) Text("Introduce un número válido", fontSize = 11.sp, color = RedBad)
                Button(onClick = {
                    val r = repsText.trim().toIntOrNull()
                    if (r == null || r <= 0) { repsError = true; return@Button }
                    onSave(r, weightText.trim().toFloatOrNull() ?: 0f)
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                    Text("GUARDAR SERIE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditSetDialog(exercise: Exercise, currentSet: WorkoutSet, onDismiss: () -> Unit, onSave: (Int, Float) -> Unit) {
    var repsText   by remember { mutableStateOf(currentSet.reps.toString()) }
    var weightText by remember { mutableStateOf(
        currentSet.weightKg.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }
    ) }
    var repsError  by remember { mutableStateOf(false) }
    val isS = exercise.isStrengthFocus

    val previewE1 = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0; val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && isS) estimatedOneRM(w, r) else null
    }
    val previewHy = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0; val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && !isS) r * w else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Editar serie", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                        Text(exercise.name, fontSize = 12.sp, color = exercise.color)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                }
                Surface(shape = RoundedCornerShape(10.dp), color = YellowWarn.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("✏️", fontSize = 12.sp)
                        Text("Valores actuales: ${currentSet.reps} reps × ${if (currentSet.weightKg == 0f) "PC" else "${currentSet.weightKg}kg"}",
                            fontSize = 12.sp, color = YellowWarn)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("REPS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec)
                        OutlinedTextField(value = repsText, onValueChange = { repsText = it; repsError = false },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                            isError = repsError, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                                cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("PESO (KG)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec)
                        OutlinedTextField(value = weightText, onValueChange = { weightText = it },
                            modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                                cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                    }
                }
                (previewE1 ?: previewHy)?.let { v ->
                    Surface(shape = RoundedCornerShape(10.dp), color = Accent.copy(0.06f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(if (isS) "E1RM estimado" else "Score reps×peso", fontSize = 12.sp, color = TextSec)
                            Text(if (isS) "${v.roundToInt()} kg" else "${v.roundToInt()}", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (repsError) Text("Introduce un número válido", fontSize = 11.sp, color = RedBad)
                Button(onClick = {
                    val r = repsText.trim().toIntOrNull()
                    if (r == null || r <= 0) { repsError = true; return@Button }
                    onSave(r, weightText.trim().toFloatOrNull() ?: 0f)
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                    Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp))
                    Text("ACTUALIZAR SERIE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddCustomExerciseDialog(vm: GymViewModel, context: Context, onDismiss: () -> Unit) {
    var name     by remember { mutableStateOf("") }
    var muscle   by remember { mutableStateOf(MUSCLES[1]) }
    var routine  by remember { mutableStateOf(ROUTINES[1]) }
    var strength by remember { mutableStateOf(false) }
    var nameErr  by remember { mutableStateOf(false) }
    var emoji    by remember { mutableStateOf("💪") }
    val emojis   = listOf("💪","🏋️","🔙","🦵","🎯","🍑","❤️","⚡","🔥","🌟","🤸","🧘")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Nuevo ejercicio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(emojis) { e ->
                        Surface(onClick = { emoji = e }, shape = RoundedCornerShape(10.dp),
                            color = if (emoji == e) Accent.copy(0.18f) else Surface3,
                            border = BorderStroke(1.dp, if (emoji == e) Accent.copy(0.4f) else Border),
                            modifier = Modifier.size(38.dp)) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(e, fontSize = 18.sp) }
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it; nameErr = false }, modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre del ejercicio", color = TextSec, fontSize = 12.sp) },
                    isError = nameErr, singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, unfocusedBorderColor = Border,
                        focusedTextColor = TextPrim, unfocusedTextColor = TextPrim, cursorColor = Accent,
                        focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                if (nameErr) Text("El nombre no puede estar vacío", fontSize = 11.sp, color = RedBad)
                Text("MÚSCULO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(MUSCLES.drop(1)) { m -> Chip(m, muscle == m) { muscle = m } }
                }
                Text("RUTINA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ROUTINES.drop(1)) { r -> Chip(r, routine == r) { routine = r } }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Ejercicio de fuerza", fontSize = 14.sp, color = TextPrim)
                        Text("Activa E1RM como métrica de progreso", fontSize = 11.sp, color = TextSec)
                    }
                    Switch(checked = strength, onCheckedChange = { strength = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Accent,
                            uncheckedThumbColor = TextSec, uncheckedTrackColor = Border))
                }
                Button(onClick = {
                    if (name.isBlank()) { nameErr = true; return@Button }
                    vm.addCustomExercise(Exercise(vm.nextCustomId(), name.trim(), muscle, routine, emoji,
                        MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93), strength, isCustom = true), context)
                    onDismiss()
                }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                    Text("CREAR EJERCICIO", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfirmDeleteDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Surface2,
        title = { Text(title, color = TextPrim, fontWeight = FontWeight.Bold) },
        text  = { Text(body,  color = TextSec,  fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar", color = RedBad, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSec) } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// CHARTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LineChart(data: List<Pair<String, Float>>, color: Color, unit: String, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    val values = data.map { it.second }
    val minV   = values.min(); val maxV = values.max()
    val range  = if (maxV == minV) 1f else maxV - minV
    val maxIdx = values.indexOf(maxV)

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val padT = 16f; val padB = 28f; val dH = h - padT - padB
            val sx = w / (data.size - 1).toFloat()
            fun xAt(i: Int)   = i * sx
            fun yAt(v: Float) = padT + dH * (1f - (v - minV) / range)
            val fill = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) { val cx = (xAt(i-1)+xAt(i))/2f; cubicTo(cx, yAt(values[i-1]), cx, yAt(values[i]), xAt(i), yAt(values[i])) }
                lineTo(xAt(data.size-1), h); lineTo(xAt(0), h); close()
            }
            drawPath(fill, Brush.verticalGradient(listOf(color.copy(0.28f), Color.Transparent), startY = padT, endY = h))
            val line = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) { val cx = (xAt(i-1)+xAt(i))/2f; cubicTo(cx, yAt(values[i-1]), cx, yAt(values[i]), xAt(i), yAt(values[i])) }
            }
            drawPath(line, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
            data.indices.forEach { i ->
                val isMax = i == maxIdx
                drawCircle(color, radius = if (isMax) 6f else 4f, center = Offset(xAt(i), yAt(values[i])))
                drawCircle(Surface1, radius = if (isMax) 3.5f else 2.5f, center = Offset(xAt(i), yAt(values[i])))
            }
        }
        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${values.first().roundToInt()}$unit", fontSize = 10.sp, color = TextSec)
            if (maxIdx != 0 && maxIdx != data.size-1) Text("${maxV.roundToInt()}$unit ★", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
            Text("${values.last().roundToInt()}$unit", fontSize = 10.sp, color = TextPrim, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BarChart(data: List<Pair<String, Float>>, color: Color, modifier: Modifier = Modifier) {
    if (data.isEmpty()) return
    val maxV   = data.maxOf { it.second }.let { if (it == 0f) 1f else it }
    val maxVal = data.maxOf { it.second }
    Canvas(modifier) {
        val w = size.width; val h = size.height - 4f
        val barW = (w / data.size) * 0.55f; val gap = (w / data.size) * 0.45f
        data.forEachIndexed { i, (_, v) ->
            val bH = (v / maxV) * h; val left = i * (barW + gap) + gap / 2f
            drawRoundRect(color = color.copy(if (v == maxVal) 1f else 0.35f),
                topLeft = Offset(left, h - bH), size = Size(barW, bH), cornerRadius = CornerRadius(barW / 2f))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExerciseBlock(
    name:     String,
    sets:     List<WorkoutSet>,
    onDelete: ((WorkoutSet) -> Unit)?,
    onEdit:   ((WorkoutSet) -> Unit)? = null
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Surface1) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrim)
            Text("${sets.size} series · ${sets.sumOf { it.reps }} reps · max ${sets.maxOf { it.weightKg }}kg", fontSize = 12.sp, color = TextSec)
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(6.dp))
            sets.forEachIndexed { i, set ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(24.dp).background(Surface3, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                        Text("${i+1}", fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("${set.reps} reps", fontSize = 14.sp, color = TextPrim, modifier = Modifier.weight(1f))
                    Text(if (set.weightKg == 0f) "Peso corp." else "${set.weightKg}kg", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                    if (onEdit != null) {
                        IconButton(onClick = { onEdit(set) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Edit, null, tint = TextSec, modifier = Modifier.size(14.dp))
                        }
                    }
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(set) }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, null, tint = TextTert, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable fun TypeBadge(label: String, color: Color) {
    Surface(shape = RoundedCornerShape(4.dp), color = color.copy(0.1f)) {
        Text(label, fontSize = 7.sp, color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}

@Composable fun SectionLabel(text: String) {
    Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
        color = TextTert, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
}

@Composable fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.height(32.dp), shape = RoundedCornerShape(50),
        color = if (selected) Accent else Surface2, border = if (selected) null else BorderStroke(1.dp, Border)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Black else TextSec)
        }
    }
}

@Composable fun ChipFlex(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(32.dp), shape = RoundedCornerShape(50),
        color = if (selected) Accent else Surface2, border = if (selected) null else BorderStroke(1.dp, Border)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (selected) Black else TextSec,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable fun PRCard(label: String, value: String, sublabel: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Surface1, border = BorderStroke(1.dp, Accent.copy(0.15f))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 11.sp, color = TextSec, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sublabel, fontSize = 9.sp, color = TextTert)
        }
    }
}

@Composable fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = Surface1) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Accent)
            Text(label, fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable fun BigStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Surface1) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 24.sp, color = Accent)
            Text(label, fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable fun EmptyState(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = TextSec, textAlign = TextAlign.Center, lineHeight = 22.sp)
    }
}

@Composable
fun ImportStatRow(label: String, value: String, valueColor: Color = Accent) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = TextSec)
        Text(value, fontSize = 13.sp, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

fun formatDate(dateStr: String): String = try {
    LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) { dateStr }