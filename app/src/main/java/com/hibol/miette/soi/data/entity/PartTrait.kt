package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "part_trait",
    indices = [Index(value = ["label"], unique = true)]
)
data class PartTrait(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val source: String = "preset" // "preset" | "manual" | "nlp" (V2)
)
