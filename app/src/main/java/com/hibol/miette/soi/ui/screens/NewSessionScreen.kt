package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.ui.components.EmotionPicker
import com.hibol.miette.soi.ui.components.EmotionSelection
import com.hibol.miette.soi.ui.components.TagPicker
import com.hibol.miette.soi.ui.viewmodel.NewEntryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSessionScreen(
    navController: NavController,
    initialDate: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: NewEntryViewModel = viewModel(
        factory = NewEntryViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository,
            app.container.emotionRepository,
            app.container.tagRepository
        )
    )

    val primaryEmotions by viewModel.primaryEmotions.collectAsState()
    val secondaryEmotions by viewModel.secondaryEmotions.collectAsState()
    val allTags by viewModel.allTags.collectAsState(initial = emptyList())
    val entrySaved by viewModel.entrySaved.collectAsState()

    var text by remember { mutableStateOf("") }
    var selectedEmotions by remember { mutableStateOf<List<EmotionSelection>>(emptyList()) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var entryDate by remember { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = entryDate
    )
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(
        initialHour = java.time.LocalTime.now().hour,
        initialMinute = java.time.LocalTime.now().minute,
        is24Hour = true
    )

    val scope = rememberCoroutineScope()

    LaunchedEffect(entrySaved) {
        if (entrySaved) navController.popBackStack()
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { entryDate = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annuler") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val cal = java.util.Calendar.getInstance()
                    cal.timeInMillis = entryDate
                    cal.set(java.util.Calendar.HOUR_OF_DAY, timePickerState.hour)
                    cal.set(java.util.Calendar.MINUTE, timePickerState.minute)
                    entryDate = cal.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Annuler") }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nouvelle session") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                viewModel.saveSession(
                                    text = text.ifBlank { null },
                                    entryDate = entryDate,
                                    emotions = selectedEmotions,
                                    tags = selectedTags
                                )
                            }
                        }
                    ) {
                        Text("Enregistrer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date
            Text("Date", style = MaterialTheme.typography.titleSmall)
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = java.time.Instant.ofEpochMilli(entryDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale.FRENCH))
                )
            }
            OutlinedButton(
                onClick = { showTimePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = java.time.Instant.ofEpochMilli(entryDate)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalTime()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                )
            }

            Text("Récit", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Contenu de la session...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 10
            )

            Text("Émotions", style = MaterialTheme.typography.titleSmall)
            EmotionPicker(
                primaryEmotions = primaryEmotions,
                secondaryEmotions = secondaryEmotions,
                selected = selectedEmotions,
                onSelectionChanged = { selectedEmotions = it },
                modifier = Modifier.heightIn(max = 400.dp)
            )

            Text("Tags", style = MaterialTheme.typography.titleSmall)
            TagPicker(
                selectedTags = selectedTags,
                suggestions = allTags,
                onTagAdded = { tag ->
                    if (tag !in selectedTags) selectedTags = selectedTags + tag
                },
                onTagRemoved = { tag ->
                    selectedTags = selectedTags - tag
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}