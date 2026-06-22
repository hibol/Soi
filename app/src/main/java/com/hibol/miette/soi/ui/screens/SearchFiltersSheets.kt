package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.Tag

enum class FilterSheet { TYPE, EMOTION, TAG, PART, PERIOD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TypeSheet(currentType: EntryType?, onSelect: (EntryType) -> Unit, onDismiss: () -> Unit) {
    SelectionSheet(
        title = "Type d'entrée",
        items = EntryType.entries,
        key = { it.ordinal },
        label = { it.label },
        isSelected = { it == currentType },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionSheet(
    emotions: List<Emotion>,
    currentId: Long?,
    onSelect: (Emotion) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionSheet(
        title = "Émotion",
        items = emotions,
        key = { it.id },
        label = { it.label },
        isSelected = { it.id == currentId },
        onSelect = onSelect,
        onDismiss = onDismiss,
        leadingContent = { emotion ->
            val color = remember(emotion.color) {
                try { Color(android.graphics.Color.parseColor(emotion.color)) }
                catch (e: Exception) { Color.Gray }
            }
            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagSheet(
    tags: List<Tag>,
    currentLabel: String?,
    onSelect: (Tag) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionSheet(
        title = "Tag",
        items = tags,
        key = { it.id },
        label = { "#${it.label}" },
        isSelected = { currentLabel?.equals(it.label, ignoreCase = true) == true },
        onSelect = onSelect,
        onDismiss = onDismiss,
        emptyMessage = "Aucun tag enregistré"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartSheet(
    parts: List<Part>,
    currentName: String?,
    onSelect: (Part) -> Unit,
    onDismiss: () -> Unit
) {
    SelectionSheet(
        title = "Partie",
        items = parts,
        key = { it.id },
        label = { it.name },
        isSelected = { currentName?.equals(it.name, ignoreCase = true) == true },
        onSelect = onSelect,
        onDismiss = onDismiss,
        emptyMessage = "Aucune partie enregistrée",
        supportingContent = { part ->
            part.role?.let { Text(roleLabel(it), style = MaterialTheme.typography.bodySmall) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodSheet(currentDays: Int?, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    SelectionSheet(
        title = "Période",
        items = listOf(7, 30, 90),
        key = { it },
        label = { days -> when (days) { 7 -> "7 jours"; 30 -> "30 jours"; else -> "3 mois" } },
        isSelected = { it == currentDays },
        onSelect = onSelect,
        onDismiss = onDismiss
    )
}

// ── Composable générique ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> SelectionSheet(
    title: String,
    items: List<T>,
    key: (T) -> Any,
    label: (T) -> String,
    isSelected: (T) -> Boolean,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    emptyMessage: String? = null,
    leadingContent: (@Composable (T) -> Unit)? = null,
    supportingContent: (@Composable (T) -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (items.isEmpty() && emptyMessage != null) {
            Text(
                emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyColumn {
                items(items, key = key) { item ->
                    ListItem(
                        headlineContent = { Text(label(item)) },
                        leadingContent = leadingContent?.let { { it(item) } },
                        supportingContent = supportingContent?.let { { it(item) } },
                        trailingContent = { RadioButton(selected = isSelected(item), onClick = null) },
                        modifier = Modifier.clickable { onSelect(item) }
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}
