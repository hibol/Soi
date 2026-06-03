package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tag",
    indices = [Index(value = ["label"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val source: String = "manual"   // "manual" | "nlp" (V2)
)
