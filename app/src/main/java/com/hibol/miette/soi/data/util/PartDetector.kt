package com.hibol.miette.soi.data.util

import com.hibol.miette.soi.data.entity.Part

object PartDetector {

    private const val MIN_NAME_LENGTH = 4

    fun detect(text: String, parts: List<Part>): List<Part> {
        if (text.isBlank()) return emptyList()
        val normText = normalize(text)
        return parts.filter { part ->
            part.name.length >= MIN_NAME_LENGTH && containsWord(normText, normalize(part.name))
        }
    }

    private fun containsWord(text: String, word: String): Boolean {
        val escaped = Regex.escape(word)
        // \p{L} et \p{N} couvrent lettres/chiffres Unicode (accents inclus)
        // — évite les faux positifs de type "Anne" dans "Jeanne"
        val pattern = Regex("(?i)(?<![\\p{L}\\p{N}])$escaped(?![\\p{L}\\p{N}])")
        return pattern.containsMatchIn(text)
    }

    private fun normalize(s: String) = s
        .replace('’', '\'')  // ' (apostrophe courbe droite) → '
        .replace('‘', '\'')  // ' (apostrophe courbe gauche) → '
        .replace('′', '\'')  // ′ (prime) → '
}
