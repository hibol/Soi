package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.entity.PartTraitLink
import kotlinx.coroutines.flow.Flow

@Dao
interface PartTraitDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(trait: PartTrait): Long

    @Query("SELECT * FROM part_trait WHERE source = 'preset' ORDER BY label ASC")
    suspend fun getAllPresetOnce(): List<PartTrait>

    @Query("SELECT * FROM part_trait WHERE source = 'preset' ORDER BY label ASC")
    fun observePreset(): Flow<List<PartTrait>>

    @Query("""
        SELECT pt.* FROM part_trait pt
        INNER JOIN part_trait_link ptl ON pt.id = ptl.traitId
        WHERE ptl.partId = :partId
        ORDER BY pt.label ASC
    """)
    fun getTraitsForPart(partId: Long): Flow<List<PartTrait>>

    @Query("""
        SELECT pt.* FROM part_trait pt
        INNER JOIN part_trait_link ptl ON pt.id = ptl.traitId
        WHERE ptl.partId = :partId
        ORDER BY pt.label ASC
    """)
    suspend fun getTraitsForPartOnce(partId: Long): List<PartTrait>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun linkTrait(link: PartTraitLink)

    @Query("DELETE FROM part_trait_link WHERE partId = :partId")
    suspend fun unlinkAllTraits(partId: Long)

    @Query("SELECT * FROM part_trait WHERE LOWER(label) = LOWER(:label) LIMIT 1")
    suspend fun findByLabel(label: String): PartTrait?
}
