package com.hibol.miette.soi.data.entity

import androidx.room.ColumnInfo

data class EmotionTrendPoint(
    @ColumnInfo(name = "localDay") val localDay: String,  // "YYYY-MM-DD" dans le fuseau local
    @ColumnInfo(name = "primaryEmotionId") val primaryEmotionId: Long,
    @ColumnInfo(name = "intensity") val intensity: Float
)
