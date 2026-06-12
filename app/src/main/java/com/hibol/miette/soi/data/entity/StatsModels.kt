package com.hibol.miette.soi.data.entity

import androidx.room.ColumnInfo

data class MemoryQualityCount(
    @ColumnInfo(name = "quality") val quality: String,
    @ColumnInfo(name = "count") val count: Int
)

data class TagCount(
    @ColumnInfo(name = "label") val label: String,
    @ColumnInfo(name = "count") val count: Int
)

data class PartCount(
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "count") val count: Int
)

/** Statistiques globales (toute la durée de vie du journal). */
data class GlobalStats(
    val totalEntries: Int,
    val activeSinceDays: Long,
    val currentStreak: Int,
    val recordStreak: Int
)

/** Statistiques filtrées par la période sélectionnée. */
data class PeriodStats(
    val byType: Map<EntryType, Int>,
    val avgPerWeek: Float,
    val topDayOfWeek: Int?,          // DayOfWeek.value : 1=Lun … 7=Dim
    val memoryQuality: List<MemoryQualityCount>,
    val topTags: List<TagCount>,
    val topParts: List<PartCount>
)
