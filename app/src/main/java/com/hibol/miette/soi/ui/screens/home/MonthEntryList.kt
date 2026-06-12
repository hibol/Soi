package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.viewmodel.MiniConstellationData
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MonthEntryList(
    groupedEntries: List<Pair<LocalDate, List<Entry>>>,
    emotionColors: Map<Long, MiniConstellationData>,
    listState: LazyListState,
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    if (groupedEntries.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Aucune entrée ce mois-ci",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val extended = extendedColorScheme()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        groupedEntries.forEach { (date, dayEntries) ->
            item(key = "day_${date.toEpochDay()}") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Column {
                        Text(
                            text = date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)
                        )
                        dayEntries.forEach { entry ->
                            DaySheetEntryRow(
                                entry = entry,
                                onClick = { onEntryClick(entry.id) },
                                extended = extended,
                                constellation = emotionColors[entry.id]
                            )
                        }
                    }
                }
            }
        }
    }
}
