package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.viewmodel.MiniConstellationData
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
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

    LazyColumn(state = listState, modifier = modifier) {
        groupedEntries.forEach { (date, dayEntries) ->
            stickyHeader(key = "header_${date.toEpochDay()}") {
                // Surface assure que l'en-tête sticky cache les lignes qui défilent dessous
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                HorizontalDivider()
            }
            items(items = dayEntries, key = { "entry_${it.id}" }) { entry ->
                DaySheetEntryRow(
                    entry = entry,
                    onClick = { onEntryClick(entry.id) },
                    extended = extended,
                    constellation = emotionColors[entry.id]
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}
