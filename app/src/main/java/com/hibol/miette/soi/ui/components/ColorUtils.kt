package com.hibol.miette.soi.ui.components

import androidx.compose.ui.graphics.Color

/**
 * Convertit un hex (#RRGGBB) en triplet HSL (H: 0-360, S: 0-100, L: 0-100).
 */
fun hexToHsl(hex: String): Triple<Float, Float, Float> {
    val argb = android.graphics.Color.parseColor(hex)
    val r = android.graphics.Color.red(argb) / 255f
    val g = android.graphics.Color.green(argb) / 255f
    val b = android.graphics.Color.blue(argb) / 255f

    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f

    if (max == min) return Triple(0f, 0f, l * 100f)

    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    }

    return Triple(h * 360f, s * 100f, l * 100f)
}

/**
 * Retourne une couleur Compose en fonction de l'intensité (1-5).
 * Intensité 1 → très pâle (sat=15%, lum=78%).
 * Intensité 5 → couleur pleine.
 */
fun colorForIntensity(hsl: Triple<Float, Float, Float>, intensity: Int): Color {
    val (h, s, l) = hsl
    val t = (intensity - 1) / 4f
    val sat = (15f + t * (s - 15f)) / 100f
    val lum = (78f - t * (78f - l)) / 100f
    return Color.hsl(h, sat.coerceIn(0f, 1f), lum.coerceIn(0f, 1f))
}
