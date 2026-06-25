package com.gymtracker.presentation.components

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.presentation.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
            Box(Modifier.fillMaxWidth().height(3.dp).background(exercise.color))
            Column(
                Modifier.fillMaxSize().padding(top = 10.dp, start = 10.dp, end = 10.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    Modifier.fillMaxWidth().weight(1f)
                        .background(exercise.color.copy(0.06f), RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ExerciseVisual(exercise = exercise, context = context)
                    if (exercise.isCustom) {
                        Box(Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Blue.copy(0.15f)) {
                                Text("C", fontSize = 7.sp, color = Blue, fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                    if (exercise.isCardio) {
                        Box(Modifier.align(Alignment.TopStart).padding(4.dp)) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE63946).copy(0.2f)) {
                                Text("⏱", fontSize = 7.sp,
                                    modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(exercise.name, fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = TextPrim, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 13.sp)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(exercise.muscle.uppercase(), fontSize = 8.sp, fontWeight = FontWeight.Bold,
                        color = exercise.color, letterSpacing = 0.3.sp)
                    if (setCount > 0) Surface(shape = CircleShape, color = Accent) {
                        Text("$setCount", modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                            fontSize = 9.sp, fontWeight = FontWeight.Black, color = Black)
                    }
                }
            }
            if (onDelete != null) {
                Box(Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 6.dp)) {
                    Surface(onClick = onDelete, shape = CircleShape, color = RedBad.copy(0.15f), modifier = Modifier.size(18.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Close, null, tint = RedBad, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }
        }
    }
}

val EXERCISE_DRAWABLES: Map<Int, Int> = mapOf()
val EXERCISE_REMOTE_URLS: Map<Int, String> = mapOf()
@Composable
fun ExerciseVisual(exercise: Exercise, context: Context) {
    val drawableRes = EXERCISE_DRAWABLES[exercise.id]
    if (drawableRes != null) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = drawableRes),
            contentDescription = exercise.name,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(4.dp)
        )
        return
    }
    val remoteUrl = EXERCISE_REMOTE_URLS[exercise.id]
    if (remoteUrl != null) {
        var remoteFailed by remember(remoteUrl) { mutableStateOf(false) }
        if (!remoteFailed) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(remoteUrl).crossfade(true).build(),
                contentDescription = exercise.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(4.dp),
                onError = { remoteFailed = true }
            )
            return
        }
    }
    Text(text = exercise.emoji, fontSize = 30.sp, textAlign = TextAlign.Center)
}


@Composable
fun ExerciseBlock(
    name:     String,
    sets:     List<WorkoutSet>,
    isCardio: Boolean = false,
    onDelete: ((WorkoutSet) -> Unit)?,
    onEdit:   ((WorkoutSet) -> Unit)? = null
) {
    Surface(shape = RoundedCornerShape(14.dp), color = Surface1) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = TextPrim)
            if (isCardio) {
                val totalMins = sets.sumOf { it.reps }
                val maxInt    = sets.maxOfOrNull { it.weightKg }
                Text("${totalMins}min total${if (maxInt != null && maxInt > 0f) " · Int. máx. ${maxInt.toInt()}/10" else ""}",
                    fontSize = 12.sp, color = TextSec)
            } else {
                Text("${sets.size} series · ${sets.sumOf { it.reps }} reps · max ${sets.maxOf { it.weightKg }}kg",
                    fontSize = 12.sp, color = TextSec)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(6.dp))
            sets.forEachIndexed { i, set ->
                Column(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).background(Surface3, RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
                            Text("${i+1}", fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            if (isCardio) {
                                Text("${set.reps} min", fontSize = 14.sp, color = TextPrim)
                            } else {
                                Text("${set.reps} reps", fontSize = 14.sp, color = TextPrim)
                            }
                            if (set.variant.isNotBlank()) {
                                Text(set.variant, fontSize = 9.sp, color = Purple, fontWeight = FontWeight.Medium)
                            }
                        }
                        if (isCardio) {
                            if (set.weightKg > 0f)
                                Text("🔥 ${set.weightKg.toInt()}/10", fontSize = 14.sp,
                                    color = intensityColor(set.weightKg.toInt()), fontWeight = FontWeight.SemiBold)
                        } else {
                            Text(if (set.weightKg == 0f) "Peso corp." else "${set.weightKg}kg",
                                fontSize = 14.sp, color = Accent, fontWeight = FontWeight.SemiBold)
                        }
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
                    if (set.note.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Row(
                            Modifier.padding(start = 32.dp).fillMaxWidth()
                                .background(Surface3, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("📝", fontSize = 9.sp)
                            Text(set.note, fontSize = 10.sp, color = TextSec, lineHeight = 14.sp)
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

@Composable
fun ConfirmDeleteDialog(title: String, body: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Surface2,
        title = { Text(title, color = TextPrim, fontWeight = FontWeight.Bold) },
        text  = { Text(body,  color = TextSec,  fontSize = 13.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Eliminar", color = RedBad, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = TextSec) } }
    )
}