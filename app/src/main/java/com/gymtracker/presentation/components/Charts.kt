package com.gymtracker.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.domain.model.Session
import com.gymtracker.domain.model.WorkoutSet
import com.gymtracker.domain.model.bestE1RM
import com.gymtracker.domain.model.estimatedOneRM
import com.gymtracker.presentation.theme.Accent
import com.gymtracker.presentation.theme.Black
import com.gymtracker.presentation.theme.Border
import com.gymtracker.presentation.theme.GreenOk
import com.gymtracker.presentation.theme.Surface0
import com.gymtracker.presentation.theme.Surface1
import com.gymtracker.presentation.theme.Surface2
import com.gymtracker.presentation.theme.TextPrim
import com.gymtracker.presentation.theme.TextSec
import com.gymtracker.presentation.theme.TextTert
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt


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


@Composable
fun LineChartEnhanced(
    data: List<Pair<String, Float>>,
    color: Color,
    unit: String,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) return

    val values  = data.map { it.second }
    val minV    = values.min()
    val maxV    = values.max()
    val range   = if (maxV == minV) 1f else maxV - minV
    val maxIdx  = values.indexOf(maxV)
    val lastIdx = data.size - 1

    val padTDp = 28.dp
    val padBDp = 22.dp
    val padLDp = 36.dp
    val padRDp = 12.dp

    val padTPx = 56f
    val padBPx = 44f
    val padLPx = 72f
    val padRPx = 24f

    Box(modifier) {

        Canvas(Modifier.fillMaxSize()) {
            val w  = size.width
            val h  = size.height
            val dW = w - padLPx - padRPx
            val dH = h - padTPx - padBPx
            if (dW <= 0f || dH <= 0f) return@Canvas
            val sx = dW / (data.size - 1).toFloat()

            fun xAt(i: Int)   = padLPx + i * sx
            fun yAt(v: Float) = padTPx + dH * (1f - (v - minV) / range)

            for (g in 0..3) {
                val gy = padTPx + dH * (g / 3f)
                drawLine(Border, Offset(padLPx, gy), Offset(w - padRPx, gy), strokeWidth = 0.5f)
            }

            val maxY = yAt(maxV)
            drawLine(
                color       = Accent.copy(alpha = 0.35f),
                start       = Offset(padLPx, maxY),
                end         = Offset(w - padRPx, maxY),
                strokeWidth = 1f,
                pathEffect  = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )

            val fillPath = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) {
                    val cx = (xAt(i - 1) + xAt(i)) / 2f
                    cubicTo(cx, yAt(values[i - 1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
                }
                lineTo(xAt(lastIdx), h - padBPx)
                lineTo(xAt(0), h - padBPx)
                close()
            }
            drawPath(
                fillPath,
                Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.22f), Color.Transparent),
                    startY = padTPx, endY = h - padBPx
                )
            )

            val linePath = Path().apply {
                moveTo(xAt(0), yAt(values[0]))
                for (i in 1 until data.size) {
                    val cx = (xAt(i - 1) + xAt(i)) / 2f
                    cubicTo(cx, yAt(values[i - 1]), cx, yAt(values[i]), xAt(i), yAt(values[i]))
                }
            }
            drawPath(linePath, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))

            data.indices.forEach { i ->
                val px     = xAt(i)
                val py     = yAt(values[i])
                val isMax  = i == maxIdx
                val isLast = i == lastIdx
                val r = when {
                    isMax && isLast -> 7f
                    isMax || isLast -> 6f
                    else            -> 3.5f
                }
                if (isMax || isLast) {
                    drawCircle(color.copy(alpha = 0.18f), radius = r + 6f, center = Offset(px, py))
                }
                drawCircle(color,    radius = r,      center = Offset(px, py))
                drawCircle(Surface0, radius = r - 2f, center = Offset(px, py))
            }
        }

        // Y axis labels
        Column(
            Modifier
                .fillMaxHeight()
                .width(padLDp)
                .padding(top = padTDp, bottom = padBDp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            for (g in 0..3) {
                val gVal = maxV - range * (g / 3f)
                Text(
                    text      = "${gVal.roundToInt()}$unit",
                    fontSize  = 8.sp,
                    color     = TextTert,
                    modifier  = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End
                )
            }
        }

        // "máx" label
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = padTDp, end = padRDp)
        ) {
            Text(
                "máx",
                fontSize   = 7.sp,
                color      = Accent.copy(alpha = 0.7f),
                fontWeight = FontWeight.Bold
            )
        }

        // Values above points (single block — duplicate removed)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val totalWDp = maxWidth.value
            val totalHDp = maxHeight.value
            val chartWDp = totalWDp - padLDp.value - padRDp.value
            val chartHDp = totalHDp - padTDp.value - padBDp.value
            val sxDp     = if (data.size > 1) chartWDp / (data.size - 1) else chartWDp

            data.indices.forEach { i ->
                val v      = values[i]
                val xDp    = padLDp.value + i * sxDp
                val yFrac  = 1f - (v - minV) / range
                val yDp    = padTDp.value + chartHDp * yFrac
                val isMax  = i == maxIdx
                val isLast = i == lastIdx
                val showVal = isMax || isLast || i == 0 || data.size <= 6 || i % 3 == 0

                if (showVal) {
                    val label = "${v.roundToInt()}$unit"
                    when {
                        isMax && isLast -> Box(
                            Modifier
                                .offset(x = (xDp - 18f).dp, y = (yDp - 22f).dp)
                                .background(Accent, RoundedCornerShape(5.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) { Text(label, fontSize = 9.sp, color = Black, fontWeight = FontWeight.Black) }

                        isMax -> Box(
                            Modifier
                                .offset(x = (xDp - 14f).dp, y = (yDp - 20f).dp)
                                .background(Accent, RoundedCornerShape(5.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) { Text(label, fontSize = 8.sp, color = Black, fontWeight = FontWeight.Black) }

                        isLast -> Box(
                            Modifier
                                .offset(x = (xDp - 14f).dp, y = (yDp - 19f).dp)
                                .background(color.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) { Text(label, fontSize = 8.sp, color = color, fontWeight = FontWeight.Bold) }

                        else -> Box(
                            Modifier
                                .offset(x = (xDp - 10f).dp, y = (yDp - 17f).dp)
                                .width(20.dp)
                        ) {
                            Text(
                                label,
                                fontSize  = 7.sp,
                                color     = TextSec,
                                textAlign = TextAlign.Center,
                                maxLines  = 1
                            )
                        }
                    }
                }
            }
        }

        // X axis dates
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val bwcMaxW  = maxWidth.value
            val bwcMaxH  = maxHeight.value
            val chartWDp = bwcMaxW - padLDp.value - padRDp.value
            val sxDp     = if (data.size > 1) chartWDp / (data.size - 1) else chartWDp
            val yDp      = bwcMaxH - padBDp.value + 3f

            fun shortDate(d: String) = try {
                LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d MMM"))
            } catch (e: Exception) { d }

            val labeled = buildSet {
                add(0)
                add(data.size - 1)
                if (maxIdx != 0 && maxIdx != data.size - 1) add(maxIdx)
            }

            labeled.forEach { i ->
                val xDp    = padLDp.value + i * sxDp
                val isMax  = i == maxIdx
                val isLast = i == lastIdx
                Box(
                    Modifier
                        .offset(x = (xDp - 14f).dp, y = yDp.dp)
                        .width(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        shortDate(data[i].first),
                        fontSize   = 8.sp,
                        color      = when {
                            isMax && isLast -> Accent
                            isMax           -> Accent.copy(alpha = 0.8f)
                            isLast          -> color
                            else            -> TextTert
                        },
                        fontWeight = if (isMax || isLast) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                        maxLines   = 1
                    )
                }
            }
        }
    }
}


@Composable
fun BarChartExpanded(
    data: List<Pair<String, Float>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    val maxVal = data.maxOf { it.second }
    val maxIdx = data.indexOfFirst { it.second == maxVal }

    val barZoneHeightDp   = 80.dp
    val labelZoneHeightDp = 20.dp

    Column(modifier) {
        Box(Modifier.height(barZoneHeightDp + labelZoneHeightDp)) {

            Canvas(Modifier.fillMaxWidth().height(barZoneHeightDp).align(Alignment.TopStart)) {
                val w    = size.width
                val h    = size.height
                val n    = data.size
                val gap  = (w * 0.018f).coerceAtLeast(2f)
                val barW = (w - gap * (n - 1)) / n
                val padB = 4f

                data.forEachIndexed { i, (_, v) ->
                    val ratio  = if (maxVal == 0f) 0f else v / maxVal
                    val bH     = ((h - padB) * ratio).coerceAtLeast(4f)
                    val left   = i * (barW + gap)
                    val top    = h - padB - bH
                    val isBest = i == maxIdx
                    val alpha  = if (isBest) 1f else 0.28f + 0.42f * ratio

                    drawRoundRect(
                        color        = color.copy(alpha = alpha),
                        topLeft      = Offset(left, top),
                        size         = Size(barW, bH),
                        cornerRadius = CornerRadius(barW * 0.35f)
                    )
                    if (isBest) {
                        drawRoundRect(
                            color        = Accent,
                            topLeft      = Offset(left, top),
                            size         = Size(barW, 3f),
                            cornerRadius = CornerRadius(2f)
                        )
                    }
                }
            }

            BoxWithConstraints(Modifier.fillMaxWidth().height(barZoneHeightDp).align(Alignment.TopStart)) {
                val w    = maxWidth
                val n    = data.size
                val gap  = (w.value * 0.018f).coerceAtLeast(2f)
                val barW = (w.value - gap * (n - 1)) / n
                val cx   = maxIdx * (barW + gap) + barW / 2f
                val label = if (maxVal >= 1000f) "${"%.1f".format(maxVal / 1000f)}K" else "${maxVal.roundToInt()}"

                Box(
                    Modifier.offset(x = (cx - 16f).dp, y = 0.dp).width(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, fontSize = 9.sp, color = Accent, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                }
            }

            BoxWithConstraints(
                Modifier.fillMaxWidth().height(labelZoneHeightDp).align(Alignment.BottomStart)
            ) {
                val w    = maxWidth
                val n    = data.size
                val gap  = (w.value * 0.018f).coerceAtLeast(2f)
                val barW = (w.value - gap * (n - 1)) / n

                fun shortDate(d: String) = try {
                    LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d/M"))
                } catch (e: Exception) { "" }

                val labeled = buildSet {
                    add(0)
                    add(n - 1)
                    if (maxIdx != 0 && maxIdx != n - 1) add(maxIdx)
                }

                labeled.forEach { i ->
                    val (date, _) = data[i]
                    val cx = i * (barW + gap) + barW / 2f
                    val isBest = i == maxIdx
                    Box(
                        Modifier.offset(x = (cx - 14f).dp, y = 0.dp).width(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            shortDate(date),
                            fontSize   = 8.sp,
                            color      = if (isBest) Accent else TextTert,
                            fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// BAR CHART GROUPED
// FIX: moved "Fila de valores" Row outside Box/BoxWithConstraints — it now sits
//      at the bottom of Column(modifier) as a proper sibling of the chart Box.
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun BarChartGrouped(
    sessionData: List<Pair<String, WorkoutSet>>,
    colorReps: Color,
    colorWeight: Color,
    modifier: Modifier = Modifier
) {
    if (sessionData.isEmpty()) return

    val repsValues   = sessionData.map { it.second.reps.toFloat() }
    val weightValues = sessionData.map { it.second.weightKg }
    val maxReps      = repsValues.max().coerceAtLeast(1f)
    val maxWeight    = weightValues.max().coerceAtLeast(1f)
    val maxRepsIdx   = repsValues.indexOf(repsValues.max())
    val maxWIdx      = weightValues.indexOf(weightValues.max())
    val n            = sessionData.size

    val barZoneH   = 90.dp
    val labelZoneH = 18.dp
    val axisLabelW = 26.dp

    // Pre-compute last/best sets for the summary row (outside composable lambdas)
    val lastWs     = sessionData.last().second
    val bestRepsWs = sessionData[maxRepsIdx].second
    val bestWWs    = sessionData[maxWIdx].second

    Column(modifier) {

        // ── Header with axis labels ──────────────────────────────────────────
        Row(
            Modifier.fillMaxWidth().padding(horizontal = axisLabelW),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(8.dp).background(colorReps, RoundedCornerShape(2.dp)))
                Text("Reps", fontSize = 9.sp, color = colorReps, fontWeight = FontWeight.SemiBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Peso kg", fontSize = 9.sp, color = colorWeight, fontWeight = FontWeight.SemiBold)
                Box(Modifier.size(8.dp).background(colorWeight, RoundedCornerShape(2.dp)))
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Chart Box (bars + Y axes + date labels) ──────────────────────────
        Box(Modifier.height(barZoneH + labelZoneH)) {

            // Bars canvas
            Canvas(
                Modifier
                    .padding(start = axisLabelW, end = axisLabelW)
                    .fillMaxWidth()
                    .height(barZoneH)
                    .align(Alignment.TopCenter)
            ) {
                val w      = size.width
                val h      = size.height
                val padB   = 4f
                val drawH  = h - padB
                val slotW  = w / n
                val barW   = slotW * 0.38f
                val innerG = slotW * 0.04f
                val slotPad = slotW * 0.1f

                sessionData.forEachIndexed { i, (_, ws) ->
                    val rFrac    = repsValues[i] / maxReps
                    val wFrac    = weightValues[i] / maxWeight
                    val rH       = (drawH * rFrac).coerceAtLeast(4f)
                    val wH       = (drawH * wFrac).coerceAtLeast(4f)
                    val leftSlot = i * slotW + slotPad

                    val rAlpha = if (i == maxRepsIdx) 1f else 0.3f + 0.5f * rFrac
                    drawRoundRect(
                        color        = colorReps.copy(alpha = rAlpha),
                        topLeft      = Offset(leftSlot, drawH - rH + padB),
                        size         = Size(barW, rH),
                        cornerRadius = CornerRadius(barW * 0.3f)
                    )
                    if (i == maxRepsIdx) {
                        drawRoundRect(
                            color        = Accent,
                            topLeft      = Offset(leftSlot, drawH - rH + padB),
                            size         = Size(barW, 3f),
                            cornerRadius = CornerRadius(2f)
                        )
                    }

                    val wAlpha = if (i == maxWIdx) 1f else 0.3f + 0.5f * wFrac
                    drawRoundRect(
                        color        = colorWeight.copy(alpha = wAlpha),
                        topLeft      = Offset(leftSlot + barW + innerG, drawH - wH + padB),
                        size         = Size(barW, wH),
                        cornerRadius = CornerRadius(barW * 0.3f)
                    )
                    if (i == maxWIdx) {
                        drawRoundRect(
                            color        = Accent,
                            topLeft      = Offset(leftSlot + barW + innerG, drawH - wH + padB),
                            size         = Size(barW, 3f),
                            cornerRadius = CornerRadius(2f)
                        )
                    }
                }
            }

            // Left Y axis (reps)
            Column(
                Modifier
                    .width(axisLabelW)
                    .height(barZoneH)
                    .align(Alignment.TopStart),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(maxReps.roundToInt(), (maxReps / 2).roundToInt(), 0).forEach { v ->
                    Text(
                        "$v",
                        fontSize  = 7.sp,
                        color     = colorReps.copy(alpha = 0.7f),
                        textAlign = TextAlign.End,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            // Right Y axis (weight)
            Column(
                Modifier
                    .width(axisLabelW)
                    .height(barZoneH)
                    .align(Alignment.TopEnd),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(maxWeight.roundToInt(), (maxWeight / 2).roundToInt(), 0).forEach { v ->
                    Text(
                        "${v}k",
                        fontSize  = 7.sp,
                        color     = colorWeight.copy(alpha = 0.7f),
                        textAlign = TextAlign.Start,
                        modifier  = Modifier.fillMaxWidth()
                    )
                }
            }

            // Date labels
            BoxWithConstraints(
                Modifier
                    .padding(start = axisLabelW, end = axisLabelW)
                    .fillMaxWidth()
                    .height(labelZoneH)
                    .align(Alignment.BottomCenter)
            ) {
                val w       = maxWidth.value
                val slotW   = w / n
                val slotPad = slotW * 0.1f
                val barW    = slotW * 0.38f

                fun shortDate(d: String) = try {
                    LocalDate.parse(d).format(DateTimeFormatter.ofPattern("d/M"))
                } catch (e: Exception) { "" }

                val labeled = buildSet {
                    add(0); add(n - 1)
                    if (maxRepsIdx != 0 && maxRepsIdx != n - 1) add(maxRepsIdx)
                    if (maxWIdx != 0 && maxWIdx != n - 1) add(maxWIdx)
                }

                labeled.forEach { i ->
                    val cx   = i * slotW + slotPad + barW
                    val isPR = i == maxRepsIdx || i == maxWIdx
                    Box(
                        Modifier.offset(x = (cx - 12f).dp).width(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            shortDate(sessionData[i].first),
                            fontSize   = 7.sp,
                            color      = if (isPR) Accent else TextTert,
                            fontWeight = if (isPR) FontWeight.Bold else FontWeight.Normal,
                            textAlign  = TextAlign.Center
                        )
                    }
                }
            } // closes BoxWithConstraints (date labels)
        } // closes Box (chart)

        // ── Summary row (last session · best reps · best weight) ─────────────
        Spacer(Modifier.height(6.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(Surface2, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text("Última sesión", fontSize = 9.sp, color = TextSec)
                Text(
                    "${lastWs.reps}r × ${lastWs.weightKg.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }}kg",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black,
                    color      = Accent
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Mejor reps", fontSize = 9.sp, color = TextSec)
                Text(
                    "${bestRepsWs.reps}r",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black,
                    color      = colorReps
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Mejor peso", fontSize = 9.sp, color = TextSec)
                Text(
                    "${bestWWs.weightKg.let { if (it == it.toLong().toFloat()) it.toLong().toString() else it.toString() }}kg",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Black,
                    color      = colorWeight
                )
            }
        }
    } // closes Column(modifier)
} // closes BarChartGrouped


@Composable
fun ConsistencyCard(
    sessions: List<Session>,
    exerciseId: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    val exSessions = sessions
        .filter { s -> s.sets.any { it.exerciseId == exerciseId } }
        .sortedBy { it.date }

    if (exSessions.isEmpty()) return

    val firstDate = try { LocalDate.parse(exSessions.first().date) } catch (e: Exception) { return }
    val today       = LocalDate.now()
    val firstMonday = firstDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val lastMonday  = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    data class WeekInfo(val monday: LocalDate, val trained: Boolean, val isPrWeek: Boolean)

    val globalMaxE1rm = exSessions.flatMap { s ->
        s.sets.filter { it.exerciseId == exerciseId }
            .map { estimatedOneRM(it.weightKg, it.reps) }
    }.maxOrNull() ?: 0f

    val globalMaxVol = exSessions.maxOfOrNull { s ->
        s.sets.filter { it.exerciseId == exerciseId }
            .sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
    } ?: 0f

    val weeks = mutableListOf<WeekInfo>()
    var cursor = firstMonday
    while (!cursor.isAfter(lastMonday)) {
        val sunday = cursor.plusDays(6)
        val weekSessions = exSessions.filter { s ->
            try {
                val d = LocalDate.parse(s.date)
                !d.isBefore(cursor) && !d.isAfter(sunday)
            } catch (e: Exception) { false }
        }
        val trained = weekSessions.isNotEmpty()
        val isPrWeek = weekSessions.any { s ->
            val sets = s.sets.filter { it.exerciseId == exerciseId }
            val e1rm = bestE1RM(sets)
            val vol  = sets.sumOf { (it.reps * it.weightKg).toDouble() }.toFloat()
            (globalMaxE1rm > 0f && e1rm >= globalMaxE1rm * 0.999f) ||
                    (globalMaxVol  > 0f && vol  >= globalMaxVol  * 0.999f)
        }
        weeks.add(WeekInfo(cursor, trained, isPrWeek))
        cursor = cursor.plusWeeks(1)
    }

    val totalWeeks   = weeks.size
    val trainedWeeks = weeks.count { it.trained }
    val adherence    = if (totalWeeks > 0) (trainedWeeks * 100) / totalWeeks else 0
    val streak = run {
        var s = 0
        for (w in weeks.reversed()) {
            if (w.trained) s++ else if (w.monday != lastMonday) break
        }
        s
    }

    val blockSize = when {
        totalWeeks <= 8  -> 32.dp
        totalWeeks <= 12 -> 26.dp
        totalWeeks <= 16 -> 22.dp
        else             -> 18.dp
    }
    val blockGap = when {
        totalWeeks <= 8  -> 5.dp
        totalWeeks <= 12 -> 4.dp
        else             -> 3.dp
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        color    = Surface1,
        border   = BorderStroke(1.dp, Border)
    ) {
        Column(Modifier.padding(14.dp)) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Column {
                    Text("Adherencia", fontSize = 10.sp, color = TextSec)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            "$adherence",
                            fontSize   = 22.sp,
                            fontWeight = FontWeight.Black,
                            color      = Accent
                        )
                        Text(
                            "%",
                            fontSize   = 12.sp,
                            color      = Accent,
                            fontWeight = FontWeight.Bold,
                            modifier   = Modifier.padding(bottom = 3.dp)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Racha actual", fontSize = 10.sp, color = TextSec)
                    Text(
                        "$streak sem",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        color      = if (streak >= 3) GreenOk else TextPrim
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Total", fontSize = 10.sp, color = TextSec)
                    Text(
                        "$trainedWeeks/$totalWeeks sem",
                        fontSize   = 16.sp,
                        fontWeight = FontWeight.Black,
                        color      = TextPrim
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Border)
            Spacer(Modifier.height(10.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(blockGap),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(weeks.size) { i ->
                    val week          = weeks[i]
                    val isCurrentWeek = week.monday == lastMonday

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            Modifier
                                .size(blockSize)
                                .background(
                                    color = when {
                                        week.isPrWeek -> Accent.copy(alpha = 0.18f)
                                        week.trained  -> color.copy(alpha = 0.22f)
                                        else          -> Surface2
                                    },
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .then(when {
                                    week.isPrWeek -> Modifier.border(1.dp,   Accent.copy(alpha = 0.55f), RoundedCornerShape(5.dp))
                                    isCurrentWeek -> Modifier.border(1.dp,   color.copy(alpha = 0.45f),  RoundedCornerShape(5.dp))
                                    !week.trained -> Modifier.border(0.5.dp, Border,                     RoundedCornerShape(5.dp))
                                    else          -> Modifier
                                }),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                week.isPrWeek ->
                                    Text("★", fontSize = (blockSize.value * 0.38f).sp, color = Accent)
                                week.trained ->
                                    Box(
                                        Modifier
                                            .size(blockSize * 0.3f)
                                            .background(color, CircleShape)
                                    )
                            }
                        }

                        if (i == 0 || i % 4 == 3 || i == weeks.size - 1) {
                            Text(
                                "S${i + 1}",
                                fontSize  = 7.sp,
                                color     = TextTert,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Spacer(Modifier.height(9.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(Modifier.size(8.dp).background(color.copy(alpha = 0.22f), RoundedCornerShape(2.dp)))
                    Text("Entrenado", fontSize = 9.sp, color = TextSec)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Accent.copy(alpha = 0.18f), RoundedCornerShape(2.dp))
                            .border(1.dp, Accent.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                    )
                    Text("PR semana", fontSize = 9.sp, color = TextSec)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(Surface2, RoundedCornerShape(2.dp))
                            .border(0.5.dp, Border, RoundedCornerShape(2.dp))
                    )
                    Text("Sin sesión", fontSize = 9.sp, color = TextSec)
                }
            }
        }
    }
}
