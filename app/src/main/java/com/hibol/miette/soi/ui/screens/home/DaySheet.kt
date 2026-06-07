package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.theme.ExtendedColorScheme
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.viewmodel.EmotionBarData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DaySheetEntryRow(
    entry: Entry,
    onClick: () -> Unit,
    extended: ExtendedColorScheme,
    emotionColors: EmotionBarData? = null
) {
    var isRevealed by remember { mutableStateOf(false) }

    val colorFamily = extended.colorFamilyForType(entry.type)
    val time = Instant.ofEpochMilli(entry.entryDate)
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("HH:mm"))

    val typeLabel = when (entry.type.value) {
        "dream" -> "Rêve"
        "session" -> "Session"
        "life_event" -> "Événement"
        else -> ""
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                // Dessiné dans l'espace complet du Row (padding inclus),
                // donc le fondu passe sous le texte et déborde sur les marges.
                emotionColors?.let { data ->
                    val barWidthPx = (.5f + data.intensityRatio * 60f).dp.toPx()
                    val flatPx = 6.dp.toPx()
                    // fraction de la largeur à laquelle le fondu s'arrête et le plateau commence
                    val gradientFraction = ((barWidthPx - flatPx) / barWidthPx).coerceIn(0f, 1f)
                    val total = data.colorWeights.sumOf { it.second }.toFloat()
                    var segmentTop = 0f
                    data.colorWeights.forEach { (color, intensity) ->
                        val segmentHeight = size.height * (intensity / total)
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    gradientFraction to color,
                                    1f to color
                                ),
                                startX = size.width - barWidthPx,
                                endX = size.width
                            ),
                            topLeft = Offset(0f, segmentTop),
                            size = Size(size.width, segmentHeight)
                        )
                        segmentTop += segmentHeight
                    }
                }
            }
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
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorFamily.color
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!entry.text.isNullOrBlank()) {
                Text(
                    text = entry.text,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
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
}
