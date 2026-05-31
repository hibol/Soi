package com.hibol.miette.soi.data.db

import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.repository.EmotionRepository
import kotlinx.coroutines.flow.first

object DatabaseInitializer {

    suspend fun populate(emotionRepository: EmotionRepository) {
        val existing = emotionRepository.getAllEmotions().first()
        if (existing.isNotEmpty()) return
        emotionRepository.insertAll(buildEmotionList())
    }

    private fun buildEmotionList(): List<Emotion> {
        data class Primary(
            val label: String,
            val color: String,
            val valence: Double?,
            val children: List<String>
        )

        val primaries = listOf(
            Primary("Joie", "#F9C74F", 1.0, listOf(
                "Jouissance", "Espoir", "Fierté", "Amusement",
                "Inspiration", "Sérénité", "Gratitude", "Admiration", "Amour"
            )),
            Primary("Surprise", "#90BE6D", null, listOf(
                "Émerveillement", "Excitation", "Stupéfaction",
                "Confusion", "Incrédulité", "Effroi"
            )),
            Primary("Peur", "#9B5DE5", -1.0, listOf(
                "Horreur", "Nervosité", "Inquiétude",
                "Terreur", "Méfiance", "Appréhension", "Anxiété"
            )),
            Primary("Tristesse", "#4895EF", -1.0, listOf(
                "Souffrance", "Mélancolie", "Désespoir", "Solitude",
                "Honte", "Culpabilité", "Dépression", "Ennui"
            )),
            Primary("Dégoût", "#43AA8B", -1.0, listOf(
                "Répulsion", "Aversion", "Mépris",
                "Pitié", "Gêne", "Remords"
            )),
            Primary("Colère", "#F94144", -1.0, listOf(
                "Agacement", "Jalousie", "Exaspération", "Rancœur",
                "Dégoût de soi", "Blessure", "Menace", "Haine", "Rage"
            ))
        )

        val result = mutableListOf<Emotion>()
        var currentId = 1L

        for (primary in primaries) {
            val primaryId = currentId++
            result.add(
                Emotion(
                    id = primaryId,
                    label = primary.label,
                    level = 1,
                    parentId = null,
                    valence = primary.valence,
                    arousal = null,
                    color = primary.color
                )
            )
            for (childLabel in primary.children) {
                result.add(
                    Emotion(
                        id = currentId++,
                        label = childLabel,
                        level = 2,
                        parentId = primaryId,
                        valence = primary.valence,
                        arousal = null,
                        color = primary.color
                    )
                )
            }
        }

        return result
    }
}