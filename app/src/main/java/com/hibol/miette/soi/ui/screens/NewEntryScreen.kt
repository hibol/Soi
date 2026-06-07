package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.components.EmotionPicker
import com.hibol.miette.soi.ui.components.EmotionSelection
import com.hibol.miette.soi.ui.components.TagPicker
import com.hibol.miette.soi.ui.viewmodel.NewEntryViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    navController: NavController,
    type: EntryType,
    initialDate: Long? = null,
    entryId: Long? = null
) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: NewEntryViewModel = viewModel(
        factory = NewEntryViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository,
            app.container.emotionRepository,
            app.container.tagRepository,
            app.container.partRepository
        )
    )

    val primaryEmotions by viewModel.primaryEmotions.collectAsState()
    val secondaryEmotions by viewModel.secondaryEmotions.collectAsState()
    val allTags by viewModel.allTags.collectAsState(initial = emptyList())
    val entrySaved by viewModel.entrySaved.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()

    val isEditing = entryId != null

    // État local du formulaire
    var text by remember { mutableStateOf("") }
    var memoryQuality by remember { mutableStateOf("clair") }
    var selectedEmotions by remember { mutableStateOf<List<EmotionSelection>>(emptyList()) }
    var selectedTags by remember { mutableStateOf<List<String>>(emptyList()) }
    var entryDate by remember { mutableStateOf(initialDate ?: System.currentTimeMillis()) }
    var isBlurred by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entryDate)
    val timePickerState = rememberTimePickerState(
        initialHour = Instant.ofEpochMilli(entryDate).atZone(ZoneId.systemDefault()).hour,
        initialMinute = Instant.ofEpochMilli(entryDate).atZone(ZoneId.systemDefault()).minute,
        is24Hour = true
    )

    // Charger l'entrée existante si édition
    LaunchedEffect(entryId) {
        if (entryId != null) viewModel.loadEntry(entryId)
    }

    // Pré-remplir le formulaire une fois chargé
    LaunchedEffect(isLoaded) {
        if (isLoaded) {
            text = viewModel.initialText.value ?: ""
            memoryQuality = viewModel.initialMemoryQuality.value
            selectedEmotions = viewModel.initialEmotions.value
            selectedTags = viewModel.initialTags.value
            viewModel.initialDate.value?.let { entryDate = it }
            isBlurred = viewModel.initialIsBlurred.value
        }
    }

    LaunchedEffect(entrySaved) {
        if (entrySaved) navController.popBackStack()
    }

    val title = when {
        isEditing -> when (type) {
            EntryType.DREAM -> "Modifier le rêve"
            EntryType.SESSION -> "Modifier la session"
            EntryType.LIFE_EVENT -> "Modifier l'événement"
        }
        else -> when (type) {
            EntryType.DREAM -> "Nouveau rêve"
            EntryType.SESSION -> "Nouvelle session"
            EntryType.LIFE_EVENT -> "Nouvel événement"
        }
    }

    // Date picker dialog
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
        ) { DatePicker(state = datePickerState) }
    }

    // Time picker dialog
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
            text = { TimePicker(state = timePickerState) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { isBlurred = !isBlurred }) {
                        Icon(
                            imageVector = if (isBlurred) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (isBlurred) "Texte flouté" else "Texte visible"
                        )
                    }
                    TextButton(
                        onClick = {
                            when (type) {
                                EntryType.DREAM -> viewModel.saveDream(
                                    entryId = entryId,
                                    text = text.ifBlank { null },
                                    memoryQuality = memoryQuality,
                                    entryDate = entryDate,
                                    emotions = selectedEmotions,
                                    tags = selectedTags,
                                    isBlurred = isBlurred
                                )
                                EntryType.SESSION -> viewModel.saveSession(
                                    entryId = entryId,
                                    text = text.ifBlank { null },
                                    entryDate = entryDate,
                                    emotions = selectedEmotions,
                                    tags = selectedTags,
                                    isBlurred = isBlurred
                                )
                                EntryType.LIFE_EVENT -> viewModel.saveEvent(
                                    entryId = entryId,
                                    text = text.ifBlank { null },
                                    entryDate = entryDate,
                                    emotions = selectedEmotions,
                                    tags = selectedTags,
                                    isBlurred = isBlurred
                                )
                            }
                        }
                    ) { Text("Enregistrer") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Date
            Text("Date", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        Instant.ofEpochMilli(entryDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.FRENCH))
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        Instant.ofEpochMilli(entryDate)
                            .atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                }
            }

            // Qualité du souvenir — rêves seulement
            if (type == EntryType.DREAM) {
                Text("Qualité du souvenir", style = MaterialTheme.typography.titleSmall)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf("flou", "partiel", "clair").forEachIndexed { index, quality ->
                        SegmentedButton(
                            selected = memoryQuality == quality,
                            onClick = { memoryQuality = quality },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text(quality.replaceFirstChar { it.uppercase() })
                        }
                    }
                }
            }

            // Texte libre
            val textLabel = when (type) {
                EntryType.DREAM -> "Récit"
                EntryType.SESSION -> "Récit"
                EntryType.LIFE_EVENT -> "Description"
            }
            val textPlaceholder = when (type) {
                EntryType.DREAM -> "Contenu du rêve..."
                EntryType.SESSION -> "Contenu de la session..."
                EntryType.LIFE_EVENT -> "Contenu de l'événement..."
            }
            Text(textLabel, style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text(textPlaceholder) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                maxLines = 10
            )

            // Émotions
            val emotionLabel = when (type) {
                EntryType.DREAM -> "Émotions au réveil"
                EntryType.SESSION -> "Émotions"
                EntryType.LIFE_EVENT -> "Émotions"
            }
            Text(emotionLabel, style = MaterialTheme.typography.titleSmall)
            EmotionPicker(
                primaryEmotions = primaryEmotions,
                secondaryEmotions = secondaryEmotions,
                selected = selectedEmotions,
                onSelectionChanged = { selectedEmotions = it }
            )

            // Tags
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