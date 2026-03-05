package com.gymtracker

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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
    // CAMBIO 3: true = ejercicio de fuerza (usa E1RM), false = hipertrofia (usa reps×peso)
    val isStrengthFocus: Boolean = false
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

// E1RM Epley formula: weight × (1 + reps/30)
fun estimatedOneRM(weightKg: Float, reps: Int): Float {
    if (weightKg == 0f) return 0f
    return weightKg * (1f + reps / 30f)
}

// Best E1RM from a list of sets (best single set)
fun bestE1RM(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { estimatedOneRM(it.weightKg, it.reps) } ?: 0f

// CAMBIO 3: Métrica de progreso para ejercicios de hipertrofia = mejor set (reps × peso)
fun bestHypertrophyScore(sets: List<WorkoutSet>): Float =
    sets.maxOfOrNull { it.reps * it.weightKg } ?: 0f

// ─────────────────────────────────────────────────────────────────────────────
// SEED DATA
// ─────────────────────────────────────────────────────────────────────────────

val EXERCISES = listOf(
    // CAMBIO 3: Press Banca = fuerza (isStrengthFocus = true)
    Exercise(1,  "Press Banca",             "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), isStrengthFocus = true),
    Exercise(2,  "Press Inclinado",         "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), isStrengthFocus = true),
    Exercise(2,  "Press Inclinado Manc.",   "Pecho",   "Push",   "💪", Color(0xFFFF6B6B), isStrengthFocus = true),

    Exercise(31, "Aperturas Cable",     "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
    Exercise(3,  "Peck Deck",           "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),
    Exercise(4,  "Fondos",              "Pecho",   "Push",   "💪", Color(0xFFFF6B6B)),

    Exercise(5,  "Press Militar",       "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B), isStrengthFocus = true),
    Exercise(6,  "Elevaciones Lat.",    "Hombros", "Push",   "🏋️", Color(0xFFFFBE0B)),
    Exercise(7,  "Reversed Peck Deck",  "Hombros", "Pull",   "🏋️", Color(0xFFFFBE0B)),
    Exercise(17, "Facepull",            "Hombros", "Pull",   "🎯", Color(0xFFFFBE0B)),

    Exercise(8,  "Extensiones Triceps",       "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(9,  "Extensiones Katana",        "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(32, "Extensiones Unilateral",    "Triceps", "Push",   "💪", Color(0xFF8338EC)),
    Exercise(33, "Extensiones sobre cabeza",  "Triceps", "Push",   "💪", Color(0xFF8338EC)),

    Exercise(10, "Dominadas",                    "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(11, "Remo en T",                    "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(12, "Jalón al Pecho",               "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(13, "Remo en Máquina Unilateral",   "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(34, "Remo en Polea Unilateral",     "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),
    Exercise(35, "Pull Over",                    "Espalda", "Pull",   "🔙", Color(0xFF4ECDC4)),

    Exercise(14, "Curl Biceps Unilateral",  "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(15, "Curl Martillo",           "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(16, "Curl Bayesian",           "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),
    Exercise(36, "Curl Predicador",         "Biceps",  "Pull",   "💪", Color(0xFF3A86FF)),

    Exercise(18, "Sentadilla Libre",        "Piernas", "Legs",   "🦵", Color(0xFFFF006E), isStrengthFocus = true),
    Exercise(23, "Sentadilla MultiPower",   "Piernas", "Legs",   "🦵", Color(0xFFFF006E), isStrengthFocus = true),
    Exercise(37, "Peso Muerto",         "Piernas", "Legs",   "🦵", Color(0xFFFF006E), isStrengthFocus = true),
    Exercise(38, "Peso Muerto Sumo",    "Piernas", "Legs",   "🦵", Color(0xFFFF006E), isStrengthFocus = true),
    Exercise(39, "Peso Muerto Rumano",  "Piernas", "Legs",   "🦵", Color(0xFFFF006E), isStrengthFocus = true),

    Exercise(19, "Prensa de Piernas",   "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(20, "Extensiones Cuad.",   "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(21, "Curl Femoral",        "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(22, "Hip Thrust",          "Gluteos", "Legs",   "🍑", Color(0xFFFB5607)),
    Exercise(24, "Gemelos de Pie",      "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(40, "Aducciones",          "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),
    Exercise(41, "Abducciones",         "Piernas", "Legs",   "🦵", Color(0xFFFF006E)),

    Exercise(25, "Elevaciones de piernas",             "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(26, "Crunch Polea",        "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(27, "Rueda Abdominal",     "Core",    "Full",   "🎯", Color(0xFF06D6A0)),
    Exercise(28, "Cinta de Correr",     "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(29, "Bicicleta Est.",      "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(30, "Remo Ergómetro",      "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),
    Exercise(42, "Máquina de Escalera",      "Cardio",  "Cardio", "❤️", Color(0xFFE63946)),

    )

val MUSCLES  = listOf("Todos","Pecho","Hombros","Triceps","Espalda","Biceps","Piernas","Gluteos","Core","Cardio")
val ROUTINES = listOf("Todas","Push","Pull","Legs","Full","Cardio")

// ─────────────────────────────────────────────────────────────────────────────
// COLORS
// ─────────────────────────────────────────────────────────────────────────────

val Black    = Color(0xFF000000)
val DarkSurf = Color(0xFF1C1C1E)
val CardBg   = Color(0xFF242426)
val Border   = Color(0xFF2C2C2E)
val Accent   = Color(0xFFE8FF47)
val TextPrim = Color(0xFFF5F5F5)
val TextSec  = Color(0xFF8E8E93)
val TextTert = Color(0xFF48484A)
val GreenOk  = Color(0xFF30D158)
val YellowWarn = Color(0xFFFFD60A)
val RedBad   = Color(0xFFFF453A)

// ─────────────────────────────────────────────────────────────────────────────
// PERSISTENCE
// ─────────────────────────────────────────────────────────────────────────────

object Storage {
    private const val PREFS = "gym_data"
    private const val KEY   = "sessions"

    fun save(context: Context, sessions: List<Session>) {
        val arr = JSONArray()
        for (s in sessions) {
            val setsArr = JSONArray()
            for (ws in s.sets) {
                setsArr.put(JSONObject().apply {
                    put("eid",    ws.exerciseId)
                    put("ename",  ws.exerciseName)
                    put("reps",   ws.reps)
                    put("weight", ws.weightKg.toDouble())
                })
            }
            arr.put(JSONObject().apply {
                put("date", s.date)
                put("sets", setsArr)
            })
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, arr.toString()).apply()
    }

    fun load(context: Context): List<Session> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()
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

    // CAMBIO 2: exportCSV movido aquí, misma lógica pero ahora incluye columna "tipo"
    fun exportCSV(context: Context, sessions: List<Session>): Uri? {
        return try {
            val sb = StringBuilder()
            sb.appendLine("fecha,ejercicio,musculo,rutina,tipo,serie,reps,peso_kg,e1rm,score_hipertrofia")
            sessions.sortedBy { it.date }.forEach { session ->
                session.sets.groupBy { it.exerciseName }.forEach { (_, exSets) ->
                    exSets.forEachIndexed { idx, set ->
                        val ex = EXERCISES.find { it.id == set.exerciseId }
                        val e1rm = if (ex?.isStrengthFocus == true)
                            String.format("%.1f", estimatedOneRM(set.weightKg, set.reps))
                        else ""
                        val hyScore = if (ex?.isStrengthFocus == false)
                            String.format("%.1f", set.reps * set.weightKg)
                        else ""
                        val tipo = if (ex?.isStrengthFocus == true) "fuerza" else "hipertrofia"
                        sb.appendLine("${session.date},${set.exerciseName},${ex?.muscle ?: ""},${ex?.routine ?: ""},$tipo,${idx + 1},${set.reps},${set.weightKg},$e1rm,$hyScore")
                    }
                }
            }
            val file = File(context.cacheDir, "gymtracker_export.csv")
            file.writeText(sb.toString())
            FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        } catch (e: Exception) { null }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// VIEW MODEL
// ─────────────────────────────────────────────────────────────────────────────

enum class TrendState { PROGRESSING, STAGNANT, FATIGUE }

data class ExerciseTrend(
    val exercise: Exercise,
    // CAMBIO 3: métrica principal según tipo de ejercicio
    val latestMetric: Float,      // E1RM si fuerza, mejor reps×peso si hipertrofia
    val avgLast5Metric: Float,
    val pctChange: Float,
    val trend: TrendState,
    val allTimeMetric: Float,
    val isStrength: Boolean
)

class GymViewModel : ViewModel() {

    var sets = mutableStateListOf<WorkoutSet>()
        private set

    var savedSessions = mutableStateListOf<Session>()
        private set

    var muscleFilter  by mutableStateOf("Todos")
    var routineFilter by mutableStateOf("Todas")
    var filterTab     by mutableStateOf(0)

    var progressRoutineFilter by mutableStateOf("Todas")

    val filteredExercises: List<Exercise>
        get() = EXERCISES.filter { ex ->
            if (filterTab == 0) muscleFilter == "Todos" || ex.muscle == muscleFilter
            else                routineFilter == "Todas" || ex.routine == routineFilter
        }

    val progressExercises: List<Exercise>
        get() {
            val trained = EXERCISES.filter { ex ->
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

    var sessionDate by mutableStateOf(LocalDate.now().toString())

    fun logSet(exercise: Exercise, reps: Int, weightKg: Float) {
        sets.add(WorkoutSet(exercise.id, exercise.name, reps, weightKg))
    }

    fun deleteSet(set: WorkoutSet) { sets.remove(set) }

    fun saveSession(context: Context) {
        if (sets.isEmpty()) return
        val idx = savedSessions.indexOfFirst { it.date == sessionDate }
        if (idx >= 0) {
            savedSessions[idx] = Session(sessionDate, savedSessions[idx].sets + sets.toList())
        } else {
            savedSessions.add(Session(sessionDate, sets.toList()))
        }
        Storage.save(context, savedSessions.toList())
        sets.clear()
        sessionDate = LocalDate.now().toString()
    }

    fun loadSessions(context: Context) {
        savedSessions.clear()
        savedSessions.addAll(Storage.load(context))
    }

    fun deleteSetFromSession(date: String, set: WorkoutSet, context: Context) {
        val idx = savedSessions.indexOfFirst { it.date == date }
        if (idx < 0) return
        val remaining = savedSessions[idx].sets.toMutableList().also { it.remove(set) }
        if (remaining.isEmpty()) savedSessions.removeAt(idx)
        else savedSessions[idx] = Session(date, remaining)
        Storage.save(context, savedSessions.toList())
    }

    fun deleteSession(date: String, context: Context) {
        savedSessions.removeAll { it.date == date }
        Storage.save(context, savedSessions.toList())
    }

    fun historyFor(exerciseId: Int): List<Pair<String, WorkoutSet>> =
        savedSessions.flatMap { s -> s.sets.filter { it.exerciseId == exerciseId }.map { s.date to it } }
            .sortedBy { it.first }

    fun prFor(exerciseId: Int): Float =
        historyFor(exerciseId).maxOfOrNull { it.second.weightKg } ?: 0f

    fun allTimeE1RMFor(exerciseId: Int): Float =
        e1rmProgressionFor(exerciseId).maxOfOrNull { it.second } ?: 0f

    // CAMBIO 3: mejor score hipertrofia histórico (reps × peso)
    fun allTimeHypertrophyScoreFor(exerciseId: Int): Float =
        hypertrophyProgressionFor(exerciseId).maxOfOrNull { it.second } ?: 0f

    fun maxRepsFor(exerciseId: Int): Int =
        historyFor(exerciseId).maxOfOrNull { it.second.reps } ?: 0

    // Best E1RM per session (fuerza)
    fun e1rmProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val sessionSets = s.sets.filter { it.exerciseId == exerciseId }
                s.date to bestE1RM(sessionSets)
            }

    // CAMBIO 3: Mejor set de reps×peso por sesión (hipertrofia)
    fun hypertrophyProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s ->
                val sessionSets = s.sets.filter { it.exerciseId == exerciseId }
                s.date to bestHypertrophyScore(sessionSets)
            }

    // Max weight per session
    fun weightProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }.maxOf { it.weightKg } }

    // Total volume (kg * reps) per session
    fun volumeProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }
                .sumOf { (it.reps * it.weightKg).toDouble() }.toFloat() }

    // Total reps per session
    fun repsProgressionFor(exerciseId: Int): List<Pair<String, Float>> =
        savedSessions.filter { s -> s.sets.any { it.exerciseId == exerciseId } }
            .sortedBy { it.date }
            .map { s -> s.date to s.sets.filter { it.exerciseId == exerciseId }.sumOf { it.reps }.toFloat() }

    // CAMBIO 3: trendFor usa métrica según tipo de ejercicio
    fun trendFor(exerciseId: Int): ExerciseTrend? {
        val ex = EXERCISES.find { it.id == exerciseId } ?: return null
        val metricData = if (ex.isStrengthFocus) e1rmProgressionFor(exerciseId)
        else hypertrophyProgressionFor(exerciseId)
        if (metricData.isEmpty()) return null

        val latestMetric  = metricData.last().second
        val allTimeMetric = metricData.maxOf { it.second }
        val last5         = metricData.dropLast(1).takeLast(5)
        val avg5          = if (last5.isEmpty()) latestMetric else last5.map { it.second }.average().toFloat()
        val pctChange     = if (avg5 == 0f) 0f else ((latestMetric - avg5) / avg5) * 100f

        val trend = when {
            metricData.size >= 3 && metricData.takeLast(3).zipWithNext().all { (a, b) -> b.second < a.second } ->
                TrendState.FATIGUE
            pctChange > 1f  -> TrendState.PROGRESSING
            pctChange < -1f -> TrendState.FATIGUE
            else            -> TrendState.STAGNANT
        }

        return ExerciseTrend(ex, latestMetric, avg5, pctChange, trend, allTimeMetric, ex.isStrengthFocus)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ACTIVITY
// ─────────────────────────────────────────────────────────────────────────────

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { GymApp() }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// NAVIGATION
// ─────────────────────────────────────────────────────────────────────────────

sealed class Tab(val route: String, val label: String, val icon: ImageVector) {
    object Home     : Tab("home",     "Inicio",     Icons.Default.Home)
    object Train    : Tab("train",    "Ejercicios", Icons.Default.FitnessCenter)
    object Calendar : Tab("calendar", "Historial",  Icons.Default.CalendarMonth)
    object Progress : Tab("progress", "Progreso",   Icons.AutoMirrored.Filled.TrendingUp)
}

val TABS = listOf(Tab.Home, Tab.Train, Tab.Calendar, Tab.Progress)

@Composable
fun GymApp() {
    val context = LocalContext.current
    val vm: GymViewModel = viewModel()

    LaunchedEffect(Unit) { vm.loadSessions(context) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary        = Accent, onPrimary    = Black,
            background     = Black,  onBackground = TextPrim,
            surface        = DarkSurf, onSurface  = TextPrim,
            surfaceVariant = CardBg, outline      = Border,
        )
    ) {
        val nav      = rememberNavController()
        val navEntry by nav.currentBackStackEntryAsState()
        val current  = navEntry?.destination?.route

        val innerRoutes = setOf(Tab.Train.route, Tab.Calendar.route, Tab.Progress.route)

        Scaffold(
            containerColor = Black,
            bottomBar = {
                if (current in innerRoutes) {
                    NavigationBar(containerColor = DarkSurf, tonalElevation = 0.dp) {
                        TABS.forEach { tab ->
                            NavigationBarItem(
                                selected = current == tab.route,
                                onClick = {
                                    if (tab == Tab.Home) {
                                        nav.navigate(Tab.Home.route) {
                                            popUpTo(0) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    } else {
                                        nav.navigate(tab.route) {
                                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState    = true
                                        }
                                    }
                                },
                                icon  = { Icon(tab.icon, contentDescription = tab.label) },
                                label = { Text(tab.label, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = Black,
                                    selectedTextColor   = Accent,
                                    unselectedIconColor = TextSec,
                                    unselectedTextColor = TextSec,
                                    indicatorColor      = Accent
                                )
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController    = nav,
                startDestination = Tab.Home.route,
                modifier         = Modifier.padding(padding)
            ) {
                composable(Tab.Home.route) {
                    HomeScreen(vm,
                        onGoToTrain    = { nav.navigate(Tab.Train.route) },
                        onGoToCalendar = { nav.navigate(Tab.Calendar.route) },
                        onGoToProgress = { nav.navigate(Tab.Progress.route) }
                    )
                }
                composable(Tab.Train.route) {
                    ExercisesScreen(vm, onGoToSession = { nav.navigate("session") })
                }
                composable(Tab.Calendar.route) { CalendarScreen(vm) }
                composable(Tab.Progress.route) { ProgressScreen(vm) }
                composable("session") {
                    SessionScreen(vm,
                        onBack = { nav.popBackStack() },
                        onSave = {
                            vm.saveSession(context)
                            nav.navigate("summary") { popUpTo(Tab.Train.route) }
                        }
                    )
                }
                composable("summary") {
                    SummaryScreen(vm,
                        onBack = { nav.navigate(Tab.Home.route) { popUpTo(0) { inclusive = true } } }
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// HOME — CAMBIO 1: sin best/worst, solo stats simples (4 y 5 editables)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun HomeScreen(
    vm: GymViewModel,
    onGoToTrain: () -> Unit,
    onGoToCalendar: () -> Unit,
    onGoToProgress: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Black).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header — CAMBIO 1: eliminado botón de export
        Column {
            Text("GYM",     fontSize = 52.sp, fontWeight = FontWeight.Black, color = Accent,   lineHeight = 52.sp)
            Text("TRACKER", fontSize = 52.sp, fontWeight = FontWeight.Black, color = TextPrim, lineHeight = 52.sp)
        }

        Spacer(Modifier.height(12.dp))

        if (vm.sets.isNotEmpty()) {
            Surface(shape = RoundedCornerShape(50), color = Accent.copy(alpha = 0.15f)) {
                Text("Sesión activa · ${vm.sets.size} series",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    fontSize = 13.sp, color = Accent, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(28.dp))

        HomeButton(Icons.Default.FitnessCenter,         "Entrenar",  "Selecciona ejercicios y registra series",   onGoToTrain)
        Spacer(Modifier.height(16.dp))
        HomeButton(Icons.Default.CalendarMonth,          "Historial", "Calendario de entrenamientos guardados",    onGoToCalendar)
        Spacer(Modifier.height(16.dp))
        HomeButton(Icons.AutoMirrored.Filled.TrendingUp, "Progreso",  "E1RM, tendencias y marcas personales",      onGoToProgress)

        if (vm.savedSessions.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(20.dp))

            // CAMBIO 1: 5 stats simples, sin lógica best/worst
            val totalSeries   = vm.savedSessions.sumOf { it.sets.size }
            val totalReps     = vm.savedSessions.sumOf { s -> s.sets.sumOf { it.reps } }
            val totalVolume   = vm.savedSessions.sumOf { s -> s.sets.sumOf { (it.weightKg * it.reps).toDouble() } }.toInt()
            val distinctEx    = vm.savedSessions.flatMap { it.sets }.map { it.exerciseId }.distinct().size

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                MiniStat("Sesiones",    "${vm.savedSessions.size}")
                MiniStat("Series",      "$totalSeries")
                MiniStat("Reps",        "$totalReps")
            }
//            Spacer(Modifier.height(14.dp))
//            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
//                MiniStat("Ejercicios",  "$distinctEx")
//                MiniStat("Volumen",     "${totalVolume}kg")
//            }
        }
    }
}

@Composable
fun HomeButton(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), color = CardBg, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).background(Accent.copy(0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Accent, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title,    fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                Text(subtitle, fontSize = 13.sp, color = TextSec)
            }
            Icon(Icons.Default.ChevronRight, null, tint = TextTert)
        }
    }
}

@Composable
fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = Accent)
        Text(label, fontSize = 11.sp, color = TextSec)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISES SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExercisesScreen(vm: GymViewModel, onGoToSession: () -> Unit) {
    var dialogExercise by remember { mutableStateOf<Exercise?>(null) }

    Scaffold(
        containerColor = Black,
        floatingActionButton = {
            if (vm.sets.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onGoToSession, containerColor = Accent, contentColor = Black,
                    icon = { Icon(Icons.Default.PlayArrow, null) },
                    text = { Text("Ver sesión (${vm.sets.size})", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text("Ejercicios", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrim)
                if (vm.sets.isNotEmpty())
                    Text("${vm.sets.size} series hoy", fontSize = 13.sp, color = Accent, fontWeight = FontWeight.SemiBold)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                item { FilterChip("Músculo", vm.filterTab == 0) { vm.filterTab = 0 } }
                item { FilterChip("Rutina",  vm.filterTab == 1) { vm.filterTab = 1 } }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp), modifier = Modifier.padding(bottom = 12.dp)) {
                val list = if (vm.filterTab == 0) MUSCLES else ROUTINES
                items(list) { label ->
                    val sel = if (vm.filterTab == 0) vm.muscleFilter == label else vm.routineFilter == label
                    FilterChip(label, sel) {
                        if (vm.filterTab == 0) vm.muscleFilter = label else vm.routineFilter = label
                    }
                }
            }

            LazyVerticalGrid(columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()) {
                items(vm.filteredExercises, key = { it.id }) { exercise ->
                    ExerciseCard(exercise, vm.setsFor(exercise.id).size) { dialogExercise = exercise }
                }
            }
        }
    }

    dialogExercise?.let { ex ->
        LogSetDialog(ex, vm.setsFor(ex.id).lastOrNull(), onDismiss = { dialogExercise = null }) { reps, weight ->
            vm.logSet(ex, reps, weight); dialogExercise = null
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
    val sessionForSelected = selectedDate?.let { d -> vm.savedSessions.find { it.date == d } }

    Column(Modifier.fillMaxSize().background(Black)) {
        Text("Historial", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrim,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))

        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { displayMonth = displayMonth.minusMonths(1) }) { Icon(Icons.Default.ChevronLeft,  null, tint = TextPrim) }
            Text(displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")).replaceFirstChar { it.uppercase() },
                fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrim)
            IconButton(onClick = { displayMonth = displayMonth.plusMonths(1) })  { Icon(Icons.Default.ChevronRight, null, tint = TextPrim) }
        }

        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            listOf("L","M","X","J","V","S","D").forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 12.sp, color = TextSec, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(4.dp))

        CalendarGrid(displayMonth, vm.trainedDates, selectedDate) { date ->
            selectedDate = if (selectedDate == date) null else date
        }

        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = Border, modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(4.dp))

        when {
            selectedDate == null -> {
                if (vm.savedSessions.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Aún no hay sesiones guardadas", color = TextSec)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(vm.savedSessions.sortedByDescending { it.date }, key = { it.date }) { session ->
                            SessionHistoryCard(session) { selectedDate = session.date }
                        }
                    }
                }
            }
            sessionForSelected == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sin entrenamiento este día", color = TextSec)
                }
            }
            else -> {
                var showDeleteDialog by remember(selectedDate) { mutableStateOf(false) }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        containerColor = DarkSurf,
                        title = { Text("Eliminar sesión", color = TextPrim, fontWeight = FontWeight.Bold) },
                        text  = { Text("Se borrará todo el entrenamiento del ${formatDate(sessionForSelected.date)}. Esta acción no se puede deshacer.", color = TextSec) },
                        confirmButton = {
                            TextButton(onClick = {
                                vm.deleteSession(sessionForSelected.date, context)
                                selectedDate = null; showDeleteDialog = false
                            }) { Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar", color = TextSec) }
                        }
                    )
                }

                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(sessionForSelected.date), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                            Surface(onClick = { showDeleteDialog = true }, shape = RoundedCornerShape(10.dp),
                                color = Color.Red.copy(alpha = 0.12f), border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Delete, null, tint = Color.Red, modifier = Modifier.size(14.dp))
                                    Text("Borrar sesión", fontSize = 11.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatCard("Series", "${sessionForSelected.sets.size}", Modifier.weight(1f))
                            StatCard("Reps",   "${sessionForSelected.sets.sumOf { it.reps }}", Modifier.weight(1f))
                            StatCard("Vol.",   "${sessionForSelected.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()}kg", Modifier.weight(1f))
                        }
                    }
                    sessionForSelected.sets.groupBy { it.exerciseName }.forEach { (name, exSets) ->
                        item(key = name) {
                            ExerciseBlock(name, exSets, onDelete = { set ->
                                vm.deleteSetFromSession(sessionForSelected.date, set, context)
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarGrid(month: YearMonth, trainedDates: Set<String>, selectedDate: String?, onSelect: (String) -> Unit) {
    val startOffset = (month.atDay(1).dayOfWeek.value - 1) % 7
    val daysInMonth = month.lengthOfMonth()
    val rows = ((startOffset + daysInMonth) + 6) / 7

    Column(Modifier.padding(horizontal = 16.dp)) {
        repeat(rows) { row ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { col ->
                    val dayNum = row * 7 + col - startOffset + 1
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val dateStr  = month.atDay(dayNum).toString()
                            val trained  = dateStr in trainedDates
                            val selected = dateStr == selectedDate
                            val isToday  = dateStr == LocalDate.now().toString()
                            Box(
                                modifier = Modifier.size(36.dp)
                                    .background(when { selected -> Accent; trained -> Accent.copy(alpha = 0.25f); else -> Color.Transparent }, CircleShape)
                                    .then(if (isToday && !selected) Modifier.border(1.dp, Accent, CircleShape) else Modifier)
                                    .clickable { onSelect(dateStr) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$dayNum", fontSize = 13.sp,
                                    fontWeight = if (trained || selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) Black else if (isToday) Accent else TextPrim)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionHistoryCard(session: Session, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = CardBg, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(formatDate(session.date), fontWeight = FontWeight.Bold, color = TextPrim)
                Text(session.sets.map { it.exerciseName }.distinct().take(3).joinToString(", "),
                    fontSize = 12.sp, color = TextSec, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${session.sets.size} series", fontWeight = FontWeight.Bold, color = Accent)
                Text("${session.sets.sumOf { it.reps }} reps", fontSize = 12.sp, color = TextSec)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PROGRESS SCREEN — CAMBIO 2: botón export aquí arriba
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ProgressScreen(vm: GymViewModel) {
    val context = LocalContext.current
    var selectedExercise by remember { mutableStateOf<Exercise?>(null) }

    if (selectedExercise == null) {
        Column(Modifier.fillMaxSize().background(Black)) {
            // CAMBIO 2: header con botón export
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Progreso", fontSize = 28.sp, fontWeight = FontWeight.Black, color = TextPrim)
                if (vm.savedSessions.isNotEmpty()) {
                    Surface(
                        onClick = {
                            val uri = Storage.exportCSV(context, vm.savedSessions.toList())
                            uri?.let {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, it)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Exportar datos"))
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = CardBg,
                        border = BorderStroke(1.dp, Border),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📤", fontSize = 16.sp)
                            Text("Exportar CSV", fontSize = 12.sp, color = TextSec, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
                modifier = Modifier.padding(bottom = 14.dp, top = 10.dp)) {
                items(ROUTINES) { r ->
                    FilterChip(r, vm.progressRoutineFilter == r) { vm.progressRoutineFilter = r }
                }
            }

            if (vm.progressExercises.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        if (vm.savedSessions.isEmpty()) "Entrena y guarda sesiones\npara ver tu progreso"
                        else "Sin ejercicios para este filtro",
                        color = TextSec, textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vm.progressExercises, key = { it.id }) { ex ->
                        val trend    = vm.trendFor(ex.id)
                        val sessions = vm.e1rmProgressionFor(ex.id).size

                        Surface(onClick = { selectedExercise = ex }, shape = RoundedCornerShape(14.dp),
                            color = CardBg, border = BorderStroke(1.dp, Border)) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {

                                Box(Modifier.size(44.dp).background(ex.color.copy(0.12f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center) { Text(ex.emoji, fontSize = 22.sp) }

                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(ex.name, fontWeight = FontWeight.Bold, color = TextPrim)
                                        // CAMBIO 3: badge tipo ejercicio
                                        if (ex.isStrengthFocus) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(0.15f)) {
                                                Text("FUERZA", fontSize = 8.sp, color = Accent, fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                            }
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${ex.muscle} · $sessions ses.", fontSize = 12.sp, color = ex.color)
                                        trend?.let {
                                            val dotLabel = when (it.trend) {
                                                TrendState.PROGRESSING -> "🟢"
                                                TrendState.STAGNANT    -> "🟡"
                                                TrendState.FATIGUE     -> "🔴"
                                            }
                                            Text(dotLabel, fontSize = 10.sp)
                                        }
                                    }
                                }

                                // CAMBIO 3: métrica mostrada según tipo
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex.isStrengthFocus) {
                                        val e1rm = trend?.latestMetric ?: 0f
                                        Text("${e1rm.roundToInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 16.sp)
                                        Text("E1RM", fontSize = 11.sp, color = TextSec)
                                    } else {
                                        val score = trend?.latestMetric ?: 0f
                                        val bestSet = vm.historyFor(ex.id)
                                            .filter { vm.savedSessions.lastOrNull { s -> s.sets.any { it.exerciseId == ex.id } }?.date == it.first }
                                            .maxByOrNull { it.second.reps * it.second.weightKg }?.second
                                        if (bestSet != null) {
                                            Text("${bestSet.reps}r×${bestSet.weightKg.toInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                        } else {
                                            Text("${score.roundToInt()}", fontWeight = FontWeight.Black, color = Accent, fontSize = 16.sp)
                                        }
                                        Text("mejor set", fontSize = 11.sp, color = TextSec)
                                    }
                                    trend?.let {
                                        if (it.pctChange != 0f) {
                                            val sign = if (it.pctChange > 0) "+" else ""
                                            val c = if (it.pctChange > 0) GreenOk else RedBad
                                            Text("$sign${it.pctChange.roundToInt()}%", fontSize = 10.sp, color = c, fontWeight = FontWeight.Bold)
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
        ExerciseDetailScreen(vm, selectedExercise!!) { selectedExercise = null }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EXERCISE DETAIL — CAMBIO 3: adaptado según tipo fuerza/hipertrofia
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExerciseDetailScreen(vm: GymViewModel, exercise: Exercise, onBack: () -> Unit) {
    val isStrength = exercise.isStrengthFocus

    val e1rmData   = vm.e1rmProgressionFor(exercise.id)
    val hyData     = vm.hypertrophyProgressionFor(exercise.id)
    val volumeData = vm.volumeProgressionFor(exercise.id)
    val repsData   = vm.repsProgressionFor(exercise.id)
    val weightData = vm.weightProgressionFor(exercise.id)
    val byDate     = vm.historyFor(exercise.id).groupBy { it.first }
    val trend      = vm.trendFor(exercise.id)

    val maxWeight        = vm.prFor(exercise.id)
    val maxReps          = vm.maxRepsFor(exercise.id)
    val bestVolSession   = volumeData.maxOfOrNull { it.second } ?: 0f

    // CAMBIO 3: gráfica principal según tipo
    // Fuerza: 0=E1RM, 1=Vol, 2=Peso, 3=Reps
    // Hipertrofia: 0=MejorSet(reps×kg), 1=Vol, 2=Peso, 3=Reps
    var chartTab by remember { mutableStateOf(0) }

    LazyColumn(Modifier.fillMaxSize().background(Black), contentPadding = PaddingValues(bottom = 32.dp)) {

        // Header
        item {
            Row(Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 20.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrim) }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(exercise.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                        if (isStrength) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(0.15f)) {
                                Text("FUERZA", fontSize = 9.sp, color = Accent, fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text("${exercise.muscle} · ${exercise.routine}", fontSize = 13.sp, color = exercise.color)
                }
            }
        }

        // Trend status banner — CAMBIO 3: texto adaptado
        trend?.let { t ->
            item {
                val metricLabel = if (isStrength) "E1RM" else "mejor set"
                val metricValue = if (isStrength) "${t.latestMetric.roundToInt()}kg"
                else {
                    val lastDate = vm.savedSessions.lastOrNull { s -> s.sets.any { it.exerciseId == exercise.id } }?.date
                    val bestSet = vm.historyFor(exercise.id).filter { it.first == lastDate }
                        .maxByOrNull { it.second.reps * it.second.weightKg }?.second
                    if (bestSet != null) "${bestSet.reps}r×${bestSet.weightKg.toInt()}kg" else "${t.latestMetric.roundToInt()}"
                }

                val (bgColor, icon, statusText, subText) = when (t.trend) {
                    TrendState.PROGRESSING -> listOf(GreenOk.copy(0.1f), "🟢", "Progresando", "+${t.pctChange.roundToInt()}% vs media últimas 5 sesiones")
                    TrendState.STAGNANT    -> listOf(YellowWarn.copy(0.1f), "🟡", "Estancado", "${t.pctChange.roundToInt()}% vs media últimas 5 sesiones")
                    TrendState.FATIGUE     -> listOf(RedBad.copy(0.1f), "🔴", "Posible fatiga", "${t.pctChange.roundToInt()}% vs media últimas 5 sesiones")
                }
                val borderColor = when (t.trend) {
                    TrendState.PROGRESSING -> GreenOk.copy(0.4f)
                    TrendState.STAGNANT    -> YellowWarn.copy(0.4f)
                    TrendState.FATIGUE     -> RedBad.copy(0.4f)
                }
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = bgColor as Color,
                    border = BorderStroke(1.dp, borderColor as Color)) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(icon as String, fontSize = 22.sp)
                        Column {
                            Text(statusText as String, fontWeight = FontWeight.Bold, color = TextPrim, fontSize = 14.sp)
                            Text(subText as String, fontSize = 12.sp, color = TextSec)
                        }
                        Spacer(Modifier.weight(1f))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(metricValue, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Accent)
                            Text(metricLabel, fontSize = 10.sp, color = TextSec)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // PR Cards — CAMBIO 3: fuerza muestra E1RM, hipertrofia muestra mejor set
        item {
            Text("RÉCORDS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = TextSec, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(8.dp))
            if (isStrength) {
                val allTimeE1RM = vm.allTimeE1RMFor(exercise.id)
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRCard("🏆 E1RM", "${allTimeE1RM.roundToInt()} kg", "Histórico", Modifier.weight(1f))
                    PRCard("🏋️ Peso", "${maxWeight} kg", "Máximo", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRCard("🔁 Reps", "$maxReps", "Máximo total", Modifier.weight(1f))
                    PRCard("📈 Vol.", "${bestVolSession.roundToInt()} kg", "Mejor sesión", Modifier.weight(1f))
                }
            } else {
                // Hipertrofia: mejor set (reps×kg), peso máx, reps máx, volumen
                val bestScore = vm.allTimeHypertrophyScoreFor(exercise.id)
                val bestSetEver = vm.historyFor(exercise.id)
                    .maxByOrNull { it.second.reps * it.second.weightKg }?.second
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRCard("🏆 Mejor set",
                        if (bestSetEver != null) "${bestSetEver.reps}r×${bestSetEver.weightKg.toInt()}kg" else "—",
                        "reps × peso", Modifier.weight(1f))
                    PRCard("🏋️ Peso", "${maxWeight} kg", "Máximo", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PRCard("🔁 Reps", "$maxReps", "Máximo total", Modifier.weight(1f))
                    PRCard("📈 Vol.", "${bestVolSession.roundToInt()} kg", "Mejor sesión", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Chart selector tabs — CAMBIO 3: primera tab distinta según tipo
        item {
            Text("GRÁFICAS", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = TextSec, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                FilterChipFlex(if (isStrength) "E1RM" else "Mejor set", chartTab == 0, Modifier.weight(1f)) { chartTab = 0 }
                FilterChipFlex("Vol.",   chartTab == 1, Modifier.weight(1f)) { chartTab = 1 }
                FilterChipFlex("Peso",   chartTab == 2, Modifier.weight(1f)) { chartTab = 2 }
                FilterChipFlex("Reps",   chartTab == 3, Modifier.weight(1f)) { chartTab = 3 }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Main chart — CAMBIO 3: tab 0 usa e1rm para fuerza, hyData para hipertrofia
        item {
            val data  = when (chartTab) {
                0    -> if (isStrength) e1rmData else hyData
                1    -> volumeData
                2    -> weightData
                else -> repsData
            }
            val unit  = when (chartTab) {
                0    -> if (isStrength) "kg" else ""
                1    -> "kg·r"
                2    -> "kg"
                else -> "r"
            }
            val label = when (chartTab) {
                0    -> if (isStrength) "E1RM estimado por sesión (mejor set)"
                else "Mejor set (reps × peso) por sesión"
                1    -> "Volumen total por sesión (kg × reps)"
                2    -> "Peso máximo por sesión"
                else -> "Reps totales por sesión"
            }

            Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp), color = CardBg) {
                Column(Modifier.padding(16.dp)) {
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSec,
                        modifier = Modifier.padding(bottom = 10.dp))
                    if (data.size < 2) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("Necesitas al menos 2 sesiones\npara ver la gráfica",
                                color = TextSec, textAlign = TextAlign.Center, fontSize = 13.sp)
                        }
                    } else {
                        LineChart(data, exercise.color, unit, Modifier.fillMaxWidth().height(160.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Volume bar chart
        if (volumeData.size >= 2) {
            item {
                Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp), color = CardBg) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Volumen por sesión — barras", fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = TextSec, modifier = Modifier.padding(bottom = 10.dp))
                        BarChart(volumeData, exercise.color, Modifier.fillMaxWidth().height(110.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // Full log — CAMBIO 3: hipertrofia no muestra E1RM, muestra reps×kg score
        item {
            Text("HISTORIAL COMPLETO", fontSize = 11.sp, fontWeight = FontWeight.Bold,
                color = TextSec, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        }

        byDate.entries.sortedByDescending { it.key }.forEach { (date, entries) ->
            item(key = date) {
                val sessionSets     = entries.map { it.second }
                val bestSetE1RM     = bestE1RM(sessionSets)
                val bestHyScore     = bestHypertrophyScore(sessionSets)
                val sessionVol      = sessionSets.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()
                val bestHySet       = sessionSets.maxByOrNull { it.reps * it.weightKg }

                Surface(shape = RoundedCornerShape(14.dp), color = CardBg,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(formatDate(date), fontWeight = FontWeight.Bold, color = TextPrim)
                            Column(horizontalAlignment = Alignment.End) {
                                if (isStrength) {
                                    Text("E1RM ${bestSetE1RM.roundToInt()}kg", fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold)
                                } else {
                                    // CAMBIO 3: mostrar mejor set como "Xr×Ykg"
                                    Text(
                                        if (bestHySet != null) "Mejor ${bestHySet.reps}r×${bestHySet.weightKg.toInt()}kg"
                                        else "",
                                        fontSize = 11.sp, color = Accent, fontWeight = FontWeight.Bold
                                    )
                                }
                                Text("Vol. ${sessionVol}kg", fontSize = 10.sp, color = TextSec)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        entries.forEachIndexed { i, (_, set) ->
                            val setE1RM    = estimatedOneRM(set.weightKg, set.reps)
                            val setHyScore = set.reps * set.weightKg
                            val isTopSet   = if (isStrength) (setE1RM == bestSetE1RM && bestSetE1RM > 0f)
                            else (setHyScore == bestHyScore && bestHyScore > 0f)
                            val isPRWeight = set.weightKg == maxWeight && maxWeight > 0f

                            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(24.dp).background(Border, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                                    Text("${i+1}", fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("${set.reps} reps", fontSize = 14.sp, color = TextPrim, modifier = Modifier.weight(1f))
                                Text(
                                    if (set.weightKg == 0f) "Peso corporal" else "${set.weightKg}kg",
                                    fontSize = 14.sp,
                                    color = if (isPRWeight) Accent else TextSec,
                                    fontWeight = if (isPRWeight) FontWeight.Black else FontWeight.Normal
                                )
                                Spacer(Modifier.width(6.dp))
                                if (isStrength) {
                                    Text("→${setE1RM.roundToInt()}", fontSize = 11.sp, color = if (isTopSet) Accent else TextTert)
                                } else {
                                    // CAMBIO 3: score reps×kg en lugar de E1RM
                                    Text("=${setHyScore.roundToInt()}", fontSize = 11.sp, color = if (isTopSet) Accent else TextTert)
                                }
                                if (isTopSet) { Spacer(Modifier.width(3.dp)); Text("★", fontSize = 11.sp, color = Accent) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PR CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PRCard(label: String, value: String, sublabel: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = CardBg,
        border = BorderStroke(1.dp, Accent.copy(0.2f))) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = TextSec, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black, color = Accent)
            Text(sublabel, fontSize = 10.sp, color = TextTert)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHARTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun LineChart(
    data: List<Pair<String, Float>>,
    color: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return
    val values = data.map { it.second }
    val minV   = values.min()
    val maxV   = values.max()
    val range  = if (maxV == minV) 1f else maxV - minV
    val maxIdx = values.indexOf(maxV)

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w     = size.width
            val h     = size.height
            val padT  = 16f
            val padB  = 28f
            val drawH = h - padT - padB
            val stepX = w / (data.size - 1).toFloat()

            fun xAt(i: Int)   = i * stepX
            fun yAt(v: Float) = padT + drawH * (1f - (v - minV) / range)

            val fillPath = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) {
                    val cx = (xAt(i - 1) + xAt(i)) / 2f
                    cubicTo(cx, yAt(values[i-1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
                }
                lineTo(xAt(data.size - 1), h); lineTo(xAt(0), h); close()
            }
            drawPath(fillPath, Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.4f), Color.Transparent),
                startY = padT, endY = h))

            val linePath = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) {
                    val cx = (xAt(i - 1) + xAt(i)) / 2f
                    cubicTo(cx, yAt(values[i-1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
                }
            }
            drawPath(linePath, color, style = Stroke(width = 3f, cap = StrokeCap.Round))

            data.indices.forEach { i ->
                val isMax = i == maxIdx
                drawCircle(color, radius = if (isMax) 7f else 5f, center = Offset(xAt(i), yAt(values[i])))
                drawCircle(Color(0xFF242426), radius = if (isMax) 4f else 3f, center = Offset(xAt(i), yAt(values[i])))
            }
        }

        Row(Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${values.first().roundToInt()}$unit", fontSize = 10.sp, color = TextSec)
            if (maxIdx != 0 && maxIdx != data.size - 1)
                Text("${maxV.roundToInt()}$unit ★", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
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
        val w    = size.width
        val h    = size.height - 4f
        val barW = (w / data.size) * 0.55f
        val gap  = (w / data.size) * 0.45f

        data.forEachIndexed { i, (_, value) ->
            val barH  = (value / maxV) * h
            val left  = i * (barW + gap) + gap / 2f
            val top   = h - barH
            val alpha = if (value == maxVal) 1f else 0.45f
            drawRoundRect(
                color        = color.copy(alpha = alpha),
                topLeft      = Offset(left, top),
                size         = Size(barW, barH),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SESSION SCREEN
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(vm: GymViewModel, onBack: () -> Unit, onSave: () -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(vm.sessionDate).toEpochDay() * 86_400_000L
            } catch (e: Exception) { System.currentTimeMillis() }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        vm.sessionDate = LocalDate.ofEpochDay(millis / 86_400_000L).toString()
                    }
                    showDatePicker = false
                }) { Text("OK", color = Accent) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar", color = TextSec) } },
            colors = DatePickerDefaults.colors(containerColor = DarkSurf)
        ) {
            DatePicker(state = pickerState, colors = DatePickerDefaults.colors(
                containerColor = DarkSurf, titleContentColor = TextPrim,
                headlineContentColor = Accent, weekdayContentColor = TextSec,
                dayContentColor = TextPrim, selectedDayContainerColor = Accent,
                selectedDayContentColor = Black, todayContentColor = Accent,
                todayDateBorderColor = Accent))
        }
    }

    Scaffold(
        containerColor = Black,
        topBar = {
            TopAppBar(
                title = { Text("SESIÓN", fontWeight = FontWeight.Bold, color = TextPrim) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrim) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Black)
            )
        },
        bottomBar = {
            if (vm.sets.isNotEmpty()) {
                Box(Modifier.padding(16.dp)) {
                    Button(onClick = onSave, modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                        Icon(Icons.Default.Save, null); Spacer(Modifier.width(8.dp))
                        Text("GUARDAR SESIÓN", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

            item {
                Surface(onClick = { showDatePicker = true }, shape = RoundedCornerShape(14.dp),
                    color = CardBg, border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CalendarMonth, null, tint = Accent, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Fecha de la sesión", fontSize = 11.sp, color = TextSec)
                                Text(
                                    if (vm.sessionDate == LocalDate.now().toString()) "Hoy - ${formatDate(vm.sessionDate)}"
                                    else formatDate(vm.sessionDate),
                                    fontWeight = FontWeight.Bold, color = TextPrim
                                )
                            }
                        }
                        Text("Cambiar", fontSize = 12.sp, color = Accent, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (vm.sets.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                        Text("Sin series registradas aún", color = TextSec)
                    }
                }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatCard("SERIES", "${vm.sets.size}",             Modifier.weight(1f))
                        StatCard("REPS",   "${vm.totalReps}",             Modifier.weight(1f))
                        StatCard("VOL.",   "${vm.totalVolume.toInt()}kg", Modifier.weight(1f))
                    }
                }
                vm.groupedSets.forEach { (name, exSets) ->
                    item(key = name) { ExerciseBlock(name, exSets, onDelete = { vm.deleteSet(it) }) }
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

    Scaffold(
        containerColor = Black,
        bottomBar = {
            Box(Modifier.padding(16.dp)) {
                Button(onClick = onBack, modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                    Text("VOLVER AL INICIO", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            item {
                Spacer(Modifier.height(8.dp))
                Icon(Icons.Default.EmojiEvents, null, tint = Accent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text("¡SESIÓN GUARDADA!", fontSize = 26.sp, fontWeight = FontWeight.Black,
                    color = TextPrim, textAlign = TextAlign.Center)
                lastSession?.let { Text(formatDate(it.date), fontSize = 14.sp, color = TextSec, modifier = Modifier.padding(top = 4.dp)) }
            }
            lastSession?.let { session ->
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        BigStat("SERIES", "${session.sets.size}", Modifier.weight(1f))
                        BigStat("REPS",   "${session.sets.sumOf { it.reps }}", Modifier.weight(1f))
                    }
                }
                item { BigStat("VOLUMEN TOTAL", "${session.sets.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()} KG", Modifier.fillMaxWidth()) }
                item { Text("POR EJERCICIO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSec, modifier = Modifier.fillMaxWidth()) }
                session.sets.groupBy { it.exerciseName }.forEach { (name, exSets) ->
                    item(key = name) {
                        val ex = EXERCISES.find { it.name == name }
                        Surface(shape = RoundedCornerShape(14.dp), color = CardBg) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, fontWeight = FontWeight.SemiBold, color = TextPrim)
                                    Text("${exSets.size} series - ${exSets.sumOf { it.reps }} reps", fontSize = 13.sp, color = TextSec)
                                }
                                // CAMBIO 3: summary muestra métrica según tipo
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex?.isStrengthFocus == true) {
                                        val bestE1RM = bestE1RM(exSets)
                                        Text("E1RM ${bestE1RM.roundToInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 15.sp)
                                        Text("${exSets.maxOf { it.weightKg }}kg max", fontSize = 11.sp, color = TextSec)
                                    } else {
                                        val bestSet = exSets.maxByOrNull { it.reps * it.weightKg }
                                        if (bestSet != null) {
                                            Text("${bestSet.reps}r×${bestSet.weightKg.toInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 15.sp)
                                        }
                                        Text("${exSets.maxOf { it.weightKg }}kg max", fontSize = 11.sp, color = TextSec)
                                    }
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
// SHARED COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ExerciseCard(exercise: Exercise, setCount: Int, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.aspectRatio(0.85f),
        shape = RoundedCornerShape(16.dp), color = CardBg, border = BorderStroke(1.dp, Border)) {
        Box {
            Box(Modifier.fillMaxWidth().height(4.dp).background(exercise.color))
            Column(Modifier.fillMaxSize().padding(top = 12.dp, start = 12.dp, end = 12.dp, bottom = 10.dp),
                verticalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.fillMaxWidth().weight(1f).background(exercise.color.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center) { Text(exercise.emoji, fontSize = 30.sp) }
                Spacer(Modifier.height(6.dp))
                Text(exercise.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrim,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.muscle.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = exercise.color)
                    if (setCount > 0) {
                        Surface(shape = CircleShape, color = Accent) {
                            Text("$setCount", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                fontSize = 10.sp, fontWeight = FontWeight.Black, color = Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.height(34.dp), shape = RoundedCornerShape(50),
        color = if (selected) Accent else CardBg,
        border = if (selected) null else BorderStroke(1.dp, Border)) {
        Box(Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (selected) Black else TextSec)
        }
    }
}

@Composable
fun FilterChipFlex(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.height(34.dp), shape = RoundedCornerShape(50),
        color = if (selected) Accent else CardBg,
        border = if (selected) null else BorderStroke(1.dp, Border)) {
        Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Black else TextSec,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun LogSetDialog(exercise: Exercise, lastSet: WorkoutSet?, onDismiss: () -> Unit, onSave: (Int, Float) -> Unit) {
    var repsText   by remember { mutableStateOf(lastSet?.reps?.toString() ?: "") }
    var weightText by remember { mutableStateOf(lastSet?.weightKg?.let {
        if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() } ?: "") }
    var repsError by remember { mutableStateOf(false) }

    // CAMBIO 3: preview según tipo ejercicio
    val isStrength = exercise.isStrengthFocus
    val previewE1RM = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0
        val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && isStrength) estimatedOneRM(w, r) else null
    }
    val previewHyScore = remember(repsText, weightText) {
        val r = repsText.trim().toIntOrNull() ?: 0
        val w = weightText.trim().toFloatOrNull() ?: 0f
        if (r > 0 && w > 0f && !isStrength) r * w else null
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = DarkSurf) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrim)
                            if (isStrength) {
                                Surface(shape = RoundedCornerShape(4.dp), color = Accent.copy(0.15f)) {
                                    Text("FUERZA", fontSize = 8.sp, color = Accent, fontWeight = FontWeight.Black,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                                }
                            }
                        }
                        Text(exercise.muscle, fontSize = 13.sp, color = exercise.color)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                }
                if (lastSet != null) {
                    Box(Modifier.fillMaxWidth().background(exercise.color.copy(0.1f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        if (isStrength) {
                            val lastE1RM = estimatedOneRM(lastSet.weightKg, lastSet.reps)
                            Text("Última vez: ${lastSet.reps}r × ${lastSet.weightKg}kg → E1RM ${lastE1RM.roundToInt()}kg",
                                fontSize = 13.sp, color = exercise.color)
                        } else {
                            val lastScore = lastSet.reps * lastSet.weightKg
                            Text("Última vez: ${lastSet.reps}r × ${lastSet.weightKg}kg (score ${lastScore.roundToInt()})",
                                fontSize = 13.sp, color = exercise.color)
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nº REPETICIONES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSec)
                    OutlinedTextField(value = repsText, onValueChange = { repsText = it; repsError = false },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                        isError = repsError, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                            focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                            cursorColor = exercise.color, focusedContainerColor = CardBg, unfocusedContainerColor = CardBg))
                    if (repsError) Text("Introduce un número válido", fontSize = 11.sp, color = Color.Red)
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PESO (KG)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSec)
                    OutlinedTextField(value = weightText, onValueChange = { weightText = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("0", color = TextTert) },
                        singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                            focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                            cursorColor = exercise.color, focusedContainerColor = CardBg, unfocusedContainerColor = CardBg))
                }
                // CAMBIO 3: preview de métrica según tipo
                previewE1RM?.let { e ->
                    Box(Modifier.fillMaxWidth().background(Accent.copy(0.08f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("E1RM estimado", fontSize = 13.sp, color = TextSec)
                            Text("${e.roundToInt()} kg", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Black)
                        }
                    }
                }
                previewHyScore?.let { s ->
                    Box(Modifier.fillMaxWidth().background(exercise.color.copy(0.08f), RoundedCornerShape(10.dp)).padding(10.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Score reps×peso", fontSize = 13.sp, color = TextSec)
                            Text("${s.roundToInt()}", fontSize = 14.sp, color = exercise.color, fontWeight = FontWeight.Black)
                        }
                    }
                }
                Button(onClick = {
                    val reps = repsText.trim().toIntOrNull()
                    if (reps == null || reps <= 0) { repsError = true; return@Button }
                    onSave(reps, weightText.trim().toFloatOrNull() ?: 0f)
                }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                    Text("GUARDAR SERIE", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ExerciseBlock(name: String, sets: List<WorkoutSet>, onDelete: ((WorkoutSet) -> Unit)?) {
    Surface(shape = RoundedCornerShape(16.dp), color = CardBg) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrim)
            Text("${sets.size} series · ${sets.sumOf { it.reps }} reps · max ${sets.maxOf { it.weightKg }}kg",
                fontSize = 13.sp, color = TextSec)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(6.dp))
            sets.forEachIndexed { i, set ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(26.dp).background(Border, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                        Text("${i+1}", fontSize = 11.sp, color = TextSec, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("${set.reps} reps", fontSize = 15.sp, color = TextPrim, modifier = Modifier.weight(1f))
                    Text(if (set.weightKg == 0f) "Peso corporal" else "${set.weightKg}kg",
                        fontSize = 15.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                    if (onDelete != null) {
                        IconButton(onClick = { onDelete(set) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Delete, null, tint = TextTert, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(12.dp), color = CardBg) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Accent)
            Text(label, fontSize = 11.sp, color = TextSec, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun BigStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = CardBg) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Black, fontSize = 28.sp, color = Accent)
            Text(label, fontSize = 11.sp, color = TextSec, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UTILS
// ─────────────────────────────────────────────────────────────────────────────

fun formatDate(dateStr: String): String = try {
    LocalDate.parse(dateStr).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) { dateStr }