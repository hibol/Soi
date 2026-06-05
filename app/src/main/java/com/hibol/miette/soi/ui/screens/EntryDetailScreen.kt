package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.viewmodel.EntryDetailViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryDetailScreen(
    navController: NavController,
    entryId: Long
) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: EntryDetailViewModel = viewModel(
        factory = EntryDetailViewModel.Factory(
            app.container.entryRepository,
            app.container.emotionRepository
        )
    )

    val entry by viewModel.entry.collectAsState()
    val emotions by viewModel.emotions.collectAsState()
    val primaryEmotions by viewModel.primaryEmotions.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val dreamDetail by viewModel.dreamDetail.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()

    var showDeleteDialog by remember { mutableStateOf(false) }
    val prefs = remember { context.getSharedPreferences("soi_prefs", android.content.Context.MODE_PRIVATE) }
    var showConstellation by remember { mutableStateOf(prefs.getBoolean("emotion_view_constellation", true)) }

    val extended = extendedColorScheme()

    LaunchedEffect(entryId) {
        viewModel.load(entryId)
    }

    LaunchedEffect(isDeleted) {
        if (isDeleted) navController.popBackStack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette entrée ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete(entryId)
                    }
                ) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Annuler") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (entry?.type) {
                            EntryType.DREAM -> "Rêve"
                            EntryType.SESSION -> "Session"
                            EntryType.LIFE_EVENT -> "Événement"
                            null -> ""
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        entry?.let {
                            navController.navigate(Routes.newEntry(it.type, entryId = entryId))
                        }
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Modifier")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Supprimer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        entry?.let { e ->
            val colorFamily = extended.colorFamilyForType(e.type)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Date et heure
                Text(
                    text = Instant.ofEpochMilli(e.entryDate)
                        .atZone(ZoneId.systemDefault())
                        .format(
                            DateTimeFormatter.ofPattern(
                                "EEEE d MMMM yyyy — HH:mm",
                                Locale.FRENCH
                            )
                        ).replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = colorFamily.color
                )

                // Qualité du souvenir — rêves seulement
                dreamDetail?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Souvenir : ",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = it.memoryQuality.replaceFirstChar { c -> c.uppercase() },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Texte
                if (!e.text.isNullOrBlank()) {
                    Text(
                        text = e.text,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                // Émotions
                if (emotions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Émotions", style = MaterialTheme.typography.titleSmall)
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = showConstellation,
                                onClick = {
                                    showConstellation = true
                                    prefs.edit().putBoolean("emotion_view_constellation", true).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) { Text("Constellation", style = MaterialTheme.typography.labelSmall) }
                            SegmentedButton(
                                selected = !showConstellation,
                                onClick = {
                                    showConstellation = false
                                    prefs.edit().putBoolean("emotion_view_constellation", false).apply()
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) { Text("Liste", style = MaterialTheme.typography.labelSmall) }
                        }
                    }

                    if (showConstellation) {
                        com.hibol.miette.soi.ui.components.EmotionConstellationView(
                            primaryEmotions = primaryEmotions,
                            selections = emotions,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            emotions.forEach { (emotion, intensity) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = emotion.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        val emotionColor = androidx.compose.ui.graphics.Color(
                                            emotion.color.toColorInt()
                                        )
                                        for (i in 1..5) {
                                            Box(
                                                modifier = Modifier
                                                    .size(10.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (i <= intensity) emotionColor
                                                        else emotionColor.copy(alpha = 0.2f)
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Tags
                if (tags.isNotEmpty()) {
                    Text("Tags", style = MaterialTheme.typography.titleSmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(tags) { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag.label) }
                            )
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}