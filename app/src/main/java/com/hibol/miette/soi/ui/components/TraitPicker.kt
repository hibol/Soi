package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.PartTrait
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TraitPicker(
    selectedTraits: List<PartTrait>,
    allTraits: List<PartTrait>,
    onAddTrait: (PartTrait) -> Unit,
    onAddByLabel: (String) -> Unit,
    onRemoveTrait: (PartTrait) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }

    val selectedLabels = selectedTraits.map { it.label }

    val filteredSuggestions = allTraits
        .filter { it.label.lowercase().startsWith(input.lowercase().trim()) && it.label !in selectedLabels }
        .take(5)

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(filteredSuggestions.size) {
        if (filteredSuggestions.isNotEmpty()) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    androidx.compose.foundation.layout.Column(
        modifier = modifier.bringIntoViewRequester(bringIntoViewRequester)
    ) {
        if (selectedTraits.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                items(selectedTraits) { trait ->
                    InputChip(
                        selected = true,
                        onClick = { onRemoveTrait(trait) },
                        label = { Text(trait.label) },
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

        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            label = { Text("Ajouter un trait") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (input.isNotBlank()) {
                        val trimmed = input.trim()
                        val exactMatch = allTraits.firstOrNull {
                            it.label.equals(trimmed, ignoreCase = true)
                        }
                        if (exactMatch != null) onAddTrait(exactMatch) else onAddByLabel(trimmed)
                        input = ""
                    }
                }
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (focusState.isFocused) {
                        coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
                    }
                }
        )

        if (input.isNotBlank() && filteredSuggestions.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                items(filteredSuggestions) { trait ->
                    SuggestionChip(
                        onClick = {
                            onAddTrait(trait)
                            input = ""
                        },
                        label = { Text(trait.label) }
                    )
                }
            }
        }
    }
}
