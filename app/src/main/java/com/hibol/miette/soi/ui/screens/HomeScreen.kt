package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.screens.home.CalendarView
import com.hibol.miette.soi.ui.screens.home.EntryTypePicker
import com.hibol.miette.soi.ui.screens.home.MonthEntryList
import com.hibol.miette.soi.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository
        )
    )

    val entries by viewModel.entries.collectAsState()
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    var pickerDate by remember { mutableStateOf<LocalDate?>(null) }

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Entrées du mois courant, groupées par jour (jours les plus récents en premier)
    val groupedEntries: List<Pair<LocalDate, List<Entry>>> = remember(entries, currentMonth) {
        entries
            .filter { entry ->
                val date = Instant.ofEpochMilli(entry.entryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                YearMonth.from(date) == currentMonth
            }
            .groupBy { entry ->
                Instant.ofEpochMilli(entry.entryDate)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
            .entries
            .sortedByDescending { it.key }
            .map { (date, dayEntries) ->
                date to dayEntries.sortedByDescending { it.entryDate }
            }
    }

    // Index de l'en-tête de chaque jour dans la liste plate (stickyHeader + items)
    // Permet de savoir vers quel index scroller quand on tape un jour du calendrier
    val dayIndexMap: Map<LocalDate, Int> = remember(groupedEntries) {
        buildMap {
            var idx = 0
            groupedEntries.forEach { (date, dayEntries) ->
                put(date, idx)           // l'en-tête du jour est à cet index
                idx += 1 + dayEntries.size  // 1 header + n entrées
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                pickerDate = null
                showPicker = true
            }) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle entrée")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CalendarView(
                entries = entries,
                currentMonth = currentMonth,
                onMonthChange = { newMonth ->
                    currentMonth = newMonth
                    selectedDate = null
                },
                selectedDate = selectedDate,
                onDayClick = { date ->
                    // Scroll uniquement si ce jour a des entrées
                    dayIndexMap[date]?.let { index ->
                        selectedDate = date
                        coroutineScope.launch {
                            listState.animateScrollToItem(index)
                        }
                    }
                },
                onDayLongClick = { date ->
                    pickerDate = date
                    showPicker = true
                },
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            MonthEntryList(
                groupedEntries = groupedEntries,
                listState = listState,
                onEntryClick = { entryId ->
                    navController.navigate(Routes.entryDetail(entryId))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }

        if (showPicker) {
            EntryTypePicker(
                onTypeSelected = { type: EntryType ->
                    showPicker = false
                    val date = pickerDate
                    if (date != null) {
                        val millis = date
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                        navController.navigate(Routes.newEntry(type, millis))
                    } else {
                        navController.navigate(Routes.newEntry(type))
                    }
                    pickerDate = null
                },
                onDismiss = {
                    showPicker = false
                    pickerDate = null
                }
            )
        }
    }
}
