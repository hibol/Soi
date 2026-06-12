package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hibol.miette.soi.data.entity.DreamEntry
import com.hibol.miette.soi.data.entity.MemoryQualityCount
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamEntryDao {

    @Insert
    suspend fun insert(dreamEntry: DreamEntry)

    @Update
    suspend fun update(dreamEntry: DreamEntry)

    @Query("SELECT * FROM dream_entry WHERE id = :id")
    fun getById(id: Long): Flow<DreamEntry?>

    @Query("""
        SELECT de.memoryQuality AS quality, COUNT(*) AS count
        FROM dream_entry de INNER JOIN entry e ON de.id = e.id
        WHERE e.profileId = :profileId AND e.entryDate >= :fromMillis AND e.entryDate < :toMillis
        GROUP BY de.memoryQuality
    """)
    fun getMemoryQualityCounts(profileId: Long, fromMillis: Long, toMillis: Long): Flow<List<MemoryQualityCount>>
}