package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "entry",
    foreignKeys = [
        ForeignKey(
            entity = Profile::class,
            parentColumns = ["id"],
            childColumns = ["profileId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("profileId")]
)
data class Entry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val profileId: Long,
    val entryType: String,          // "dream" | "session" | "life_event"
    val createdAt: Long = System.currentTimeMillis(),
    val entryDate: Long = System.currentTimeMillis(),
    val text: String? = null,
    val extraData: String? = null,  // JSON libre, V2
    val updatedAt: Long = System.currentTimeMillis()
) {
    val type: EntryType
        get() = EntryType.entries.find { it.value == entryType } ?: throw IllegalStateException("Type d'entrée inconnu : $entryType")
}