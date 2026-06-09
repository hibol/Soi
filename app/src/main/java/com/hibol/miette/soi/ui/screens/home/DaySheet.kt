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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.theme.ExtendedColorScheme
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.viewmodel.MiniConstellationData
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.cos
import kotlin.math.sin

private val CONSTELLATION_ZONE_WIDTH = 88.dp

@Composable
fun DaySheetEntryRow(
    entry: Entry,
    onClick: () -> Unit,
    extended: ExtendedColorScheme,
    constellation: MiniConstellationData? = null
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
            // clipToBounds clipe le dessin (drawBehind inclus) à la hauteur de la carte,
            // évitant que les grands cercles débordent sur les items voisins de la LazyColumn
            .clipToBounds()
            .drawBehind {
                constellation?.let { data ->
                    val zoneW = CONSTELLATION_ZONE_WIDTH.toPx()
                    val cx = size.width - zoneW / 2f
                    val cy = size.height / 2f
                    val ringR = 26.dp.toPx()
                    data.dots.forEach { dot ->
                        val dotR = 4.dp.toPx() + (dot.intensity / 5f) * 24.dp.toPx()
                        val px = cx + ringR * cos(dot.angle)
                        val py = cy + ringR * sin(dot.angle)
                        drawCircle(color = dot.color, radius = dotR, center = Offset(px, py))
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
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
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

        // Réserve l'espace pour que le texte ne s'étende pas sous la zone constellation
        if (constellation != null) {
            Spacer(modifier = Modifier.width(CONSTELLATION_ZONE_WIDTH))
        }
    }
}
