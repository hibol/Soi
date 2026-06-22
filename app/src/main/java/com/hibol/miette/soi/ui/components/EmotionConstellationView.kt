package com.hibol.miette.soi.ui.components

import android.graphics.BlurMaskFilter
import android.graphics.Paint as NativePaint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.hibol.miette.soi.data.entity.Emotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

// ── Seuils LOD ───────────────────────────────────────────────────────────────

private const val LOD_SECONDARY = 1.5f   // labels secondaires intensité ≥ 3
private const val LOD_ALL       = 2.5f   // tous les labels secondaires

// ── Structures internes ──────────────────────────────────────────────────────

private data class PrimaryNode(
    val emotion: Emotion,
    val x: Float,
    val y: Float,
    val hasSelections: Boolean,
    val hsl: Triple<Float, Float, Float>
)

private data class SecondaryNode(
    val emotion: Emotion,
    val x: Float,
    val y: Float,
    val r: Float,
    val intensity: Int,
    val parentX: Float,
    val parentY: Float,
    val hsl: Triple<Float, Float, Float>
)

private data class ConstellationLayout(
    val primaryNodes: List<PrimaryNode>,
    val secondaryNodes: List<SecondaryNode>,
    val primaryR: Float
)

private data class PlacedItem(val x: Float, val y: Float, val r: Float, val labelH: Float)

// ── Algorithme de placement ──────────────────────────────────────────────────

private fun buildLayout(
    primaryEmotions: List<Emotion>,
    selections: List<Pair<Emotion, Int>>,
    width: Float,
    height: Float,
    rng: Random
): ConstellationLayout {
    val cx = width / 2f
    val cy = height / 2f
    val scale = width / 340f
    val ringR = 68f * scale
    val primaryR = 19f * scale
    val margin = 22f * scale
    val secLabelH = 12f * scale
    val n = primaryEmotions.size

    val selByParentId = selections
        .filter { (e, _) -> e.level == 2 }
        .groupBy { (e, _) -> e.parentId }

    val placed = mutableListOf<PlacedItem>()
    val primaryNodes = mutableListOf<PrimaryNode>()
    val secondaryNodes = mutableListOf<SecondaryNode>()

    primaryEmotions.forEachIndexed { i, primary ->
        val angle = (-PI / 2.0 + (i.toDouble() + 0.5) / n * 2.0 * PI).toFloat()
        val px = cx + ringR * cos(angle)
        val py = cy + ringR * sin(angle)
        val hsl = hexToHsl(primary.color)
        val hasSelections = selByParentId[primary.id]?.isNotEmpty() == true

        primaryNodes.add(PrimaryNode(primary, px, py, hasSelections, hsl))
        placed.add(PlacedItem(px, py, primaryR, 0f))

        selByParentId[primary.id]?.forEach { (emotion, intensity) ->
            val secR = (5f + intensity * 1.6f) * scale
            var bx: Float
            var by: Float
            var tries = 0
            do {
                val spread = rng.nextFloat() * 3.2f - 1.6f
                val secAngle = angle + spread
                val dist = primaryR + secR + 16f * scale + rng.nextFloat() * (38f + intensity * 5f) * scale
                bx = (px + dist * cos(secAngle)).coerceIn(margin + secR, width - margin - secR)
                by = (py + dist * sin(secAngle)).coerceIn(margin + secR, height - margin - secR - secLabelH)
                tries++
            } while (overlaps(bx, by, secR, secLabelH, placed) && tries < 100)

            secondaryNodes.add(SecondaryNode(emotion, bx, by, secR, intensity, px, py, hsl))
            placed.add(PlacedItem(bx, by, secR, secLabelH))
        }
    }

    return ConstellationLayout(primaryNodes, secondaryNodes, primaryR)
}

private fun overlaps(x: Float, y: Float, r: Float, labelH: Float, existing: List<PlacedItem>): Boolean {
    for (item in existing) {
        val dx = item.x - x
        val dy = item.y - y
        val minDist = item.r + r + item.labelH + labelH + 6f
        if (dx * dx + dy * dy < minDist * minDist) return true
    }
    return false
}

// ── Composable ───────────────────────────────────────────────────────────────

@Composable
fun EmotionConstellationView(
    primaryEmotions: List<Emotion>,
    selections: List<Pair<Emotion, Int>>,
    modifier: Modifier = Modifier
) {
    if (primaryEmotions.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val seed = remember(selections) {
        selections.fold(0L) { acc, (e, i) -> acc * 31L + e.id * 7L + i.toLong() }
    }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val layout = remember(primaryEmotions, selections, seed, canvasSize) {
        if (canvasSize == IntSize.Zero) null
        else buildLayout(
            primaryEmotions, selections,
            canvasSize.width.toFloat(), canvasSize.height.toFloat(),
            Random(seed)
        )
    }

    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX   by remember { mutableFloatStateOf(0f) }
    var offsetY   by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(340f / 380f)
            .onSizeChanged { canvasSize = it }
            .clipToBounds()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val newScale = (zoomScale * zoom).coerceAtLeast(1f)
                    zoomScale = newScale
                    if (newScale > 1f) {
                        val maxX = canvasSize.width  * (newScale - 1f) / 2f
                        val maxY = canvasSize.height * (newScale - 1f) / 2f
                        offsetX = (offsetX + pan.x).coerceIn(-maxX, maxX)
                        offsetY = (offsetY + pan.y).coerceIn(-maxY, maxY)
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
    ) {
        val l = layout ?: return@Canvas
        val scale = size.width / 340f

        // intensité ≥ 3 visible dès le départ ; intensité 1-2 à partir de LOD_SECONDARY
        val showLowIntensityLabels = zoomScale >= LOD_SECONDARY
        val showAllLabels          = zoomScale >= LOD_ALL

        withTransform({
            // translate d'abord (espace écran), puis scale autour du centre
            translate(offsetX, offsetY)
            scale(zoomScale, zoomScale, Offset(size.width / 2f, size.height / 2f))
        }) {

            // 1. Traînées d'étoiles (remplacent les lignes)
            l.secondaryNodes.forEach { node ->
                val color = colorForIntensity(node.hsl, node.intensity)
                val dx = node.x - node.parentX
                val dy = node.y - node.parentY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist < 1f) return@forEach
                val nPoints = (dist / 5f).toInt().coerceAtLeast(4)
                val perpX = -dy / dist
                val perpY =  dx / dist
                val trailRng = Random(node.emotion.id)
                repeat(nPoints) { i ->
                    val t = i.toFloat() / nPoints
                    val jitter = (trailRng.nextFloat() - 0.5f) * 2.5f * scale
                    val dotR   = (0.7f + trailRng.nextFloat() * 0.7f) * scale
                    drawCircle(
                        color  = color.copy(alpha = t * 0.55f),
                        radius = dotR,
                        center = Offset(
                            node.parentX + dx * t + perpX * jitter,
                            node.parentY + dy * t + perpY * jitter
                        )
                    )
                }
            }

            // 2. Glows secondaires
            l.secondaryNodes.forEach { node ->
                val color = colorForIntensity(node.hsl, node.intensity)
                val glowR = node.r * 2.2f
                val alpha = 0.22f + node.intensity * 0.04f
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawCircle(
                        node.x, node.y, glowR,
                        NativePaint().apply {
                            isAntiAlias = true
                            this.color = color.copy(alpha = alpha).toArgb()
                            maskFilter = BlurMaskFilter(glowR, BlurMaskFilter.Blur.NORMAL)
                        }
                    )
                }
            }

            // 3. Glows primaires (uniquement si actif)
            l.primaryNodes.forEach { node ->
                if (!node.hasSelections) return@forEach
                val baseColor = Color(node.emotion.color.toColorInt())
                val glowR = l.primaryR * 2f
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawCircle(
                        node.x, node.y, glowR,
                        NativePaint().apply {
                            isAntiAlias = true
                            this.color = baseColor.copy(alpha = 0.28f).toArgb()
                            maskFilter = BlurMaskFilter(glowR, BlurMaskFilter.Blur.NORMAL)
                        }
                    )
                }
            }

            // 4. Nœuds secondaires
            l.secondaryNodes.forEach { node ->
                val color = colorForIntensity(node.hsl, node.intensity)
                val center = Offset(node.x, node.y)
                drawCircle(color = color, radius = node.r, center = center)

                val showLabel = showAllLabels || node.intensity >= 3 || showLowIntensityLabels
                if (showLabel) {
                    val labelAlpha = 0.45f + (node.intensity - 1) / 4f * 0.5f
                    val measured = textMeasurer.measure(
                        node.emotion.label,
                        TextStyle(
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = labelAlpha),
                            textAlign = TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(node.x - measured.size.width / 2f, node.y + node.r + 3f)
                    )
                }
            }

            // 5. Nœuds primaires
            l.primaryNodes.forEach { node ->
                val baseColor = Color(node.emotion.color.toColorInt())
                val center = Offset(node.x, node.y)
                if (node.hasSelections) {
                    drawCircle(color = baseColor, radius = l.primaryR, center = center)
                    val measured = textMeasurer.measure(
                        node.emotion.label,
                        TextStyle(
                            fontSize = 9.sp, fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(node.x - measured.size.width / 2f,
                                         node.y - measured.size.height / 2f)
                    )
                } else {
                    drawCircle(color = baseColor.copy(alpha = 0.12f), radius = l.primaryR, center = center)
                    drawCircle(color = baseColor.copy(alpha = 0.35f), radius = l.primaryR,
                               center = center, style = Stroke(width = 1f))
                    val measured = textMeasurer.measure(
                        node.emotion.label,
                        TextStyle(
                            fontSize = 8.sp,
                            color = baseColor.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(node.x - measured.size.width / 2f,
                                         node.y - measured.size.height / 2f)
                    )
                }
            }
        }
    }
}
