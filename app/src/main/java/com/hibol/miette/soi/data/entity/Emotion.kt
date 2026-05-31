package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "emotion",
    foreignKeys = [
        ForeignKey(
            entity = Emotion::class,
            parentColumns = ["id"],
            childColumns = ["parentId"]
        )
    ],
    indices = [Index("parentId")]
)
data class Emotion(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val level: Int,                 // 1 = primaire, 2 = secondaire
    val parentId: Long? = null,     // null pour niveau 1
    val valence: Double? = null,
    val arousal: Double? = null,
    val color: String
)