package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Emotion
import androidx.core.graphics.toColorInt

data class EmotionSelection(
    val emotion: Emotion,
    val intensity: Int  // 1-5
)

@Composable
fun EmotionPicker(
    primaryEmotions: List<Emotion>,
    secondaryEmotions: List<Emotion>,
    selected: List<EmotionSelection>,
    onSelectionChanged: (List<EmotionSelection>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Grouper les secondaires par parentId
    val secondaryByParent = secondaryEmotions.groupBy { it.parentId }

    LazyColumn(modifier = modifier) {
        primaryEmotions.forEach { primary ->
            val children = secondaryByParent[primary.id] ?: emptyList()
            val primaryColor = Color(primary.color.toColorInt())

            item {
                Text(
                    text = primary.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryColor,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 16.dp,
                        bottom = 4.dp
                    )
                )
                HorizontalDivider(color = primaryColor.copy(alpha = 0.3f))
            }

            items(children) { emotion ->
                val currentSelection = selected.find { it.emotion.id == emotion.id }
                val isSelected = currentSelection != null

                EmotionRow(
                    emotion = emotion,
                    primaryColor = primaryColor,
                    intensity = currentSelection?.intensity ?: 0,
                    isSelected = isSelected,
                    onToggle = {
                        val newSelection = if (isSelected) {
                            selected.filter { it.emotion.id != emotion.id }
                        } else {
                            selected + EmotionSelection(emotion, 1)
                        }
                        onSelectionChanged(newSelection)
                    },
                    onIntensityChange = { intensity ->
                        val newSelection = selected.map {
                            if (it.emotion.id == emotion.id) it.copy(intensity = intensity)
                            else it
                        }
                        onSelectionChanged(newSelection)
                    }
                )
            }
        }
    }
}

@Composable
private fun EmotionRow(
    emotion: Emotion,
    primaryColor: Color,
    intensity: Int,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onIntensityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = emotion.label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        if (isSelected) {
            IntensityDots(
                intensity = intensity,
                color = primaryColor,
                onIntensityChange = onIntensityChange
            )
        }
    }
}

@Composable
private fun IntensityDots(
    intensity: Int,
    color: Color,
    onIntensityChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (i <= intensity) color else color.copy(alpha = 0.2f))
                    .clickable { onIntensityChange(i) }
            )
        }
    }
}