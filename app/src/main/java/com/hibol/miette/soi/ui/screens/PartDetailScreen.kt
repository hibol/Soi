package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.ui.components.TraitPicker
import com.hibol.miette.soi.ui.viewmodel.PartDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(navController: NavController, partId: Long?, initialRole: String? = null) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: PartDetailViewModel = viewModel(
        factory = PartDetailViewModel.Factory(
            app.container.partRepository,
            app.container.profileRepository
        )
    )

    val selectedTraits by viewModel.selectedTraits.collectAsState()
    val allPresetTraits by viewModel.allPresetTraits.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val savedId by viewModel.savedPartId.collectAsState()
    val saveError by viewModel.saveError.collectAsState()

    LaunchedEffect(partId) { viewModel.load(partId, initialRole) }
    LaunchedEffect(savedId) { if (savedId != null) navController.popBackStack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(partTitle(role = viewModel.role, isNew = partId == null)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.save() }) {
                        Text("Sauvegarder")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Nom
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Nom *") },
                    placeholder = { Text("4 caractères min. pour la détection automatique") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = saveError != null && viewModel.name.isBlank(),
                    supportingText = saveError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } }
                )

                // Âge
                OutlinedTextField(
                    value = viewModel.age,
                    onValueChange = { viewModel.age = it },
                    label = { Text("Âge (approximatif, optionnel)") },
                    placeholder = { Text("ex. ~8 ans, très ancienne") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp),
                    minLines = 4
                )

                HorizontalDivider()

                // Traits de caractère
                Text("Traits de caractère", style = MaterialTheme.typography.labelLarge)
                TraitPicker(
                    selectedTraits = selectedTraits,
                    allTraits = allPresetTraits,
                    onAddTrait = { viewModel.addTrait(it) },
                    onAddByLabel = { viewModel.addTraitByLabel(it) },
                    onRemoveTrait = { viewModel.removeTrait(it) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

private fun partTitle(role: String?, isNew: Boolean): String = when {
    isNew -> when (role) {
        "exile" -> "Nouvel exilé"
        "manager" -> "Nouveau manager"
        "firefighter" -> "Nouveau pompier"
        "other" -> "Nouvelle partie"
        else -> "Nouvelle Partie"
    }
    else -> when (role) {
        "exile" -> "Modifier l'exilé"
        "manager" -> "Modifier le manager"
        "firefighter" -> "Modifier le pompier"
        else -> "Modifier"
    }
}
