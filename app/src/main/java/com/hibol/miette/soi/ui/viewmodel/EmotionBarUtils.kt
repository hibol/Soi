package com.hibol.miette.soi.ui.viewmodel

import androidx.compose.ui.graphics.Color
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.data.entity.EntryEmotion

data class EmotionBarData(
    val colorWeights: List<Pair<Color, Int>>,
    val intensityRatio: Float  // total intensité / (nb émotions × 5), dans [0, 1]
)

fun computeEmotionColors(
    entryEmotions: List<EntryEmotion>,
    allEmotions: List<Emotion>
): Map<Long, EmotionBarData> = buildMap {
    entryEmotions.groupBy { it.entryId }.forEach { (entryId, emotions) ->
        val totalActual = emotions.sumOf { it.intensity }
        val maxPossible = emotions.size * 5
        val ratio = if (maxPossible > 0) totalActual.toFloat() / maxPossible else 0f

        val primaryIntensity = mutableMapOf<Long, Int>()
        emotions.forEach { ee ->
            val emotion = allEmotions.firstOrNull { it.id == ee.emotionId } ?: return@forEach
            val primaryId = if (emotion.level == 1) emotion.id else emotion.parentId ?: return@forEach
            primaryIntensity[primaryId] = (primaryIntensity[primaryId] ?: 0) + ee.intensity
        }
        if (primaryIntensity.isEmpty()) return@forEach

        val colorWeights = primaryIntensity
            .entries
            .sortedByDescending { it.value }
            .mapNotNull { (primaryId, intensity) ->
                val hex = allEmotions.firstOrNull { it.id == primaryId }?.color ?: return@mapNotNull null
                val color = try { Color(android.graphics.Color.parseColor(hex)) }
                catch (_: IllegalArgumentException) { return@mapNotNull null }
                color to intensity
            }
        if (colorWeights.isNotEmpty()) put(entryId, EmotionBarData(colorWeights, ratio))
    }
}
