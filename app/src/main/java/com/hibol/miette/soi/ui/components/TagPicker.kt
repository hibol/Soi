package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Tag

@Composable
fun TagPicker(
    selectedTags: List<String>,
    suggestions: List<Tag>,
    onTagAdded: (String) -> Unit,
    onTagRemoved: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    val filteredSuggestions = suggestions
        .filter { it.label.startsWith(input.lowercase().trim()) && it.label !in selectedTags }
        .take(5)

    Column(modifier = modifier) {
        // Tags sélectionnés
        if (selectedTags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(selectedTags) { tag ->
                    InputChip(
                        selected = true,
                        onClick = { onTagRemoved(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Retirer",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        // Champ de saisie
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Ajouter un tag") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (input.isNotBlank()) {
                        onTagAdded(input.trim().lowercase())
                        input = ""
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Suggestions
        if (input.isNotBlank() && filteredSuggestions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(filteredSuggestions) { tag ->
                    SuggestionChip(
                        onClick = {
                            onTagAdded(tag.label)
                            input = ""
                        },
                        label = { Text(tag.label) }
                    )
                }
            }
        }
    }
}