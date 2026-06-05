package com.hibol.miette.soi.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import com.hibol.miette.soi.data.entity.Emotion
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

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
    // Tout est exprimé relativement à la taille de la maquette (340px) pour s'adapter à n'importe quelle largeur
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
        // + 0.5 pour placer la primaire au milieu du segment, identique au label de la roue de saisie
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
    // Seed stable : mêmes sélections → même placement, même après rotation d'écran
    val seed = remember(selections) {
        selections.fold(0L) { acc, (e, i) -> acc * 31L + e.id * 7L + i.toLong() }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(340f / 380f)
    ) {
        val scale = size.width / 340f
        val layout = buildLayout(primaryEmotions, selections, size.width, size.height, Random(seed))

        // 1. Lignes de connexion secondaire → primaire
        layout.secondaryNodes.forEach { node ->
            drawLine(
                color = colorForIntensity(node.hsl, node.intensity).copy(alpha = 0.55f),
                start = Offset(node.parentX, node.parentY),
                end = Offset(node.x, node.y),
                strokeWidth = 1.5f * scale
            )
        }

        // 2. Nœuds secondaires
        layout.secondaryNodes.forEach { node ->
            val color = colorForIntensity(node.hsl, node.intensity)
            drawCircle(color = color, radius = node.r, center = Offset(node.x, node.y))
            val labelAlpha = 0.45f + (node.intensity - 1) / 4f * 0.5f
            val measured = textMeasurer.measure(
                node.emotion.label,
                TextStyle(fontSize = 8.sp, color = Color.White.copy(alpha = labelAlpha),
                          textAlign = TextAlign.Center)
            )
            drawText(
                textLayoutResult = measured,
                topLeft = Offset(node.x - measured.size.width / 2f, node.y + node.r + 3f)
            )
        }

        // 3. Nœuds primaires (par-dessus les lignes et secondaires)
        layout.primaryNodes.forEach { node ->
            val baseColor = Color(node.emotion.color.toColorInt())
            val center = Offset(node.x, node.y)
            if (node.hasSelections) {
                drawCircle(color = baseColor, radius = layout.primaryR, center = center)
                val measured = textMeasurer.measure(
                    node.emotion.label,
                    TextStyle(fontSize = 9.sp, fontWeight = FontWeight.Medium,
                              color = Color.White.copy(alpha = 0.95f), textAlign = TextAlign.Center)
                )
                drawText(
                    textLayoutResult = measured,
                    topLeft = Offset(node.x - measured.size.width / 2f,
                                     node.y - measured.size.height / 2f)
                )
            } else {
                drawCircle(color = baseColor.copy(alpha = 0.12f), radius = layout.primaryR, center = center)
                drawCircle(color = baseColor.copy(alpha = 0.35f), radius = layout.primaryR,
                           center = center, style = Stroke(width = 1f))
                val measured = textMeasurer.measure(
                    node.emotion.label,
                    TextStyle(fontSize = 8.sp, color = baseColor.copy(alpha = 0.55f),
                              textAlign = TextAlign.Center)
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
