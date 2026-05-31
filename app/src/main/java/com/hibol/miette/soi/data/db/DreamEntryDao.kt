package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hibol.miette.soi.data.entity.DreamEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamEntryDao {

    @Insert
    suspend fun insert(dreamEntry: DreamEntry)

    @Update
    suspend fun update(dreamEntry: DreamEntry)

    @Query("SELECT * FROM dream_entry WHERE id = :id")
    fun getById(id: Long): Flow<DreamEntry?>
}