package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    entries: List<Entry>,
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val entriesByDate = entries.groupBy {
        LocalDate.ofEpochDay(it.entryDate / 86400000L)
    }

    Column(
        modifier = modifier.pointerInput(currentMonth) {
            var totalDrag = 0f
            val threshold = with(density) { 80.dp.toPx() }
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (totalDrag > threshold) onMonthChange(currentMonth.minusMonths(1))
                    else if (totalDrag < -threshold) onMonthChange(currentMonth.plusMonths(1))
                    totalDrag = 0f
                },
                onDragCancel = { totalDrag = 0f },
                onHorizontalDrag = { _, dragAmount -> totalDrag += dragAmount }
            )
        }
    ) {
        CalendarHeader(
            yearMonth = currentMonth,
            onPrevious = { onMonthChange(currentMonth.minusMonths(1)) },
            onNext = { onMonthChange(currentMonth.plusMonths(1)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        DayOfWeekHeader()

        Spacer(modifier = Modifier.height(4.dp))

        val days = buildCalendarDays(currentMonth)

        days.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    DayCell(
                        day = day,
                        hasEntries = (entriesByDate[day.date]?.isNotEmpty()) == true,
                        onClick = { if (day.isCurrentMonth) onDayClick(day.date) },
                        onLongClick = { if (day.isCurrentMonth) onDayLongClick(day.date) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Mois précédent")
        }

        Text(
            text = "${yearMonth.month.getDisplayName(TextStyle.FULL, Locale.FRENCH)} ${yearMonth.year}",
            style = MaterialTheme.typography.titleMedium
        )

        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Mois suivant")
        }
    }
}

@Composable
private fun DayOfWeekHeader() {
    val days = listOf("L", "M", "M", "J", "V", "S", "D")
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEach { day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DayCell(
    day: CalendarDay,
    hasEntries: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = day.date == LocalDate.now()
    val primary = MaterialTheme.colorScheme.primary
    val textColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        isToday || hasEntries -> primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val glow = if (day.isCurrentMonth && hasEntries) Shadow(
        color = primary.copy(alpha = 0.75f),
        offset = Offset.Zero,
        blurRadius = 18f
    ) else null
    val fontWeight = if (isToday || hasEntries) FontWeight.Bold else null

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .combinedClickable(
                enabled = day.isCurrentMonth,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall.copy(shadow = glow),
            color = textColor,
            fontWeight = fontWeight
        )
    }
}
