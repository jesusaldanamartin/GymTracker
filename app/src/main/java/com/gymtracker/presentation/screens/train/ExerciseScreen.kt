package com.gymtracker.presentation.screens.train

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.local.ExerciseDefaults.variantSuggestionsFor
import com.gymtracker.data.local.SeedData.MUSCLES
import com.gymtracker.data.local.SeedData.ROUTINES
import com.gymtracker.domain.model.Exercise
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Black
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.Surface0
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.Surface2
import com.gymtracker.presentation.theme.Surface3
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import com.gymtracker.presentation.theme.TextTert

import com.gymtracker.presentation.components.AddCustomExerciseDialog
import com.gymtracker.presentation.components.LogCardioDialog
import com.gymtracker.presentation.components.LogSetDialog
import com.gymtracker.presentation.components.ConfirmDeleteDialog
import com.gymtracker.presentation.components.ExerciseCard
import com.gymtracker.presentation.screens.GymViewModel

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
        if (ex.isCardio) {
            LogCardioDialog(
                exercise = ex,
                lastSet  = vm.setsFor(ex.id).lastOrNull(),
                vm = vm,
                knownVariants = vm.knownVariantsFor(ex.id),
                suggestedVariants = variantSuggestionsFor(ex),
                onDismiss = { dialogExercise = null }
            ) { mins, intensity, variant, note ->
                vm.logSet(ex, mins, intensity, variant, note, context)
                dialogExercise = null
            }
        } else {
            LogSetDialog(
                exercise = ex,
                lastSet  = vm.setsFor(ex.id).lastOrNull(),
                vm = vm,
                knownVariants = vm.knownVariantsFor(ex.id),
                suggestedVariants = variantSuggestionsFor(ex),
                onDismiss = { dialogExercise = null }
            ) { reps, weight, variant, note ->
                vm.logSet(ex, reps, weight, variant, note, context)
                dialogExercise = null
            }
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