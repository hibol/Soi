package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.abs

internal fun orientationLabel(value: Float): String = when {
    value < -0.6f -> "Très confus·e"
    value < -0.2f -> "Confus·e"
    value < 0.2f  -> "Neutre"
    value < 0.6f  -> "Assez clair·e"
    else          -> "Très clair·e"
}

// Alpha en V : transparent au centre (0), plein aux extrémités (±1)
private fun orientationAlpha(value: Float): Float =
    (0.12f + 0.88f * abs(value)).coerceIn(0f, 1f)

@Composable
private fun orientationColor(value: Float): Color =
    MaterialTheme.colorScheme.onSurface.copy(alpha = orientationAlpha(value))

// ─── Saisie ───────────────────────────────────────────────────────────────────

@Composable
fun OrientationSlider(
    value: Float?,
    onValueChange: (Float?) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderPosition = value ?: 0f
    val isSet = value != null
    val baseColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Orientation cognitive", style = MaterialTheme.typography.titleSmall)
            if (isSet) {
                TextButton(onClick = { onValueChange(null) }) {
                    Text(
                        "Effacer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            // Piste en V : plein aux deux bouts, transparent au centre
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                baseColor,
                                baseColor.copy(alpha = 0.06f),
                                baseColor
                            )
                        ),
                        alpha = if (isSet) 1f else 0.3f
                    )
            )
            Slider(
                value = sliderPosition,
                onValueChange = { onValueChange(it) },
                valueRange = -1f..1f,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = if (isSet) orientationColor(sliderPosition)
                                 else baseColor.copy(alpha = 0.3f),
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                )
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Confusion",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSet) {
                Text(
                    orientationLabel(sliderPosition),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = orientationColor(sliderPosition)
                )
            }
            Text(
                "Clarté",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Lecture ─────────────────────────────────────────────────────────────────

@Composable
fun OrientationBar(
    orientation: Float,
    modifier: Modifier = Modifier
) {
    val fraction = (orientation + 1f) / 2f
    val baseColor = MaterialTheme.colorScheme.onSurface

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Orientation cognitive", style = MaterialTheme.typography.titleSmall)
            Text(
                orientationLabel(orientation),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = orientationColor(orientation)
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                baseColor,
                                baseColor.copy(alpha = 0.06f),
                                baseColor
                            )
                        )
                    )
            )
            val offsetX = (maxWidth * fraction - 1.dp).coerceIn(0.dp, maxWidth - 2.dp)
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .offset(x = offsetX)
                    .background(orientationColor(orientation))
            )
        }
    }
}
