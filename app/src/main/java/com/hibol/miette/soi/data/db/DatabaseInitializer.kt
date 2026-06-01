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
                "Inspiration", "Sérénité", "Gratitude", "Admiration", "Amour",
                "Émerveillement", "Excitation", "Délice", "Satisfaction",
                "Encouragement", "Contentement", "Tendresse", "Foi", "Présence"
            )),
            Primary("Surprise", "#90BE6D", null, listOf(
                "Stupéfaction", "Confusion", "Incrédulité", "Choc"
            )),
            Primary("Peur", "#9B5DE5", -1.0, listOf(
                "Horreur", "Nervosité", "Inquiétude", "Terreur",
                "Méfiance", "Appréhension", "Anxiété", "Effroi",
                "Gêne", "Insécurité"
            )),
            Primary("Tristesse", "#4895EF", -1.0, listOf(
                "Souffrance", "Mélancolie", "Désespoir", "Solitude",
                "Honte", "Culpabilité", "Dépression", "Ennui",
                "Remords", "Apathie", "Infériorité", "Inadéquation", "Pitié"
            )),
            Primary("Colère", "#F94144", -1.0, listOf(
                "Agacement", "Jalousie", "Exaspération", "Rancœur",
                "Dégoût de soi", "Blessure", "Menace", "Haine", "Rage",
                "Répulsion", "Mépris", "Aversion", "Frustration",
                "Hostilité", "Irritation", "Condescendance"
            )),
            Primary("Puissance", "#FF9F1C", 1.0, listOf(
                "Confiance", "Créativité", "Courage", "Valorisation", "Reconnaissance"
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