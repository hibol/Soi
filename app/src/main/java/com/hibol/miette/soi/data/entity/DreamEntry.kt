package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "dream_entry",
    foreignKeys = [
        ForeignKey(
            entity = Entry::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class DreamEntry(
    @PrimaryKey
    val id: Long,                   // même id que Entry — pas d'autoGenerate
    val memoryQuality: String       // "flou" | "partiel" | "clair"
)