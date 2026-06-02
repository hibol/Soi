package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    entries: List<Entry>,
    currentMonth: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    selectedDate: LocalDate?,
    onDayClick: (LocalDate) -> Unit,
    onDayLongClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val entriesByDate = entries.groupBy {
        LocalDate.ofEpochDay(it.entryDate / 86400000L)
    }

    Column(modifier = modifier) {
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
                        dayEntries = entriesByDate[day.date] ?: emptyList(),
                        isSelected = day.isCurrentMonth && day.date == selectedDate,
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
    dayEntries: List<Entry>,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = day.date == LocalDate.now()
    // Capturé ici car drawBehind s'exécute dans un DrawScope sans accès à MaterialTheme
    val primaryColor = MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .drawBehind {
                if (isSelected) {
                    val radius = size.minDimension / 3f
                    drawContext.canvas.nativeCanvas.drawCircle(
                        center.x, center.y, radius,
                        android.graphics.Paint().apply {
                            isAntiAlias = true
                            color = primaryColor.copy(alpha = 0.4f).toArgb()
                            maskFilter = android.graphics.BlurMaskFilter(
                                radius,
                                android.graphics.BlurMaskFilter.Blur.NORMAL
                            )
                        }
                    )
                }
            }
            .clip(CircleShape)
            .combinedClickable(
                enabled = day.isCurrentMonth,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodySmall,
            color = when {
                !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isSelected || isToday -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (isSelected || isToday) androidx.compose.ui.text.font.FontWeight.Bold else null
        )

        if (dayEntries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            EntryDots(entries = dayEntries)
        }
    }
}

@Composable
private fun EntryDots(entries: List<Entry>) {
    val extended = extendedColorScheme()
    val types = entries.map { it.type }.distinct().take(3)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        types.forEach { type ->
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(extended.colorFamilyForType(type).color)
            )
        }
    }
}