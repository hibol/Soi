package com.hibol.miette.soi.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.hibol.miette.soi.data.entity.EntryType

@Composable
@ReadOnlyComposable
fun extendedColorScheme(): ExtendedColorScheme =
    if (androidx.compose.foundation.isSystemInDarkTheme()) extendedDark else extendedLight

fun ExtendedColorScheme.colorFamilyForType(type: EntryType): ColorFamily = when (type) {
    EntryType.DREAM -> dream
    EntryType.SESSION -> session
    EntryType.LIFE_EVENT -> event
}