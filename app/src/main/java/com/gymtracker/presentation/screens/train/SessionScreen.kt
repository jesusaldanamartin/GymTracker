package com.gymtracker.presentation.screens.train

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.local.ExerciseDefaults.variantSuggestionsFor
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.bestE1RM
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Black
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.Surface0
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.Surface2
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import java.time.LocalDate
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.math.roundToInt

import com.gymtracker.presentation.components.EditCardioDialog
import com.gymtracker.presentation.components.EditSetDialog

import com.gymtracker.presentation.components.BigStat
import com.gymtracker.presentation.components.ExerciseBlock
import com.gymtracker.presentation.components.SectionLabel
import com.gymtracker.presentation.components.StatCard
import com.gymtracker.presentation.components.formatDate
import com.gymtracker.presentation.screens.GymViewModel

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
            if (exercise.isCardio) {
                EditCardioDialog(
                    exercise = exercise,
                    currentSet = setToEdit,
                    knownVariants = vm.knownVariantsFor(exercise.id),
                    suggestedVariants = variantSuggestionsFor(exercise),
                    onDismiss = { editingSet = null }
                ) { newMins: Int, newIntensity: Float, newVariant: String, newNote: String ->
                    vm.editSet(setToEdit, newMins, newIntensity, newVariant, newNote, context)
                    editingSet = null
                }
            } else {
                EditSetDialog(
                    exercise = exercise,
                    currentSet = setToEdit,
                    vm = vm,
                    knownVariants = vm.knownVariantsFor(exercise.id),
                    suggestedVariants = variantSuggestionsFor(exercise),
                    onDismiss = { editingSet = null }
                ) { newReps: Int, newWeight: Float, newVariant: String, newNote: String ->
                    vm.editSet(setToEdit, newReps, newWeight, newVariant, newNote, context)
                    editingSet = null
                }
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
                        val ex = vm.allExercises.find { it.name == name }
                        ExerciseBlock(name = name, sets = sets, isCardio = ex?.isCardio == true,
                            onDelete = { vm.deleteSet(it, context) },
                            onEdit   = { editingSet = it })
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}


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
                                    if (ex?.isCardio == true) {
                                        val totalMins = exSets.sumOf { it.reps }
                                        Text("${totalMins}min total", fontSize = 12.sp, color = TextSec)
                                    } else {
                                        Text("${exSets.size} series · ${exSets.sumOf { it.reps }} reps", fontSize = 12.sp, color = TextSec)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    if (ex?.isCardio == true) {
                                        val maxInt = exSets.maxOfOrNull { it.weightKg }
                                        if (maxInt != null && maxInt > 0f)
                                            Text("🔥 Int. ${maxInt.toInt()}/10", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                    } else if (ex?.isStrengthFocus == true) {
                                        Text("E1RM ${bestE1RM(exSets).roundToInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                    } else {
                                        val bs = exSets.maxByOrNull { it.reps * it.weightKg }
                                        if (bs != null) Text("${bs.reps}r×${bs.weightKg.toInt()}kg", fontWeight = FontWeight.Black, color = Accent, fontSize = 14.sp)
                                    }
                                    if (ex?.isCardio != true)
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