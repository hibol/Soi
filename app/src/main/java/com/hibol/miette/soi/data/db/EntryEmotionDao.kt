package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.EntryEmotion
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryEmotionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entryEmotion: EntryEmotion)

    @Query("DELETE FROM entry_emotion WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: Long)

    @Query("SELECT * FROM entry_emotion WHERE entryId = :entryId")
    fun getByEntry(entryId: Long): Flow<List<EntryEmotion>>
}