package com.gymtracker.presentation.screens.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.local.ExerciseDefaults.ROUTINE_COLORS
import com.gymtracker.domain.model.Exercise
import com.gymtracker.domain.model.Session
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Black
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.RedBad
import com.gymtracker.presentation.theme.Surface0
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.collections.component1
import kotlin.collections.component2

import com.gymtracker.presentation.components.ConfirmDeleteDialog
import com.gymtracker.presentation.components.EmptyState
import com.gymtracker.presentation.components.ExerciseBlock
import com.gymtracker.presentation.components.StatCard
import com.gymtracker.presentation.components.formatDate
import com.gymtracker.presentation.screens.GymViewModel

//import com.gymtracker.presentation.components.SessionHistoryCard

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
                            val ex = vm.allExercises.find { it.name == name }
                            ExerciseBlock(
                                name = name, sets = sets, isCardio = ex?.isCardio == true,
                                onDelete = { set -> vm.deleteSetFromSession(session.date, set, context) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarLegend() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ROUTINE_COLORS.entries.forEach { (routine, color) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.size(9.dp).background(color, CircleShape))
                Text(routine, fontSize = 10.sp, color = TextSec, fontWeight = FontWeight.Medium)
            }
        }
    }
}

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
                    Box(Modifier.weight(1f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                        if (dayNum in 1..daysInMonth) {
                            val dateStr      = month.atDay(dayNum).toString()
                            val trained      = dateStr in trainedDates
                            val selected     = dateStr == selectedDate
                            val isToday      = dateStr == LocalDate.now().toString()
                            val routine      = routineByDate[dateStr]
                            val routineColor = routine?.let { ROUTINE_COLORS[it] }
                            val activeColor  = routineColor ?: Accent

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(
                                    Modifier.size(32.dp)
                                        .background(
                                            color = when {
                                                selected -> activeColor
                                                trained  -> activeColor.copy(alpha = 0.15f)
                                                else     -> Color.Transparent
                                            },
                                            shape = CircleShape
                                        )
                                        .then(when {
                                            isToday && trained && !selected -> Modifier.border(2.dp, activeColor, CircleShape)
                                            isToday && !trained && !selected -> Modifier.border(1.5.dp, Accent, CircleShape)
                                            trained && !selected -> Modifier.border(1.dp, activeColor.copy(alpha = 0.6f), CircleShape)
                                            else -> Modifier
                                        })
                                        .clickable { onSelect(dateStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$dayNum", fontSize = 12.sp,
                                        fontWeight = if (trained || selected || isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            selected -> Black
                                            isToday  -> activeColor
                                            trained  -> TextPrim
                                            else     -> TextPrim.copy(alpha = 0.6f)
                                        })
                                }
                                if (trained && !selected) {
                                    Spacer(Modifier.height(2.dp))
                                    Box(Modifier.size(4.dp).background(activeColor, CircleShape))
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
    val routines = session.sets.mapNotNull { set ->
        allExercises.find { it.id == set.exerciseId }?.routine
    }
    val dominantRoutine = routines.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
    val routineColor    = dominantRoutine?.let { ROUTINE_COLORS[it] } ?: Accent

    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = Surface1,
        border = BorderStroke(1.dp, routineColor.copy(alpha = 0.3f))) {
        Row(Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(3.dp).height(36.dp).background(routineColor, RoundedCornerShape(2.dp)))
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