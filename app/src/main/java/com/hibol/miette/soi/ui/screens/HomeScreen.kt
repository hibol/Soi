package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.screens.home.CalendarView
import com.hibol.miette.soi.ui.screens.home.DaySheet
import com.hibol.miette.soi.ui.screens.home.EntryTypePicker
import com.hibol.miette.soi.ui.viewmodel.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository
        )
    )

    val entries by viewModel.entries.collectAsState()
    var showPicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val entriesByDate = entries.groupBy {
        Instant.ofEpochMilli(it.entryDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nouvelle entrée")
            }
        }
    ) { innerPadding ->
        CalendarView(
            entries = entries,
            onDayClick = { date: LocalDate -> selectedDate = date },
            modifier = Modifier.padding(innerPadding)
        )

        // FAB → EntryTypePicker (sans date spécifique)
        if (showPicker) {
            EntryTypePicker(
                onTypeSelected = { type ->
                    showPicker = false
                    navController.navigate(
                        when (type) {
                            EntryType.DREAM -> Routes.NEW_DREAM
                            EntryType.SESSION -> Routes.NEW_SESSION
                            EntryType.LIFE_EVENT -> Routes.NEW_EVENT
                        }
                    )
                },
                onDismiss = { showPicker = false }
            )
        }

        // Tap sur un jour → DaySheet
        selectedDate?.let { date ->
            DaySheet(
                date = date,
                entries = entriesByDate[date] ?: emptyList(),
                onEntryClick = { entryId ->
                    selectedDate = null
                    // navigation vers détail — à venir
                },
                onNewEntry = { type, date ->
                    selectedDate = null
                    val millis = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    navController.navigate(
                        when (type) {
                            "dream" -> Routes.newDream(millis)
                            "session" -> Routes.newSession(millis)
                            else -> Routes.newEvent(millis)
                        }
                    )
                },
                onDismiss = { selectedDate = null }
            )
        }
    }
}