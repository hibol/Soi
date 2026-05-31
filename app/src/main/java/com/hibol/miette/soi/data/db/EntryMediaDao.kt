package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hibol.miette.soi.data.entity.EntryMedia
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryMediaDao {

    @Insert
    suspend fun insert(entryMedia: EntryMedia): Long

    @Query("DELETE FROM entry_media WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM entry_media WHERE entryId = :entryId ORDER BY createdAt ASC")
    fun getByEntry(entryId: Long): Flow<List<EntryMedia>>
}