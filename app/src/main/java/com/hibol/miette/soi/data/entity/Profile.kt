package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class Profile (
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val authType: String = "none",  // "none" | "pin" | "biometric"
    val pinHash: String? = null
)