package com.hibol.miette.soi.ui.viewmodel

import androidx.compose.ui.graphics.Color
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.EntryEmotion

data class EmotionDot(
    val color: Color,
    val intensity: Int,
    val angle: Float     // radians, -π/2 = sommet
)

data class MiniConstellationData(
    val dots: List<EmotionDot>
)

fun computeMiniConstellations(
    entryEmotions: List<EntryEmotion>,
    allEmotions: List<Emotion>
): Map<Long, MiniConstellationData> {
    // Même ordre que le DAO : ORDER BY label ASC — cohérent avec EmotionConstellationView
    val primaries = allEmotions.filter { it.level == 1 }.sortedBy { it.label }
    val n = primaries.size
    if (n == 0) return emptyMap()

    // Angle de chaque primaire sur l'anneau (-π/2 = sommet, sens horaire)
    val angleOf = primaries.mapIndexed { i, e ->
        e.id to (-Math.PI / 2 + (i + 0.5) * 2 * Math.PI / n).toFloat()
    }.toMap()

    // Résolution émotion → primaire parente
    val primaryIdOf = buildMap<Long, Long> {
        allEmotions.forEach { e ->
            put(e.id, if (e.level == 1) e.id else e.parentId ?: return@forEach)
        }
    }

    return buildMap {
        entryEmotions.groupBy { it.entryId }.forEach { (entryId, emotions) ->
            val intensityByPrimary = mutableMapOf<Long, Int>()
            emotions.forEach { ee ->
                val primaryId = primaryIdOf[ee.emotionId] ?: return@forEach
                intensityByPrimary[primaryId] = (intensityByPrimary[primaryId] ?: 0) + ee.intensity
            }

            val dots = intensityByPrimary.mapNotNull { (primaryId, totalIntensity) ->
                val angle = angleOf[primaryId] ?: return@mapNotNull null
                val primary = primaries.firstOrNull { it.id == primaryId } ?: return@mapNotNull null
                val color = try {
                    Color(android.graphics.Color.parseColor(primary.color))
                } catch (_: Exception) { return@mapNotNull null }
                val clamped = totalIntensity.coerceIn(1, 5)
                EmotionDot(color = color, intensity = clamped, angle = angle)
            }

            if (dots.isNotEmpty()) put(entryId, MiniConstellationData(dots))
        }
    }
}
