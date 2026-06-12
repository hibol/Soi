package com.hibol.miette.soi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.hibol.miette.soi.SoiApplication
import com.hibol.miette.soi.data.entity.Emotion
import com.hibol.miette.soi.ui.viewmodel.SettingsViewModel
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as SoiApplication
    val viewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(app.container.emotionRepository)
    )

    val primaryEmotions by viewModel.primaryEmotions.collectAsState()
    var editingEmotion by remember { mutableStateOf<Emotion?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                Column {
                    Text(
                        "Couleurs des émotions",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                    HorizontalDivider()
                }
            }
            items(primaryEmotions, key = { it.id }) { emotion ->
                EmotionColorRow(
                    emotion = emotion,
                    onClick = { editingEmotion = emotion }
                )
            }
        }
    }

    editingEmotion?.let { emotion ->
        ModalBottomSheet(
            onDismissRequest = { editingEmotion = null },
            sheetState = sheetState
        ) {
            ColorPickerSheet(
                emotion = emotion,
                onConfirm = { newHex ->
                    viewModel.updateColor(emotion, newHex)
                    scope.launch { sheetState.hide() }.invokeOnCompletion { editingEmotion = null }
                },
                onDismiss = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion { editingEmotion = null }
                }
            )
        }
    }
}

@Composable
private fun EmotionColorRow(emotion: Emotion, onClick: () -> Unit) {
    val color = remember(emotion.color) {
        runCatching { Color(android.graphics.Color.parseColor(emotion.color)) }
            .getOrDefault(Color.Gray)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(emotion.label, style = MaterialTheme.typography.bodyMedium)
            Text(
                emotion.color,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ColorPickerSheet(
    emotion: Emotion,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    // Parse hex → HSV
    val initialHsv = remember(emotion.color) {
        val parsed = runCatching { android.graphics.Color.parseColor(emotion.color) }
            .getOrDefault(android.graphics.Color.GRAY)
        FloatArray(3).also { android.graphics.Color.colorToHSV(parsed, it) }
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }          // 0..360
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }   // 0..1
    var value by remember { mutableFloatStateOf(initialHsv[2]) }        // 0..1

    val currentColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    val currentHex = "#%06X".format(currentColor.toArgb() and 0xFFFFFF)

    // Champ hex : source de vérité secondaire — mis à jour par les sliders,
    // et quand l'utilisateur tape un hex valide il met les sliders à jour.
    var hexInput by remember { mutableStateOf(emotion.color) }
    // Synchroniser hexInput dès que les sliders bougent (sauf si l'utilisateur est en train de taper)
    var hexFocused by remember { mutableStateOf(false) }
    if (!hexFocused) hexInput = currentHex

    fun applyHex(input: String) {
        hexInput = input
        val clean = input.trim().uppercase()
        if (clean.matches(Regex("^#[0-9A-F]{6}$"))) {
            val parsed = runCatching { android.graphics.Color.parseColor(clean) }.getOrNull()
            if (parsed != null) {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(parsed, hsv)
                hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(emotion.label, style = MaterialTheme.typography.titleMedium)

        // Aperçu + hex éditable
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(currentColor)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            OutlinedTextField(
                value = hexInput,
                onValueChange = { applyHex(it) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                    .width(130.dp)
                    .onFocusChanged { hexFocused = it.isFocused },
                isError = !hexInput.trim().uppercase().matches(Regex("^#[0-9A-F]{6}$"))
            )
        }

        // Teinte
        Text("Teinte", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    Brush.horizontalGradient(
                        colors = (0..12).map { i ->
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(i * 30f, 1f, 1f)))
                        }
                    )
                )
        )
        Slider(
            value = hue,
            onValueChange = { hue = it },
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )

        // Saturation
        Text("Saturation", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value))),
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))
                        )
                    )
                )
        )
        Slider(
            value = saturation,
            onValueChange = { saturation = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )

        // Luminosité
        Text("Luminosité", style = MaterialTheme.typography.labelMedium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(MaterialTheme.shapes.small)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black,
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                        )
                    )
                )
        )
        Slider(
            value = value,
            onValueChange = { value = it },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = currentColor,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent
            )
        )

        // Boutons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) { Text("Annuler") }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { onConfirm(currentHex) }) { Text("Appliquer") }
        }
    }
}
