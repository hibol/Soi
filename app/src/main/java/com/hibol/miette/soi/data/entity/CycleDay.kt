package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cycle_day",
    foreignKeys = [ForeignKey(
        entity = Profile::class,
        parentColumns = ["id"],
        childColumns = ["profileId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [
        Index("profileId"),
        Index(value = ["profileId", "epochDay"], unique = true)
    ]
)
data class CycleDay(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val profileId: Long,
    val epochDay: Long  // LocalDate.toEpochDay() — pas de fuseau, date pure
)
