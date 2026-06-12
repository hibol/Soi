package com.hibol.miette.soi.data.entity

import androidx.room.ColumnInfo

data class EmotionTrendPoint(
    @ColumnInfo(name = "dayEpoch") val dayEpoch: Long,
    @ColumnInfo(name = "primaryEmotionId") val primaryEmotionId: Long,
    @ColumnInfo(name = "avgIntensity") val avgIntensity: Float
)
