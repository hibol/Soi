package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.ui.theme.ExtendedColorScheme
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun DaySheetEntryRow(
    entry: Entry,
    onClick: () -> Unit,
    extended: ExtendedColorScheme
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
            .pointerInput(entry.id, entry.isBlurred) {
                detectTapGestures(
                    onPress = { _ ->
                        val pressStart = System.currentTimeMillis()
                        if (entry.isBlurred != 0) isRevealed = true

                        val released = tryAwaitRelease()
                        if (entry.isBlurred != 0) isRevealed = false

                        // Navigation uniquement si tap court (pas un hold)
                        if (released && System.currentTimeMillis() - pressStart < viewConfiguration.longPressTimeoutMillis) {
                            onClick()
                        }
                    }
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(colorFamily.color)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = typeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = colorFamily.color
            )
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

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = time,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
