package com.hibol.miette.soi.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.hibol.miette.soi.data.entity.Emotion
import kotlinx.coroutines.launch
import kotlin.math.*

data class EmotionSelection(
    val emotion: Emotion,
    val intensity: Int
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmotionPicker(
    primaryEmotions: List<Emotion>,
    secondaryEmotions: List<Emotion>,
    selected: List<EmotionSelection>,
    onSelectionChanged: (List<EmotionSelection>) -> Unit,
    modifier: Modifier = Modifier
) {
    if (primaryEmotions.isEmpty()) return

    val secondaryByParent = remember(secondaryEmotions) {
        secondaryEmotions.groupBy { it.parentId }
    }

    var activeIndex by remember { mutableIntStateOf(0) }
    var joieInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(primaryEmotions) {
        if (primaryEmotions.isNotEmpty() && !joieInitialized) {
            joieInitialized = true
            val idx = primaryEmotions.indexOfFirst { it.label.equals("Joie", ignoreCase = true) }
            if (idx >= 0) activeIndex = idx
        }
    }
    val safeIndex = activeIndex.coerceIn(0, primaryEmotions.lastIndex)
    val activePrimary = primaryEmotions[safeIndex]
    val activeChildren = secondaryByParent[activePrimary.id] ?: emptyList()
    val activeColor = Color(activePrimary.color.toColorInt())

    val density = LocalDensity.current
    val expansionPx = with(density) { 10.dp.toPx() }

    // Un Animatable par segment pour l'expansion du rayon extérieur
    val expandAnims = remember(primaryEmotions.size) {
        List(primaryEmotions.size) { Animatable(0f) }
    }

    LaunchedEffect(safeIndex) {
        expandAnims.forEachIndexed { i, anim ->
            launch {
                anim.animateTo(
                    targetValue = if (i == safeIndex) expansionPx else 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    // Lecture de .value ici : la composition observe les Animatable → redessine le Canvas à chaque frame
    val expansions = expandAnims.map { it.value }
    val textMeasurer = rememberTextMeasurer()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            val sizeDp = maxWidth
            // 14dp de marge pour que le segment actif ne soit pas rogné par le Canvas
            val baseOuterRDp = sizeDp / 2 - 14.dp
            val ringThicknessDp = 46.dp
            val innerRDp = baseOuterRDp - ringThicknessDp

            val sizePx = with(density) { sizeDp.toPx() }
            val baseOuterRPx = with(density) { baseOuterRDp.toPx() }
            val innerRPx = with(density) { innerRDp.toPx() }

            Box(
                modifier = Modifier.size(sizeDp),
                contentAlignment = Alignment.Center
            ) {
                // ── Anneau ──────────────────────────────────────────────────────
                Canvas(
                    modifier = Modifier
                        .size(sizeDp)
                        .pointerInput(primaryEmotions.size) {
                            detectTapGestures { offset ->
                                val dx = offset.x - sizePx / 2
                                val dy = offset.y - sizePx / 2
                                val distance = sqrt(dx * dx + dy * dy)
                                if (distance >= innerRPx && distance <= baseOuterRPx + expansionPx) {
                                    var angle = atan2(dy, dx) * 180f / PI.toFloat()
                                    if (angle < 0f) angle += 360f
                                    val normalized = (angle + 90f + 360f) % 360f
                                    val n = primaryEmotions.size
                                    activeIndex = (normalized / (360f / n)).toInt().coerceIn(0, n - 1)
                                }
                            }
                        }
                ) {
                    val cx = sizePx / 2
                    val cy = sizePx / 2
                    val n = primaryEmotions.size
                    val sweepAngle = 360f / n

                    primaryEmotions.forEachIndexed { i, emotion ->
                        val startAngle = i * sweepAngle - 90f
                        val color = Color(emotion.color.toColorInt())
                        val outerR = baseOuterRPx + expansions[i]

                        // Segment : arc extérieur → trait → arc intérieur → fermeture
                        val outerRect = Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR)
                        val innerRect = Rect(cx - innerRPx, cy - innerRPx, cx + innerRPx, cy + innerRPx)
                        val path = Path().apply {
                            arcTo(outerRect, startAngle, sweepAngle, true)
                            arcTo(innerRect, startAngle + sweepAngle, -sweepAngle, false)
                            close()
                        }
                        drawPath(path, color)

                        // Label centré dans l'épaisseur de l'anneau
                        val midAngle = startAngle + sweepAngle / 2f
                        val midRad = midAngle * PI.toFloat() / 180f
                        val labelR = (outerR + innerRPx) / 2f
                        val lx = cx + labelR * cos(midRad)
                        val ly = cy + labelR * sin(midRad)
                        val labelStyle = TextStyle(
                            fontSize = if (i == safeIndex) 10.sp else 9.sp,
                            fontWeight = if (i == safeIndex) FontWeight.Bold else FontWeight.Medium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        val measured = textMeasurer.measure(emotion.label, labelStyle)
                        drawText(
                            textLayoutResult = measured,
                            topLeft = Offset(
                                lx - measured.size.width / 2f,
                                ly - measured.size.height / 2f
                            )
                        )

                        // Pastille blanche sur le bord intérieur si des secondaires sont sélectionnées
                        val hasSelections = selected.any { sel ->
                            secondaryByParent[emotion.id]?.any { sec -> sec.id == sel.emotion.id } == true
                        }
                        if (hasSelections) {
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = Offset(
                                    cx + (innerRPx - 8f) * cos(midRad),
                                    cy + (innerRPx - 8f) * sin(midRad)
                                ),
                                alpha = 0.85f
                            )
                        }
                    }
                }

                // ── Trou central : liste des émotions secondaires ────────────────
                Box(
                    modifier = Modifier
                        .size(innerRDp * 2)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = activePrimary.label.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = activeColor,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            color = activeColor.copy(alpha = 0.2f)
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            items(activeChildren, key = { it.id }) { emotion ->
                                val currentSel = selected.find { it.emotion.id == emotion.id }
                                val isSelected = currentSel != null
                                SecondaryEmotionRow(
                                    emotion = emotion,
                                    primaryColor = activeColor,
                                    intensity = currentSel?.intensity ?: 0,
                                    isSelected = isSelected,
                                    onToggle = {
                                        val newSel = if (isSelected)
                                            selected.filter { it.emotion.id != emotion.id }
                                        else
                                            selected + EmotionSelection(emotion, 1)
                                        onSelectionChanged(newSel)
                                    },
                                    onIntensityChange = { newIntensity ->
                                        onSelectionChanged(selected.map {
                                            if (it.emotion.id == emotion.id) it.copy(intensity = newIntensity)
                                            else it
                                        })
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Chips récap des émotions sélectionnées ──────────────────────────
        if (selected.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                selected.forEach { sel ->
                    val chipColor = Color(sel.emotion.color.toColorInt())
                    SuggestionChip(
                        onClick = {
                            onSelectionChanged(selected.filter { it.emotion.id != sel.emotion.id })
                        },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(sel.emotion.label, style = MaterialTheme.typography.labelSmall)
                                IntensityDots(
                                    intensity = sel.intensity,
                                    color = chipColor,
                                    dotSize = 5.dp,
                                    onIntensityChange = { newIntensity ->
                                        onSelectionChanged(selected.map {
                                            if (it.emotion.id == sel.emotion.id) it.copy(intensity = newIntensity)
                                            else it
                                        })
                                    }
                                )
                            }
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = chipColor.copy(alpha = 0.12f)
                        ),
                        border = BorderStroke(1.dp, chipColor.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryEmotionRow(
    emotion: Emotion,
    primaryColor: Color,
    intensity: Int,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onIntensityChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = emotion.label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        )
        if (isSelected) {
            Spacer(Modifier.height(3.dp))
            IntensityDots(
                intensity = intensity,
                color = primaryColor,
                dotSize = 9.dp,
                onIntensityChange = onIntensityChange
            )
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 5.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
        )
    }
}

@Composable
private fun IntensityDots(
    intensity: Int,
    color: Color,
    dotSize: Dp = 10.dp,
    onIntensityChange: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..5) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(if (i <= intensity) color else color.copy(alpha = 0.2f))
                    .clickable { onIntensityChange(i) }
            )
        }
    }
}
