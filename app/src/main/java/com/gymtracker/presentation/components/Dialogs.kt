package com.gymtracker.presentation.components

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.copy
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.gymtracker.data.local.ExerciseDefaults.MUSCLE_COLORS
import com.gymtracker.data.local.SeedData.MUSCLES
import com.gymtracker.data.local.SeedData.ROUTINES
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.estimatedOneRM
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Black
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.BorderLight
import com.gymtracker.presentation.theme.GreenOk
import com.gymtracker.presentation.theme.OrangeStk
import com.gymtracker.presentation.theme.Purple
import com.gymtracker.presentation.theme.RedBad
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.Surface2
import com.gymtracker.presentation.theme.Surface3
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import com.gymtracker.presentation.theme.TextTert
import com.gymtracker.presentation.theme.YellowWarn
import kotlin.math.roundToInt

import com.gymtracker.presentation.screens.GymViewModel
import com.gymtracker.presentation.components.Chip
import com.gymtracker.presentation.components.NoteSection
import com.gymtracker.presentation.components.intensityColor

fun intensityColor(level: Int): Color = when {
    level <= 3 -> GreenOk
    level <= 6 -> YellowWarn
    level <= 8 -> OrangeStk
    else       -> RedBad
}

@Composable
fun CollapsibleVariantAndNoteSection(
    variantText: String,
    noteText: String,
    knownVariants: List<String>,
    suggestedVariants: List<String>,
    exerciseColor: Color,
    onVariantChange: (String) -> Unit,
    onNoteChange: (String) -> Unit
) {
    val allChips = remember(knownVariants, suggestedVariants) {
        (suggestedVariants + knownVariants).distinct()
    }
    if (allChips.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(5.dp).background(Purple, CircleShape))
                Text("VARIANTE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    color = TextTert, letterSpacing = 0.8.sp)
                Text("(opcional)", fontSize = 9.sp, color = TextTert)
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                items(allChips) { v ->
                    val sel = variantText.trim().equals(v, ignoreCase = true)
                    Surface(
                        onClick = { onVariantChange(if (sel) "" else v) },
                        shape  = RoundedCornerShape(50),
                        color  = if (sel) Purple else Purple.copy(0.06f),
                        border = BorderStroke(1.dp, if (sel) Purple else Purple.copy(0.2f)),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                            Text(v, fontSize = 10.sp,
                                color = if (sel) Color.White else Purple.copy(0.85f),
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
            OutlinedTextField(
                value = variantText,
                onValueChange = onVariantChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("O escribe una variante…", color = TextTert, fontSize = 12.sp) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple, unfocusedBorderColor = Border,
                    focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                    cursorColor = Purple, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
                )
            )
        }
        Spacer(Modifier.height(4.dp))
    }
    NoteSection(
        noteText      = noteText,
        exerciseColor = exerciseColor,
        onNoteChange  = onNoteChange
    )
}


@Composable
fun VariantChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape  = RoundedCornerShape(50),
        color  = if (selected) Purple else Purple.copy(0.06f),
        border = BorderStroke(1.dp, if (selected) Purple else Purple.copy(0.25f)),
        modifier = Modifier.height(30.dp)
    ) {
        Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) Color.White else Purple)
        }
    }
}

@Composable
fun VariantSection(
    variantText: String,
    configuredVariants: List<String>,
    knownUsedVariants: List<String>,
    onVariantChange: (String) -> Unit
) {
    val allChips = remember(configuredVariants, knownUsedVariants) {
        (configuredVariants + knownUsedVariants).distinct()
    }
    if (allChips.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(5.dp).background(Purple, CircleShape))
            Text("VARIANTE", fontSize = 9.sp, fontWeight = FontWeight.Bold,
                color = TextTert, letterSpacing = 0.8.sp)
            Text("(opcional)", fontSize = 9.sp, color = TextTert)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items(allChips) { v ->
                val sel = variantText.trim().equals(v, ignoreCase = true)
                Surface(
                    onClick = { onVariantChange(if (sel) "" else v) },
                    shape  = RoundedCornerShape(50),
                    color  = if (sel) Purple else Purple.copy(0.06f),
                    border = BorderStroke(1.dp, if (sel) Purple else Purple.copy(0.2f)),
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Text(v, fontSize = 10.sp,
                            color = if (sel) Color.White else Purple.copy(0.85f),
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        OutlinedTextField(
            value = variantText,
            onValueChange = onVariantChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("O escribe una variante…", color = TextTert, fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple, unfocusedBorderColor = Border,
                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                cursorColor = Purple, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
            )
        )
    }
}

@Composable
fun NoteSection(
    noteText: String,
    exerciseColor: Color,
    onNoteChange: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(noteText.isNotBlank()) }

    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Surface(
            onClick = { expanded = !expanded },
            shape = RoundedCornerShape(10.dp),
            color = if (expanded) Surface3 else Color.Transparent,
            border = BorderStroke(1.dp, if (expanded) BorderLight else Border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("📝", fontSize = 12.sp)
                    Text("Añadir nota", fontSize = 12.sp,
                        color = if (expanded) TextPrim else TextSec,
                        fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Normal)
                    if (!expanded && noteText.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(4.dp), color = TextSec.copy(0.12f)) {
                            Text(noteText, fontSize = 9.sp, color = TextSec,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    null, tint = TextTert, modifier = Modifier.size(16.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            OutlinedTextField(
                value = noteText,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                placeholder = { Text("Molestia, contexto, fatiga…", color = TextTert, fontSize = 12.sp) },
                maxLines = 3,
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = exerciseColor.copy(0.6f), unfocusedBorderColor = Border,
                    focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                    cursorColor = exerciseColor, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
                )
            )
        }
    }
}

@Composable
fun LogSetDialog(
    exercise: Exercise,
    lastSet: WorkoutSet?,
    vm: GymViewModel,
    knownVariants: List<String>,
    suggestedVariants: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, Float, String, String) -> Unit
) {
    var repsText    by remember { mutableStateOf(lastSet?.reps?.toString() ?: "") }
    var weightText  by remember { mutableStateOf(lastSet?.weightKg?.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() } ?: "") }
    var variantText by remember { mutableStateOf(lastSet?.variant ?: "") }
    var noteText    by remember { mutableStateOf("") }
    var repsError   by remember { mutableStateOf(false) }
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
            LazyColumn(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
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
                }

                if (lastSet != null) {
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = exercise.color.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(if (isS) "Última: ${lastSet.reps}r × ${lastSet.weightKg}kg → E1RM ${estimatedOneRM(lastSet.weightKg, lastSet.reps).roundToInt()}kg"
                                else "Última: ${lastSet.reps}r × ${lastSet.weightKg}kg (score ${(lastSet.reps * lastSet.weightKg).roundToInt()})",
                                    fontSize = 12.sp, color = exercise.color)
                                if (lastSet.variant.isNotBlank()) {
                                    Text("↳ ${lastSet.variant}", fontSize = 10.sp, color = Purple, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                item {
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
                }

                (previewE1 ?: previewHy)?.let { v ->
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = Accent.copy(0.06f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isS) "E1RM estimado" else "Score reps×peso", fontSize = 12.sp, color = TextSec)
                                Text(if (isS) "${v.roundToInt()} kg" else "${v.roundToInt()}", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }

                item {
                    if (vm.hasVariants(exercise.id)) {
                        HorizontalDivider(color = Border)
                        Spacer(Modifier.height(8.dp))
                        VariantSection(
                            variantText        = variantText,
                            configuredVariants = vm.variantsFor(exercise.id),
                            knownUsedVariants  = knownVariants,
                            onVariantChange    = { variantText = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                item {
                    NoteSection(
                        noteText      = noteText,
                        exerciseColor = exercise.color,
                        onNoteChange  = { noteText = it }
                    )
                }

                if (repsError) { item { Text("Introduce un número válido", fontSize = 11.sp, color = RedBad) } }

                item {
                    Button(onClick = {
                        val r = repsText.trim().toIntOrNull()
                        if (r == null || r <= 0) { repsError = true; return@Button }
                        onSave(r, weightText.trim().toFloatOrNull() ?: 0f, variantText.trim(), noteText.trim())
                    }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                        Text("GUARDAR SERIE", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}


@Composable
fun LogCardioDialog(
    exercise: Exercise,
    lastSet: WorkoutSet?,
    vm: GymViewModel,                   // ← NUEVO parámetro
    knownVariants: List<String>,
    suggestedVariants: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, Float, String, String) -> Unit   // mins, intensity(0-10), variant, note
) {
    var minsText      by remember { mutableStateOf(lastSet?.reps?.toString() ?: "") }
    var intensityText by remember { mutableStateOf(lastSet?.weightKg?.let { if (it == 0f) "" else it.toInt().toString() } ?: "") }
    var variantText   by remember { mutableStateOf(lastSet?.variant ?: "") }
    var noteText      by remember { mutableStateOf("") }
    var minsError     by remember { mutableStateOf(false) }

    // Slider de intensidad (1-10)
    var intensitySlider by remember { mutableStateOf(lastSet?.weightKg?.let { if (it > 0f) it else 5f } ?: 5f) }
    var useSlider       by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            LazyColumn(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(exercise.emoji, fontSize = 18.sp)
                                Text(exercise.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                                TypeBadge("CARDIO", Color(0xFFE63946))
                            }
                            Text(exercise.muscle, fontSize = 12.sp, color = exercise.color)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                    }
                }

                if (lastSet != null) {
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = exercise.color.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("⏱", fontSize = 14.sp)
                                Text("Última: ${lastSet.reps}min${if (lastSet.weightKg > 0f) " · Int. ${lastSet.weightKg.toInt()}/10" else ""}",
                                    fontSize = 12.sp, color = exercise.color)
                                if (lastSet.variant.isNotBlank())
                                    Text("· ${lastSet.variant}", fontSize = 10.sp, color = Purple)
                            }
                        }
                    }
                }

                // Duración
                item {
                    Column {
                        Text("DURACIÓN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = minsText,
                            onValueChange = { minsText = it; minsError = false },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Minutos", color = TextTert) },
                            isError = minsError, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            trailingIcon = { Text("min", fontSize = 12.sp, color = TextSec, modifier = Modifier.padding(end = 8.dp)) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                                focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                                cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
                            )
                        )
                    }
                }

                // Intensidad (slider + chips rápidos)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("INTENSIDAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                            Text("${intensitySlider.toInt()}/10", fontSize = 13.sp, fontWeight = FontWeight.Black, color = intensityColor(intensitySlider.toInt()))
                        }
                        Slider(
                            value = intensitySlider,
                            onValueChange = { intensitySlider = it },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = intensityColor(intensitySlider.toInt()),
                                activeTrackColor = intensityColor(intensitySlider.toInt()),
                                inactiveTrackColor = Border
                            )
                        )
                        // Chips rápidos de intensidad
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            listOf(3 to "Suave", 5 to "Medio", 7 to "Duro", 9 to "Máx.").forEach { (v, label) ->
                                Surface(
                                    onClick = { intensitySlider = v.toFloat() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (intensitySlider.toInt() == v) intensityColor(v).copy(0.18f) else Surface3,
                                    border = BorderStroke(1.dp, if (intensitySlider.toInt() == v) intensityColor(v).copy(0.5f) else Border),
                                    modifier = Modifier.weight(1f).height(32.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(label, fontSize = 10.sp, color = if (intensitySlider.toInt() == v) intensityColor(v) else TextSec,
                                            fontWeight = if (intensitySlider.toInt() == v) FontWeight.Bold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                }

                // FEATURE 1: colapsable
                item {
                    if (vm.hasVariants(exercise.id)) {
                        HorizontalDivider(color = Border)
                        Spacer(Modifier.height(8.dp))
                        VariantSection(
                            variantText        = variantText,
                            configuredVariants = vm.variantsFor(exercise.id),
                            knownUsedVariants  = knownVariants,
                            onVariantChange    = { variantText = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                item {
                    NoteSection(
                        noteText      = noteText,
                        exerciseColor = exercise.color,
                        onNoteChange  = { noteText = it }
                    )
                }

                if (minsError) { item { Text("Introduce una duración válida (minutos)", fontSize = 11.sp, color = RedBad) } }

                item {
                    Button(onClick = {
                        val m = minsText.trim().toIntOrNull()
                        if (m == null || m <= 0) { minsError = true; return@Button }
                        onSave(m, intensitySlider, variantText.trim(), noteText.trim())
                    }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = exercise.color, contentColor = Color.White)) {
                        Text("GUARDAR CARDIO", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditCardioDialog(
    exercise: Exercise,
    currentSet: WorkoutSet,
    knownVariants: List<String>,
    suggestedVariants: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, Float, String, String) -> Unit
) {
    var minsText        by remember { mutableStateOf(currentSet.reps.toString()) }
    var variantText     by remember { mutableStateOf(currentSet.variant) }
    var noteText        by remember { mutableStateOf(currentSet.note) }
    var minsError       by remember { mutableStateOf(false) }
    var intensitySlider by remember { mutableStateOf(if (currentSet.weightKg > 0f) currentSet.weightKg else 5f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            LazyColumn(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Editar cardio", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                            Text(exercise.name, fontSize = 12.sp, color = exercise.color)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(10.dp), color = YellowWarn.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✏️", fontSize = 12.sp)
                            Text("Actual: ${currentSet.reps}min${if (currentSet.weightKg > 0f) " · Int. ${currentSet.weightKg.toInt()}/10" else ""}",
                                fontSize = 12.sp, color = YellowWarn)
                        }
                    }
                }
                item {
                    Text("DURACIÓN (MIN)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                    Spacer(Modifier.height(4.dp))
                    OutlinedTextField(value = minsText, onValueChange = { minsText = it; minsError = false },
                        modifier = Modifier.fillMaxWidth(), isError = minsError, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        trailingIcon = { Text("min", fontSize = 12.sp, color = TextSec, modifier = Modifier.padding(end = 8.dp)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = exercise.color, unfocusedBorderColor = Border,
                            focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                            cursorColor = exercise.color, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1))
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("INTENSIDAD", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSec, letterSpacing = 0.8.sp)
                        Text("${intensitySlider.toInt()}/10", fontSize = 13.sp, fontWeight = FontWeight.Black, color = intensityColor(intensitySlider.toInt()))
                    }
                    Slider(value = intensitySlider, onValueChange = { intensitySlider = it }, valueRange = 1f..10f, steps = 8,
                        colors = SliderDefaults.colors(thumbColor = intensityColor(intensitySlider.toInt()),
                            activeTrackColor = intensityColor(intensitySlider.toInt()), inactiveTrackColor = Border))
                }
                item {
                    HorizontalDivider(color = Border)
                    Spacer(Modifier.height(4.dp))
                    CollapsibleVariantAndNoteSection(
                        variantText = variantText,
                        noteText = noteText,
                        knownVariants = knownVariants,
                        suggestedVariants = suggestedVariants,
                        exerciseColor = exercise.color,
                        onVariantChange = { newVariant -> variantText = newVariant },
                        onNoteChange = { newNote -> noteText = newNote }
                    )
                }
                if (minsError) { item { Text("Introduce una duración válida", fontSize = 11.sp, color = RedBad) } }
                item {
                    Button(onClick = {
                        val m = minsText.trim().toIntOrNull()
                        if (m == null || m <= 0) { minsError = true; return@Button }
                        onSave(m, intensitySlider, variantText.trim(), noteText.trim())
                    }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                        Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp))
                        Text("ACTUALIZAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun EditSetDialog(
    exercise: Exercise,
    currentSet: WorkoutSet,
    knownVariants: List<String>,
    vm: GymViewModel,                   // ← NUEVO parámetro
    suggestedVariants: List<String>,
    onDismiss: () -> Unit,
    onSave: (Int, Float, String, String) -> Unit
) {
    var repsText    by remember { mutableStateOf(currentSet.reps.toString()) }
    var weightText  by remember { mutableStateOf(currentSet.weightKg.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }) }
    var variantText by remember { mutableStateOf(currentSet.variant) }
    var noteText    by remember { mutableStateOf(currentSet.note) }
    var repsError   by remember { mutableStateOf(false) }
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
            LazyColumn(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Editar serie", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                            Text(exercise.name, fontSize = 12.sp, color = exercise.color)
                        }
                        IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                    }
                }
                item {
                    Surface(shape = RoundedCornerShape(10.dp), color = YellowWarn.copy(0.07f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("✏️", fontSize = 12.sp)
                            Column {
                                Text("Valores actuales: ${currentSet.reps} reps × ${if (currentSet.weightKg == 0f) "PC" else "${currentSet.weightKg}kg"}",
                                    fontSize = 12.sp, color = YellowWarn)
                                if (currentSet.variant.isNotBlank())
                                    Text("↳ ${currentSet.variant}", fontSize = 10.sp, color = Purple)
                            }
                        }
                    }
                }
                item {
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
                }
                (previewE1 ?: previewHy)?.let { v ->
                    item {
                        Surface(shape = RoundedCornerShape(10.dp), color = Accent.copy(0.06f), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isS) "E1RM estimado" else "Score reps×peso", fontSize = 12.sp, color = TextSec)
                                Text(if (isS) "${v.roundToInt()} kg" else "${v.roundToInt()}", fontSize = 14.sp, color = Accent, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
                // FEATURE 1: colapsable
                item {
                    if (vm.hasVariants(exercise.id)) {
                        HorizontalDivider(color = Border)
                        Spacer(Modifier.height(8.dp))
                        VariantSection(
                            variantText        = variantText,
                            configuredVariants = vm.variantsFor(exercise.id),
                            knownUsedVariants  = knownVariants,
                            onVariantChange    = { variantText = it }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                item {
                    NoteSection(
                        noteText      = noteText,
                        exerciseColor = exercise.color,
                        onNoteChange  = { noteText = it }
                    )
                }
                if (repsError) { item { Text("Introduce un número válido", fontSize = 11.sp, color = RedBad) } }
                item {
                    Button(onClick = {
                        val r = repsText.trim().toIntOrNull()
                        if (r == null || r <= 0) { repsError = true; return@Button }
                        onSave(r, weightText.trim().toFloatOrNull() ?: 0f, variantText.trim(), noteText.trim())
                    }, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)) {
                        Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp))
                        Text("ACTUALIZAR SERIE", fontWeight = FontWeight.Bold)
                    }
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
    var isCardio by remember { mutableStateOf(false) }
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
                // FEATURE 3: toggle cardio para ejercicios custom
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Ejercicio de cardio", fontSize = 14.sp, color = TextPrim)
                        Text("Registra tiempo e intensidad en vez de reps+peso", fontSize = 11.sp, color = TextSec)
                    }
                    Switch(checked = isCardio, onCheckedChange = { isCardio = it; if (it) strength = false },
                        colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Color(0xFFE63946),
                            uncheckedThumbColor = TextSec, uncheckedTrackColor = Border))
                }
                if (!isCardio) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Ejercicio de fuerza", fontSize = 14.sp, color = TextPrim)
                            Text("Activa E1RM como métrica de progreso", fontSize = 11.sp, color = TextSec)
                        }
                        Switch(checked = strength, onCheckedChange = { strength = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = Black, checkedTrackColor = Accent,
                                uncheckedThumbColor = TextSec, uncheckedTrackColor = Border))
                    }
                }
                Button(onClick = {
                    if (name.isBlank()) { nameErr = true; return@Button }
                    vm.addCustomExercise(Exercise(vm.nextCustomId(), name.trim(), muscle, routine, emoji,
                        MUSCLE_COLORS[muscle] ?: Color(0xFF8E8E93), strength, isCustom = true, isCardio = isCardio), context)
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
fun ConfigureVariantsDialog(
    exercise: Exercise,
    currentVariants: List<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>) -> Unit
) {
    var items by remember { mutableStateOf(currentVariants.toMutableList()) }
    var newText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(22.dp), color = Surface2) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Variantes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrim)
                        Text(exercise.name, fontSize = 12.sp, color = exercise.color)
                    }
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = TextSec) }
                }

                Text("Configura los agarres o técnicas que usas. Aparecerán como chips al registrar una serie.",
                    fontSize = 12.sp, color = TextSec, lineHeight = 16.sp)

                // Lista de variantes actuales
                if (items.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items.toList().forEach { v ->
                            Surface(shape = RoundedCornerShape(10.dp), color = Surface1, border = BorderStroke(1.dp, Border)) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).background(Purple, CircleShape))
                                    Spacer(Modifier.width(10.dp))
                                    Text(v, fontSize = 13.sp, color = TextPrim, modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { items = items.toMutableList().also { it.remove(v) } },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = RedBad, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text("Sin variantes — el ejercicio no mostrará chips", fontSize = 12.sp, color = TextTert, textAlign = TextAlign.Center)
                    }
                }

                // Añadir nueva
                HorizontalDivider(color = Border)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newText,
                        onValueChange = { newText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nueva variante…", color = TextTert, fontSize = 12.sp) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple, unfocusedBorderColor = Border,
                            focusedTextColor = TextPrim, unfocusedTextColor = TextPrim,
                            cursorColor = Purple, focusedContainerColor = Surface1, unfocusedContainerColor = Surface1
                        )
                    )
                    Surface(
                        onClick = {
                            val t = newText.trim()
                            if (t.isNotBlank() && !items.contains(t)) {
                                items = items.toMutableList().also { it.add(t) }
                                newText = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = Purple.copy(0.15f),
                        border = BorderStroke(1.dp, Purple.copy(0.4f)),
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Add, null, tint = Purple, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Guardar / Cancelar
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancelar", color = TextSec)
                    }
                    Button(
                        onClick = { onSave(items.toList()) },
                        modifier = Modifier.weight(2f).height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Black)
                    ) {
                        Text("GUARDAR", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}