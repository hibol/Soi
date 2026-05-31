package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.EntryTag
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entryTag: EntryTag)

    @Query("DELETE FROM entry_tag WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: Long)

    @Query("SELECT * FROM entry_tag WHERE entryId = :entryId")
    fun getByEntry(entryId: Long): Flow<List<EntryTag>>
}