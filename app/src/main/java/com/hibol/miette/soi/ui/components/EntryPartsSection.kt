package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.db.EntryPartRow
import com.hibol.miette.soi.data.entity.Part

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EntryPartsSection(
    entryParts: List<EntryPartRow>,
    availableParts: List<Part>,
    onConfirm: (Long) -> Unit,
    onRemove: (Long) -> Unit,
    onLink: (Long) -> Unit,
    onRedetect: () -> Unit,
    onNavigateToPart: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPartPicker by remember { mutableStateOf(false) }

    if (showPartPicker) {
        PartPickerSheet(
            parts = availableParts,
            onSelect = { partId ->
                showPartPicker = false
                onLink(partId)
            },
            onDismiss = { showPartPicker = false }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Parties mentionnées", style = MaterialTheme.typography.titleSmall)
            Row {
                IconButton(onClick = onRedetect) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Relancer la détection",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = { showPartPicker = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Lier une partie manuellement",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        if (entryParts.isEmpty()) {
            Text(
                "Aucune partie détectée",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                entryParts.forEach { row ->
                    if (row.source == "explicit") {
                        // Partie confirmée : chip rempli, tap → fiche, ✕ → supprime le lien
                        FilterChip(
                            selected = true,
                            onClick = { onNavigateToPart(row.partId) },
                            label = { Text(row.partName) },
                            leadingIcon = {
                                Icon(Icons.Default.Check, null, Modifier.size(16.dp))
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemove(row.partId) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Supprimer le lien", Modifier.size(14.dp))
                                }
                            }
                        )
                    } else {
                        // Partie suggérée : chip outlined, tap = confirme, ✕ = ignore
                        InputChip(
                            selected = false,
                            onClick = { onConfirm(row.partId) },
                            label = { Text(row.partName) },
                            trailingIcon = {
                                IconButton(
                                    onClick = { onRemove(row.partId) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, "Ignorer", Modifier.size(14.dp))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PartPickerSheet(
    parts: List<Part>,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    val filtered = parts.filter { it.name.contains(search, ignoreCase = true) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                "Lier une Partie",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                label = { Text("Rechercher") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Text(
                    "Aucune partie disponible",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                filtered.forEach { part ->
                    Text(
                        part.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(part.id) }
                            .padding(vertical = 12.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
