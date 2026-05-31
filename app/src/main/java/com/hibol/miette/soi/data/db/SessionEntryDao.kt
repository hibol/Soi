package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hibol.miette.soi.data.entity.SessionEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEntryDao {

    @Insert
    suspend fun insert(sessionEntry: SessionEntry)

    @Update
    suspend fun update(sessionEntry: SessionEntry)

    @Query("SELECT * FROM session_entry WHERE id = :id")
    fun getById(id: Long): Flow<SessionEntry?>
}