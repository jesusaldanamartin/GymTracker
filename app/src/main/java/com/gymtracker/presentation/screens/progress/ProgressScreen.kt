package com.gymtracker.presentation.screens.progress

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.gymtracker.data.local.SeedData.ROUTINES
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.ExerciseTrend
import com.gymtracker.domain.model.ImportResult
import com.gymtracker.domain.model.TrendState
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.bestE1RM
import com.gymtracker.domain.model.bestHypertrophyScore
import com.gymtracker.domain.model.estimatedOneRM
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Blue
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.GreenOk
import com.gymtracker.presentation.theme.OrangeStk
import com.gymtracker.presentation.theme.Purple
import com.gymtracker.presentation.theme.RedBad
import com.gymtracker.presentation.theme.Surface0
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.Surface2
import com.gymtracker.presentation.theme.Surface3
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import com.gymtracker.presentation.theme.TextTert
import com.gymtracker.presentation.theme.YellowWarn
import kotlin.math.roundToInt
import com.gymtracker.presentation.components.BarChartExpanded
import com.gymtracker.presentation.components.BarChartGrouped
import com.gymtracker.presentation.components.LineChartEnhanced
import com.gymtracker.presentation.components.ConsistencyCard
import com.gymtracker.presentation.components.Chip
import com.gymtracker.presentation.components.ChipFlex
import com.gymtracker.presentation.components.EmptyState
import com.gymtracker.presentation.components.ImportStatRow
import com.gymtracker.presentation.components.SectionLabel
import com.gymtracker.presentation.components.TypeBadge
import com.gymtracker.presentation.components.formatDate
import com.gymtracker.presentation.components.ConfigureVariantsDialog
import com.gymtracker.presentation.components.VariantChip
import com.gymtracker.presentation.screens.GymViewModel

@Composable
fun ProgressScreen(vm: GymViewModel) {
    val context = LocalContext.current
    var selectedEx    by remember { mutableStateOf<Exercise?>(null) }
    var exportResult  by remember { mutableStateOf<String?>(null) }
    var importResult  by remember { mutableStateOf<ImportResult?>(null) }
    var importPending by remember { mutableStateOf<ImportResult.Success?>(null) }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val result = vm.importFromCsv(context, it)
            when (result) {
                is ImportResult.Success -> importPending = result
                is ImportResult.Error   -> importResult  = result
            }
        }
    }

    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) exportResult = vm.exportToDownloads(context) ?: ""
        else {
            vm.exportForShare(context)?.let { uri ->
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Exportar CSV"))
            }
        }
    }

    fun doDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            exportResult = vm.exportToDownloads(context) ?: ""
        else {
            val perm = android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED)
                exportResult = vm.exportToDownloads(context) ?: ""
            else permLauncher.launch(perm)
        }
    }

    fun doShare() {
        vm.exportForShare(context)?.let { uri ->
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
                        ImportStatRow("Ejercicios creados", "${s.newCustomExercises.count { it.isCustom }}", Blue)
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
                                        if (ex.isCardio)        TypeBadge("CARDIO", Color(0xFFE63946))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("${ex.muscle} · $sessions ses.", fontSize = 12.sp, color = ex.color)
                                        trend?.let { Text(when(it.trend) { TrendState.PROGRESSING -> "🟢"; TrendState.STAGNANT -> "🟡"; TrendState.FATIGUE -> "🟠" }, fontSize = 10.sp) }
                                    }
                                    val variants = vm.knownVariantsFor(ex.id)
                                    if (variants.isNotEmpty()) {
                                        Spacer(Modifier.height(4.dp))
                                        LazyRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            items(variants.take(4)) { v ->
                                                Surface(shape = RoundedCornerShape(4.dp), color = Purple.copy(0.1f),
                                                    border = BorderStroke(0.5.dp, Purple.copy(0.2f))) {
                                                    Text(v, fontSize = 8.sp, color = Purple,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                                }
                                            }
                                            if (variants.size > 4) item {
                                                Text("+${variants.size - 4}", fontSize = 8.sp, color = TextTert,
                                                    modifier = Modifier.padding(vertical = 2.dp))
                                            }
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex.isCardio) {
                                        val lastDuration = vm.durationProgressionFor(ex.id).lastOrNull()?.second?.toInt() ?: 0
                                        val lastIntensity = vm.intensityProgressionFor(ex.id).lastOrNull()?.second
                                        Text("${lastDuration}min", fontWeight = FontWeight.Black, color = Accent, fontSize = 15.sp)
                                        if (lastIntensity != null && lastIntensity > 0f)
                                            Text("Int. ${lastIntensity.toInt()}/10", fontSize = 10.sp, color = TextSec)
                                        else
                                            Text("duración", fontSize = 10.sp, color = TextSec)
                                    } else if (ex.isStrengthFocus) {
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

@Composable
fun ExerciseDetailScreen(vm: GymViewModel, exercise: Exercise, onBack: () -> Unit) {
    val isS = exercise.isStrengthFocus
    val isC = exercise.isCardio

    var selectedVariant by remember { mutableStateOf("") }
    val knownVariants   = remember(vm.savedSessions.size) { vm.knownVariantsFor(exercise.id) }

    val context = LocalContext.current
    var showConfigVariants by remember { mutableStateOf(false) }

    if (showConfigVariants) {
        ConfigureVariantsDialog(
            exercise        = exercise,
            currentVariants = vm.variantsFor(exercise.id),
            onDismiss       = { showConfigVariants = false },
            onSave          = { newList: List<String> ->
                vm.setVariants(exercise.id, newList, context)
                showConfigVariants = false
            }
        )
    }

    val e1rmData  = vm.e1rmProgressionFor(exercise.id, selectedVariant)
    val hyData    = vm.hypertrophyProgressionFor(exercise.id, selectedVariant)
    val volData   = vm.volumeProgressionFor(exercise.id, selectedVariant)
    val repsData  = vm.repsProgressionFor(exercise.id, selectedVariant)
    val wData     = vm.weightProgressionFor(exercise.id, selectedVariant)
    val durData   = vm.durationProgressionFor(exercise.id, selectedVariant)
    val intData   = vm.intensityProgressionFor(exercise.id, selectedVariant)
    val byDate    = vm.historyForVariant(exercise.id, selectedVariant).groupBy { it.first }
    val trend     = vm.trendFor(exercise.id, selectedVariant)

    val allHistory    = vm.historyForVariant(exercise.id, selectedVariant)
    val maxWeight     = allHistory.maxOfOrNull { it.second.weightKg } ?: 0f
    val maxReps       = allHistory.maxOfOrNull { it.second.reps } ?: 0
    val totalSessions = if (isC) durData.size else e1rmData.size

    var chartTab by remember { mutableStateOf(0) }
    var sessionViewTab by remember { mutableStateOf(0) }

    val chartData: List<Pair<String, Float>>
    val chartUnit: String
    val chartLabel: String
    if (isC) {
        chartData  = if (chartTab == 0) durData else intData
        chartUnit  = if (chartTab == 0) "min" else "/10"
        chartLabel = if (chartTab == 0) "Duración por sesión (min)" else "Intensidad por sesión (/10)"
    } else {
        chartData  = when (chartTab) { 0 -> if (isS) e1rmData else hyData; 1 -> volData; 2 -> wData; else -> repsData }
        chartUnit  = when (chartTab) { 0 -> if (isS) "kg" else ""; 1 -> "kg"; 2 -> "kg"; else -> "r" }
        chartLabel = when (chartTab) { 0 -> if (isS) "E1RM estimado" else "Mejor set"; 1 -> "Volumen total"; 2 -> "Peso máximo"; else -> "Reps totales" }
    }

    LazyColumn(
        Modifier.fillMaxSize().background(Surface0),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, end = 12.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = TextPrim)
                }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrim)
                        if (isS && !isC) TypeBadge("FUERZA", Accent)
                        if (isC)         TypeBadge("CARDIO", Color(0xFFE63946))
                        if (exercise.isCustom) TypeBadge("CUSTOM", Blue)
                    }
                    Text("${exercise.muscle} · ${exercise.routine}", fontSize = 12.sp, color = exercise.color)
                }
                Surface(
                    onClick = { showConfigVariants = true },
                    shape = RoundedCornerShape(10.dp),
                    color = Purple.copy(0.08f),
                    border = BorderStroke(1.dp, Purple.copy(0.25f)),
                    modifier = Modifier.height(34.dp)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Default.Tune, null, tint = Purple, modifier = Modifier.size(13.dp))
                        Text("Variantes", fontSize = 10.sp, color = Purple, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (knownVariants.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(6.dp).background(Purple, CircleShape))
                        Text("MODO / VARIANTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextTert, letterSpacing = 0.8.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            VariantChip(label = "Todas", selected = selectedVariant.isEmpty()) { selectedVariant = "" }
                        }
                        items(knownVariants) { v ->
                            VariantChip(label = v, selected = selectedVariant == v) {
                                selectedVariant = if (selectedVariant == v) "" else v
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        item {
            HeroMetricCard(
                exercise        = exercise,
                trend           = trend,
                isS             = isS,
                isC             = isC,
                e1rmData        = e1rmData,
                durData         = durData,
                volData         = volData,
                selectedVariant = selectedVariant,
                vm              = vm,
                modifier        = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(10.dp))
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isC) {
                    MiniStatCard("⏱", "Duración máx.", "${durData.maxOfOrNull { it.second }?.toInt() ?: 0}min", Modifier.weight(1f))
                    MiniStatCard("🔥", "Int. máxima", "${intData.maxOfOrNull { it.second }?.toInt() ?: 0}/10", Modifier.weight(1f))
                    MiniStatCard("📅", "Sesiones", "$totalSessions", Modifier.weight(1f))
                } else {
                    MiniStatCard("🏋️", "Peso máx.", "${maxWeight}kg", Modifier.weight(1f))
                    MiniStatCard("📅", "Sesiones", "$totalSessions", Modifier.weight(1f))
                    MiniStatCard("🔁", "Mejor reps", "$maxReps", Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            SectionLabel("PROGRESIÓN DETALLADA")
            if (isC) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFlex("Duración", chartTab == 0, Modifier.weight(1f)) { chartTab = 0 }
                    ChipFlex("Intensidad", chartTab == 1, Modifier.weight(1f)) { chartTab = 1 }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ChipFlex(if (isS) "E1RM" else "Mejor set", chartTab == 0, Modifier.weight(1f)) { chartTab = 0 }
                    ChipFlex("Vol.",  chartTab == 1, Modifier.weight(1f)) { chartTab = 1 }
                    ChipFlex("Peso",  chartTab == 2, Modifier.weight(1f)) { chartTab = 2 }
                    ChipFlex("Reps",  chartTab == 3, Modifier.weight(1f)) { chartTab = 3 }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        item {
            Surface(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = Surface1
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(chartLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSec,
                        modifier = Modifier.padding(bottom = 10.dp))
                    if (chartData.size < 2) {
                        Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                            Text("Necesitas al menos 2 sesiones\npara ver la gráfica",
                                color = TextSec, textAlign = TextAlign.Center, fontSize = 12.sp)
                        }
                    } else {
                        LineChartEnhanced(chartData, exercise.color, chartUnit, Modifier.fillMaxWidth().height(160.dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (!isC && (volData.size >= 2 || wData.size >= 2)) {
            item {
                SectionLabel("POR SESIÓN")
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChipFlex("Reps / Peso", sessionViewTab == 0, Modifier.weight(1f)) { sessionViewTab = 0 }
                    ChipFlex("Volumen",     sessionViewTab == 1, Modifier.weight(1f)) { sessionViewTab = 1 }
                }
                Spacer(Modifier.height(10.dp))
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Surface1
                ) {
                    Column(Modifier.padding(14.dp)) {
                        if (sessionViewTab == 0) {
                            val bestSetPerSession = vm.savedSessions
                                .filter { s -> s.sets.any { it.exerciseId == exercise.id } }
                                .sortedBy { it.date }
                                .mapNotNull { s ->
                                    val sets = s.sets.filter {
                                        it.exerciseId == exercise.id &&
                                                (selectedVariant.isBlank() || it.variant == selectedVariant)
                                    }
                                    val best = sets.maxByOrNull { it.reps * it.weightKg }
                                    if (best != null) s.date to best else null
                                }
                            if (bestSetPerSession.size >= 2) {
                                BarChartGrouped(
                                    sessionData = bestSetPerSession,
                                    colorReps   = Color(0xFF4ECDC4),
                                    colorWeight = exercise.color,
                                    modifier    = Modifier.fillMaxWidth()
                                )
                            } else {
                                Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                                    Text("Necesitas al menos 2 sesiones", color = TextSec, fontSize = 12.sp)
                                }
                            }
                        } else {
                            if (volData.size >= 2) {
                                BarChartExpanded(data = volData, color = exercise.color, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (isC && durData.size >= 2) {
            item {
                SectionLabel("DURACIÓN POR SESIÓN")
                Surface(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Surface1
                ) {
                    Column(Modifier.padding(16.dp)) {
                        BarChartExpanded(data = durData, color = exercise.color, modifier = Modifier.fillMaxWidth().height(100.dp))
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        item {
            SectionLabel("CONSISTENCIA")
            ConsistencyCard(
                sessions   = vm.savedSessions.toList(),
                exerciseId = exercise.id,
                color      = exercise.color,
                modifier   = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(16.dp))
        }

        item { SectionLabel("HISTORIAL COMPLETO${if (selectedVariant.isNotEmpty()) " · $selectedVariant" else ""}") }

        val sortedHistory = byDate.entries.sortedByDescending { it.key }
        sortedHistory.forEachIndexed { idx, (date, entries) ->
            item(key = "$date-$selectedVariant") {
                CollapsibleHistoryEntry(
                    date               = date,
                    entries            = entries,
                    isS                = isS,
                    isC                = isC,
                    maxWeight          = maxWeight,
                    isInitiallyExpanded = idx == 0
                )
            }
        }
    }
}

@Composable
fun MiniStatCard(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape    = RoundedCornerShape(14.dp),
        color    = Surface1,
        border   = BorderStroke(1.dp, Border)
    ) {
        Column(
            Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Accent, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, fontSize = 9.sp, color = TextSec, textAlign = TextAlign.Center, lineHeight = 12.sp)
        }
    }
}

@Composable
fun HeroMetricCard(
    exercise: Exercise,
    trend: ExerciseTrend?,
    isS: Boolean,
    isC: Boolean,
    e1rmData: List<Pair<String, Float>>,
    durData: List<Pair<String, Float>>,
    volData: List<Pair<String, Float>>,
    selectedVariant: String,
    vm: GymViewModel,
    modifier: Modifier = Modifier
) {
    val trendColor = when (trend?.trend) {
        TrendState.PROGRESSING -> GreenOk
        TrendState.FATIGUE     -> OrangeStk
        else                   -> YellowWarn
    }
    val trendIcon = when (trend?.trend) {
        TrendState.PROGRESSING -> "↑"
        TrendState.FATIGUE     -> "↓"
        else                   -> "→"
    }
    val trendLabel = when (trend?.trend) {
        TrendState.PROGRESSING -> "Progresando"
        TrendState.FATIGUE     -> "Fatiga"
        else                   -> "Mantenimiento"
    }

    val mainValue: String
    val mainUnit: String
    val mainSublabel: String
    val sparkData: List<Pair<String, Float>>

    if (isC) {
        val lastDur = durData.lastOrNull()?.second?.toInt() ?: 0
        mainValue    = "$lastDur"
        mainUnit     = "min"
        mainSublabel = "última sesión · total ${durData.sumOf { it.second.toInt() }}min"
        sparkData    = durData
    } else if (isS) {
        val currentE1rm = trend?.latestMetric?.roundToInt() ?: 0
        val allTimeE1rm = e1rmData.maxOfOrNull { it.second }?.roundToInt() ?: 0
        mainValue    = "$currentE1rm"
        mainUnit     = "kg"
        mainSublabel = "Histórico: ${allTimeE1rm}kg"
        sparkData    = e1rmData
    } else {
        val ld = vm.savedSessions.lastOrNull { s -> s.sets.any { it.exerciseId == exercise.id } }?.date
        val bs = vm.historyForVariant(exercise.id, selectedVariant)
            .filter { it.first == ld }.maxByOrNull { it.second.reps * it.second.weightKg }?.second
        mainValue    = if (bs != null) "${bs.reps}r×${bs.weightKg.toInt()}" else "—"
        mainUnit     = "kg"
        mainSublabel = "mejor set · vol. ${volData.lastOrNull()?.second?.roundToInt() ?: 0}kg"
        sparkData    = volData
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(18.dp),
        color    = Surface1,
        border   = BorderStroke(1.dp, exercise.color.copy(alpha = 0.25f))
    ) {
        Box {
            Box(Modifier.fillMaxWidth().height(3.dp).background(exercise.color, RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)))
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column {
                        Text(
                            if (isS) "E1RM actual" else if (isC) "Duración" else "Mejor set",
                            fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(mainValue, fontSize = 40.sp, fontWeight = FontWeight.Black, color = Accent, lineHeight = 40.sp, letterSpacing = (-1).sp)
                            if (!(!isS && !isC)) {
                                Text(mainUnit, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Accent, modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }
                        Text(mainSublabel, fontSize = 11.sp, color = TextSec)
                    }
                    trend?.let { t ->
                        Column(horizontalAlignment = Alignment.End) {
                            Surface(shape = RoundedCornerShape(10.dp), color = trendColor.copy(alpha = 0.1f), border = BorderStroke(1.dp, trendColor.copy(alpha = 0.3f))) {
                                Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(trendIcon, fontSize = 18.sp, color = trendColor, fontWeight = FontWeight.Black)
                                    Text(trendLabel, fontSize = 9.sp, color = trendColor, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (t.pctChange != 0f) {
                                Spacer(Modifier.height(6.dp))
                                val sign = if (t.pctChange > 0) "+" else ""
                                Text("$sign${t.pctChange.roundToInt()}% vs 5", fontSize = 10.sp,
                                    color = if (t.pctChange > 0) GreenOk else RedBad, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (sparkData.size >= 2) {
                    Spacer(Modifier.height(12.dp))
                    HeroSparkline(sparkData, exercise.color, Modifier.fillMaxWidth().height(48.dp))
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatDate(sparkData.first().first), fontSize = 9.sp, color = TextTert)
                        Text(formatDate(sparkData.last().first), fontSize = 9.sp, color = TextSec, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun HeroSparkline(data: List<Pair<String, Float>>, color: Color, modifier: Modifier = Modifier) {
    if (data.size < 2) return
    val values = data.map { it.second }
    val minV   = values.min()
    val maxV   = values.max()
    val range  = if (maxV == minV) 1f else maxV - minV

    Canvas(modifier) {
        val w    = size.width
        val h    = size.height
        val padV = 6f
        val dH   = h - padV * 2
        val sx   = w / (data.size - 1).toFloat()

        fun xAt(i: Int)   = i * sx
        fun yAt(v: Float) = padV + dH * (1f - (v - minV) / range)

        val fillPath = Path().apply {
            moveTo(xAt(0), yAt(values[0]))
            for (i in 1 until data.size) {
                val cx = (xAt(i - 1) + xAt(i)) / 2f
                cubicTo(cx, yAt(values[i - 1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
            }
            lineTo(xAt(data.size - 1), h)
            lineTo(0f, h)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.22f), Color.Transparent), startY = padV, endY = h))

        val linePath = Path().apply {
            moveTo(xAt(0), yAt(values[0]))
            for (i in 1 until data.size) {
                val cx = (xAt(i - 1) + xAt(i)) / 2f
                cubicTo(cx, yAt(values[i - 1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
            }
        }
        drawPath(linePath, color, style = Stroke(width = 2f, cap = StrokeCap.Round))

        val lastX = xAt(data.size - 1)
        val lastY = yAt(values.last())
        drawCircle(color.copy(alpha = 0.25f), radius = 10f, center = Offset(lastX, lastY))
        drawCircle(color, radius = 4f, center = Offset(lastX, lastY))
        drawCircle(Surface1, radius = 2f, center = Offset(lastX, lastY))
    }
}

@Composable
fun CollapsibleHistoryEntry(
    date: String,
    entries: List<Pair<String, WorkoutSet>>,
    isS: Boolean,
    isC: Boolean = false,
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
            Row(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(formatDate(date), fontWeight = FontWeight.SemiBold, color = TextPrim, fontSize = 13.sp)
                    if (isC) {
                        val totalMins = ss.sumOf { it.reps }
                        val maxInt    = ss.maxOfOrNull { it.weightKg }
                        Text("${totalMins}min${if (maxInt != null && maxInt > 0f) " · Int. ${maxInt.toInt()}/10" else ""}",
                            fontSize = 10.sp, color = TextSec)
                    } else {
                        Text("${ss.size} series · Vol. ${ss.sumOf { (it.weightKg * it.reps).toDouble() }.toInt()}kg",
                            fontSize = 10.sp, color = TextSec)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (isC) {
                            val maxInt = ss.maxOfOrNull { it.weightKg }
                            if (maxInt != null && maxInt > 0f)
                                Text("🔥 ${maxInt.toInt()}/10", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                        } else if (isS) {
                            Text("E1RM ${bE.roundToInt()}kg", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                        } else if (bHS != null) {
                            Text("${bHS.reps}r×${bHS.weightKg.toInt()}kg", fontSize = 10.sp, color = Accent, fontWeight = FontWeight.Bold)
                        }
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

                        Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(22.dp).background(Surface3, RoundedCornerShape(5.dp)), contentAlignment = Alignment.Center) {
                                    Text("${i+1}", fontSize = 9.sp, color = TextSec, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    if (isC) Text("${set.reps} min", fontSize = 13.sp, color = TextPrim)
                                    else     Text("${set.reps} reps", fontSize = 13.sp, color = TextPrim)
                                    if (set.variant.isNotBlank()) {
                                        Text(set.variant, fontSize = 9.sp, color = Purple, fontWeight = FontWeight.Medium)
                                    }
                                }
                                if (isC) {
                                    if (set.weightKg > 0f)
                                        Text("Int. ${set.weightKg.toInt()}/10", fontSize = 13.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text(if (set.weightKg == 0f) "PC" else "${set.weightKg}kg", fontSize = 13.sp,
                                        color = if (pr) Accent else TextSec,
                                        fontWeight = if (pr) FontWeight.Black else FontWeight.Normal)
                                    Spacer(Modifier.width(6.dp))
                                    if (!isC) {
                                        Text(if (isS) "→${e1.roundToInt()}" else "=${hy.roundToInt()}",
                                            fontSize = 10.sp, color = if (top) Accent else TextTert)
                                        if (top) { Spacer(Modifier.width(2.dp)); Text("★", fontSize = 9.sp, color = Accent) }
                                    }
                                }
                            }
                            if (set.note.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Row(
                                    Modifier.padding(start = 30.dp).fillMaxWidth()
                                        .background(Surface3, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("📝", fontSize = 9.sp)
                                    Text(set.note, fontSize = 10.sp, color = TextSec, lineHeight = 14.sp)
                                }
                                Spacer(Modifier.height(2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}