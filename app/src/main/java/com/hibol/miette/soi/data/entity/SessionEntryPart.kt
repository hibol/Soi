package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "session_entry_part",
    primaryKeys = ["sessionEntryId", "partId"],
    foreignKeys = [
        ForeignKey(
            entity = SessionEntry::class,
            parentColumns = ["id"],
            childColumns = ["sessionEntryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Part::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionEntryId"), Index("partId")]
)
data class SessionEntryPart(
    val sessionEntryId: Long,
    val partId: Long,
    val source: String = "suggested", // "explicit" | "suggested" | "nlp" (V2)
    val confidence: Double? = null    // 0.0–1.0, V2
)