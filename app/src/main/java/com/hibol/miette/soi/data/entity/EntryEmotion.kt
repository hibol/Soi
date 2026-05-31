package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "entry_emotion",
    primaryKeys = ["entryId", "emotionId"],
    foreignKeys = [
        ForeignKey(
            entity = Entry::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Emotion::class,
            parentColumns = ["id"],
            childColumns = ["emotionId"]
        )
    ],
    indices = [Index("entryId"), Index("emotionId")]
)
data class EntryEmotion(
    val entryId: Long,
    val emotionId: Long,
    val intensity: Int,             // 1 à 5
    val phase: String? = null       // null (V1) | "start" | "middle" | "end" (V2)
)