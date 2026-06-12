package com.hibol.miette.soi.data.entity

import androidx.room.ColumnInfo

data class TopSecondaryEmotion(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "color") val color: String,
    @ColumnInfo(name = "maxIntensity") val maxIntensity: Int
)
