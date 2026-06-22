package com.hibol.miette.soi.ui.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import java.text.Normalizer
import java.util.Locale

// Normalisation NFD : "rêve" et "reve" sont équivalents côté SQLite (unicode61).
// NFD préserve les positions dans le texte original → la surbrillance coïncide avec le FTS.
fun buildHighlightedSnippet(
    text: String,
    words: List<String>,
    highlightColor: Color
): AnnotatedString {
    val normalizedText = normalizeForSearch(text)
    val normalizedWords = words.map { normalizeForSearch(it) }.filter { it.isNotBlank() }

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

    val firstMatchStart = ranges.minOfOrNull { it.first } ?: 0
    val windowStart = (firstMatchStart - 40).coerceAtLeast(0)
    val windowEnd = (windowStart + 150).coerceAtMost(text.length)
    val snippet = text.substring(windowStart, windowEnd)

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

fun normalizeForSearch(s: String): String =
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
