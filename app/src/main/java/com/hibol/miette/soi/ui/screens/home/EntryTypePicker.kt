package com.hibol.miette.soi.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.ModeNight
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hibol.miette.soi.data.entity.EntryType
import com.hibol.miette.soi.ui.theme.ColorFamily
import com.hibol.miette.soi.ui.theme.ExtendedColorScheme
import com.hibol.miette.soi.ui.theme.colorFamilyForType
import com.hibol.miette.soi.ui.theme.extendedColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntryTypePicker(
    onTypeSelected: (EntryType) -> Unit,
    onDismiss: () -> Unit
) {
    val extended = extendedColorScheme()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Nouvelle entrée",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            EntryTypeRow(
                label = "Rêve",
                entryType = EntryType.DREAM,
                colorFamily = extended.colorFamilyForType(EntryType.DREAM),
                icon = { Icon(Icons.Default.ModeNight, contentDescription = null) },
                onClick = { onTypeSelected(EntryType.DREAM) }
            )

            EntryTypeRow(
                label = "Session",
                entryType = EntryType.SESSION,
                colorFamily = extended.colorFamilyForType(EntryType.SESSION),
                icon = { Icon(Icons.Default.SelfImprovement, contentDescription = null) },
                onClick = { onTypeSelected(EntryType.SESSION) }
            )

            EntryTypeRow(
                label = "Événement",
                entryType = EntryType.LIFE_EVENT,
                colorFamily = extended.colorFamilyForType(EntryType.LIFE_EVENT),
                icon = { Icon(Icons.Default.Event, contentDescription = null) },
                onClick = { onTypeSelected(EntryType.LIFE_EVENT) }
            )
        }
    }
}

@Composable
private fun EntryTypeRow(
    label: String,
    entryType: EntryType,
    colorFamily: ColorFamily,
    icon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = colorFamily.colorContainer,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                CompositionLocalProvider(
                    LocalContentColor provides colorFamily.onColorContainer
                ) {
                    icon()
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}