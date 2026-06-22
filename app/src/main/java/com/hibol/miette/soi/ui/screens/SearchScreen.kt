package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.util.buildHighlightedSnippet
import com.hibol.miette.soi.ui.viewmodel.SearchFilters
import com.hibol.miette.soi.ui.viewmodel.SearchViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: SearchViewModel = viewModel(
        factory = SearchViewModel.Factory(
            app.container.profileRepository,
            app.container.entryRepository,
            app.container.emotionRepository,
            app.container.tagRepository,
            app.container.partRepository
        )
    )

    val query by viewModel.query.collectAsState()
    val filters by viewModel.filters.collectAsState()
    val results by viewModel.results.collectAsState()
    val primaryEmotions by viewModel.primaryEmotions.collectAsState()
    val allTags by viewModel.allTags.collectAsState()
    val allParts by viewModel.allParts.collectAsState()

    val queryWords = remember(query) {
        query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }
    val isSearchActive = query.isNotBlank() || !filters.isEmpty
    var openSheet by remember { mutableStateOf<FilterSheet?>(null) }

    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = query,
                onQueryChange = viewModel::onQueryChange,
                onSearch = {},
                expanded = true,
                onExpandedChange = { if (!it) navController.popBackStack() },
                placeholder = { Text("Rechercher dans le journal…") },
                leadingIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Effacer")
                        }
                    }
                }
            )
        },
        expanded = true,
        onExpandedChange = { if (!it) navController.popBackStack() },
        modifier = Modifier.fillMaxWidth()
    ) {
        val emotionLabel = remember(filters.primaryEmotionId, primaryEmotions) {
            primaryEmotions.find { it.id == filters.primaryEmotionId }?.label
        }
        FilterChipsRow(
            filters = filters,
            emotionLabel = emotionLabel,
            onChipClick = { sheet ->
                // Chip actif → efface le filtre. Chip inactif → ouvre le sheet.
                when {
                    sheet == FilterSheet.TYPE && filters.type != null ->
                        viewModel.setFilter(filters.copy(type = null))
                    sheet == FilterSheet.EMOTION && filters.primaryEmotionId != null ->
                        viewModel.setFilter(filters.copy(primaryEmotionId = null))
                    sheet == FilterSheet.TAG && filters.tagLabel != null ->
                        viewModel.setFilter(filters.copy(tagLabel = null))
                    sheet == FilterSheet.PART && filters.partName != null ->
                        viewModel.setFilter(filters.copy(partName = null))
                    sheet == FilterSheet.PERIOD && filters.periodDays != null ->
                        viewModel.setFilter(filters.copy(periodDays = null))
                    else -> openSheet = sheet
                }
            }
        )

        when {
            !isSearchActive -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tapez un mot-clé ou choisissez un filtre",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val msg = when {
                        query.isNotBlank() && !filters.isEmpty ->
                            "Aucun résultat pour « $query » avec ces filtres"
                        query.isNotBlank() -> "Aucun résultat pour « $query »"
                        else -> "Aucun résultat avec ces filtres"
                    }
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(results, key = { it.id }) { entry ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            SearchResultCard(
                                entry = entry,
                                queryWords = queryWords,
                                onClick = { navController.navigate(Routes.entryDetail(entry.id)) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── ModalBottomSheets ─────────────────────────────────────────────────────

    if (openSheet == FilterSheet.TYPE) {
        TypeSheet(
            currentType = filters.type,
            onSelect = { type ->
                viewModel.setFilter(filters.copy(type = type))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }

    if (openSheet == FilterSheet.EMOTION) {
        EmotionSheet(
            emotions = primaryEmotions,
            currentId = filters.primaryEmotionId,
            onSelect = { emotion ->
                viewModel.setFilter(filters.copy(primaryEmotionId = emotion.id))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }

    if (openSheet == FilterSheet.TAG) {
        TagSheet(
            tags = allTags,
            currentLabel = filters.tagLabel,
            onSelect = { tag ->
                viewModel.setFilter(filters.copy(tagLabel = tag.label))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }

    if (openSheet == FilterSheet.PART) {
        PartSheet(
            parts = allParts,
            currentName = filters.partName,
            onSelect = { part ->
                viewModel.setFilter(filters.copy(partName = part.name))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }

    if (openSheet == FilterSheet.PERIOD) {
        PeriodSheet(
            currentDays = filters.periodDays,
            onSelect = { days ->
                viewModel.setFilter(filters.copy(periodDays = days))
                openSheet = null
            },
            onDismiss = { openSheet = null }
        )
    }
}

// ── FilterChipsRow ────────────────────────────────────────────────────────────

@Composable
private fun FilterChipsRow(
    filters: SearchFilters,
    emotionLabel: String?,
    onChipClick: (FilterSheet) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = filters.type != null,
                onClick = { onChipClick(FilterSheet.TYPE) },
                label = { Text(filters.type?.label ?: "Type") },
                trailingIcon = if (filters.type != null) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        item {
            FilterChip(
                selected = filters.primaryEmotionId != null,
                onClick = { onChipClick(FilterSheet.EMOTION) },
                label = { Text(emotionLabel ?: "Émotion") },
                trailingIcon = if (filters.primaryEmotionId != null) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        item {
            FilterChip(
                selected = filters.tagLabel != null,
                onClick = { onChipClick(FilterSheet.TAG) },
                label = { Text(if (filters.tagLabel != null) "#${filters.tagLabel}" else "Tag") },
                trailingIcon = if (filters.tagLabel != null) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        item {
            FilterChip(
                selected = filters.partName != null,
                onClick = { onChipClick(FilterSheet.PART) },
                label = { Text(filters.partName ?: "Partie") },
                trailingIcon = if (filters.partName != null) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        item {
            FilterChip(
                selected = filters.periodDays != null,
                onClick = { onChipClick(FilterSheet.PERIOD) },
                label = {
                    Text(when (filters.periodDays) {
                        7    -> "7 jours"
                        30   -> "30 jours"
                        90   -> "3 mois"
                        else -> "Période"
                    })
                },
                trailingIcon = if (filters.periodDays != null) {
                    { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}

// ── SearchResultCard ──────────────────────────────────────────────────────────

@Composable
private fun SearchResultCard(
    entry: Entry,
    queryWords: List<String>,
    onClick: () -> Unit
) {
    val extended = extendedColorScheme()
    val colorFamily = extended.colorFamilyForType(entry.type)
    var isRevealed by remember(entry.id) { mutableStateOf(false) }
    val highlightColor = MaterialTheme.colorScheme.primary

    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH) }
    val date = remember(entry.entryDate) {
        Instant.ofEpochMilli(entry.entryDate)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(dateFormatter)
    }

    val highlightedText: AnnotatedString? = remember(entry.text, queryWords, highlightColor) {
        entry.text?.takeIf { it.isNotBlank() }
            ?.let { buildHighlightedSnippet(it, queryWords, highlightColor) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(entry.id, entry.isBlurred) {
                detectTapGestures(
                    onPress = { _ ->
                        val pressStart = System.currentTimeMillis()
                        if (entry.isBlurred != 0) isRevealed = true

                        val released = tryAwaitRelease()
                        if (entry.isBlurred != 0) isRevealed = false

                        if (released && System.currentTimeMillis() - pressStart < viewConfiguration.longPressTimeoutMillis) {
                            onClick()
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.type.label,
                style = MaterialTheme.typography.labelSmall,
                color = colorFamily.color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (highlightedText != null) {
            Text(
                text = highlightedText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = if (entry.isBlurred != 0 && !isRevealed) Modifier.blur(8.dp) else Modifier
            )
        } else {
            Text(
                text = "Aucun texte",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
