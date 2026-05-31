package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "session_entry",
    foreignKeys = [
        ForeignKey(
            entity = Entry::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SessionEntry(
    @PrimaryKey
    val id: Long                    // même id que Entry — pas d'autoGenerate
    // pas de champs supplémentaires en V1 — extensible en V2
)