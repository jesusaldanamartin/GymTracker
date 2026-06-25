package com.gymtracker.presentation.screens.home

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.local.currentWeekMonday
import com.gymtracker.data.local.currentWeekSunday
import com.gymtracker.presentation.theme.*
import java.time.format.DateTimeFormatter
import com.gymtracker.presentation.components.ConfirmDeleteDialog
import com.gymtracker.presentation.components.formatDate
import com.gymtracker.presentation.screens.GymViewModel

//import com.gymtracker.presentation.components.WeekStatItem
//import com.gymtracker.presentation.components.HomeNavButton

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