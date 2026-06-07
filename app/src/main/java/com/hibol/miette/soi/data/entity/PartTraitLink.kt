package com.hibol.miette.soi.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "part_trait_link",
    primaryKeys = ["partId", "traitId"],
    foreignKeys = [
        ForeignKey(
            entity = Part::class,
            parentColumns = ["id"],
            childColumns = ["partId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PartTrait::class,
            parentColumns = ["id"],
            childColumns = ["traitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("partId"), Index("traitId")]
)
data class PartTraitLink(
    val partId: Long,
    val traitId: Long
)
