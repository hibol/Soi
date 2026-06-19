package com.hibol.miette.soi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hibol.miette.soi.R

val Dosis = FontFamily(
    Font(R.font.dosis_extralight, FontWeight.ExtraLight),
    Font(R.font.dosis_light,      FontWeight.Light),
    Font(R.font.dosis_regular,    FontWeight.Normal),
    Font(R.font.dosis_medium,     FontWeight.Medium),
    Font(R.font.dosis_semibold,   FontWeight.SemiBold),
    Font(R.font.dosis_extrabold,  FontWeight.ExtraBold),
)

val AppTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.ExtraLight, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.ExtraLight, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Light,      fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Light,   fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Normal,  fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Normal,  fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleSmall  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge   = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall   = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Light,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall  = TextStyle(fontFamily = Dosis, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
)
