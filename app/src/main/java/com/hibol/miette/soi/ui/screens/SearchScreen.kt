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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.navigation.Routes
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme
import com.hibol.miette.soi.ui.viewmodel.SearchViewModel
import java.text.Normalizer
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
            app.container.entryRepository
        )
    )

    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    // Découpage en mots une seule fois par changement de query, pas à chaque recomposition de carte
    val queryWords = remember(query) {
        query.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    }

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
        when {
            query.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tapez un mot-clé pour chercher dans vos entrées",
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
                    Text(
                        text = "Aucun résultat pour « $query »",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(results, key = { it.id }) { entry ->
                        SearchResultCard(
                            entry = entry,
                            queryWords = queryWords,
                            onClick = { navController.navigate(Routes.entryDetail(entry.id)) }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

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

    val typeLabel = when (entry.type.value) {
        "dream"      -> "Rêve"
        "session"    -> "Session"
        "life_event" -> "Événement"
        else         -> ""
    }

    // Snippet surligné calculé une fois par (text, queryWords, highlightColor)
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
                text = typeLabel,
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

// ── Utilitaires de surbrillance ───────────────────────────────────────────────

/**
 * Extrait un snippet autour du premier match et y applique la surbrillance.
 *
 * Normalisation NFD : "rêve" et "reve" sont équivalents côté SQLite (unicode61),
 * on fait pareil ici pour que la surbrillance corresponde au résultat FTS4.
 * La longueur en chars est préservée après normalisation pour les caractères
 * latins, donc les positions dans le texte normalisé mappent directement sur
 * les positions dans le texte original (avec accents).
 */
private fun buildHighlightedSnippet(
    text: String,
    words: List<String>,
    highlightColor: Color
): AnnotatedString {
    val normalizedText = normalizeForSearch(text)
    val normalizedWords = words.map { normalizeForSearch(it) }.filter { it.isNotBlank() }

    // Positions de tous les matchs dans le texte normalisé
    val ranges = mutableListOf<IntRange>()
    for (word in normalizedWords) {
        var start = 0
        while (start <= normalizedText.length - word.length) {
            val idx = normalizedText.indexOf(word, start)
            if (idx < 0) break
            ranges.add(idx..idx + word.length - 1)
            start = idx + 1
        }
    }

    // Fenêtre de 150 chars centrée sur le premier match
    val firstMatchStart = ranges.minOfOrNull { it.first } ?: 0
    val windowStart = (firstMatchStart - 40).coerceAtLeast(0)
    val windowEnd = (windowStart + 150).coerceAtMost(text.length)
    val snippet = text.substring(windowStart, windowEnd)

    // Recalibrer les ranges sur les coordonnées du snippet et fusionner les chevauchements
    val snippetRanges = mergeRanges(
        ranges
            .map { (it.first - windowStart)..(it.last - windowStart) }
            .filter { it.first >= 0 && it.last < snippet.length }
            .sortedBy { it.first }
    )

    return buildAnnotatedString {
        if (windowStart > 0) append("…")

        var cursor = 0
        for (r in snippetRanges) {
            if (r.first > cursor) append(snippet.substring(cursor, r.first))
            withStyle(SpanStyle(color = highlightColor, fontWeight = FontWeight.SemiBold)) {
                append(snippet.substring(r.first, r.last + 1))
            }
            cursor = r.last + 1
        }
        if (cursor < snippet.length) append(snippet.substring(cursor))
        if (windowEnd < text.length) append("…")
    }
}

private fun normalizeForSearch(s: String): String =
    Normalizer.normalize(s, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase(Locale.FRENCH)

private fun mergeRanges(sorted: List<IntRange>): List<IntRange> {
    if (sorted.isEmpty()) return emptyList()
    val merged = mutableListOf(sorted[0])
    for (r in sorted.drop(1)) {
        val last = merged.last()
        if (r.first <= last.last + 1) {
            merged[merged.lastIndex] = last.first..maxOf(last.last, r.last)
        } else {
            merged.add(r)
        }
    }
    return merged
}
