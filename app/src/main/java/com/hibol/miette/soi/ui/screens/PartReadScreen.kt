package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.screens.home.DaySheetEntryRow
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.viewmodel.PartReadViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PartReadScreen(navController: NavController, partId: Long) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: PartReadViewModel = viewModel(
        factory = PartReadViewModel.Factory(
            app.container.partRepository,
            app.container.entryRepository,
            app.container.emotionRepository
        )
    )

    val part by viewModel.part.collectAsState()
    val traits by viewModel.traits.collectAsState()
    val isDeleted by viewModel.isDeleted.collectAsState()
    val partEntries by viewModel.partEntries.collectAsState()
    val emotionColors by viewModel.emotionColors.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    val extended = extendedColorScheme()
    val dateFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)

    LaunchedEffect(partId) { viewModel.load(partId) }
    LaunchedEffect(isDeleted) { if (isDeleted) navController.popBackStack() }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Supprimer cette partie ?") },
            text = { Text("Cette action est irréversible.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete(partId)
                }) {
                    Text("Supprimer", color = MaterialTheme.colorScheme.error)
                }
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
                    Text(part?.role?.let { roleLabel(it) } ?: "Partie")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate(Routes.partDetail(partId))
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
        part?.let { p ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Nom
                Text(p.name, style = MaterialTheme.typography.headlineMedium)

                // Âge
                p.age?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // Description
                if (!p.description.isNullOrBlank()) {
                    Text(p.description, style = MaterialTheme.typography.bodyLarge)
                }

                // Traits de caractère
                if (traits.isNotEmpty()) {
                    Text("Traits de caractère", style = MaterialTheme.typography.titleSmall)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        traits.forEach { trait ->
                            AssistChip(onClick = {}, label = { Text(trait.label) })
                        }
                    }
                }

                // Apparitions dans les entrées
                Text("Apparitions", style = MaterialTheme.typography.titleSmall)
                if (partEntries.isEmpty()) {
                    Text(
                        "Aucune apparition confirmée",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    val grouped = partEntries
                        .groupBy { entry ->
                            Instant.ofEpochMilli(entry.entryDate)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        }
                        .entries
                        .sortedByDescending { it.key }

                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        grouped.forEach { (date, dayEntries) ->
                            Text(
                                text = date.format(dateFormatter).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            HorizontalDivider()
                            dayEntries.forEach { entry ->
                                DaySheetEntryRow(
                                    entry = entry,
                                    onClick = { navController.navigate(Routes.entryDetail(entry.id)) },
                                    extended = extended,
                                    emotionColors = emotionColors[entry.id]
                                )
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        } ?: Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}
