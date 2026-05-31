package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.SessionEntryPart
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionEntryPartDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sessionEntryPart: SessionEntryPart)

    @Query("UPDATE session_entry_part SET source = 'explicit' WHERE sessionEntryId = :sessionEntryId AND partId = :partId")
    suspend fun confirm(sessionEntryId: Long, partId: Long)

    @Query("SELECT * FROM session_entry_part WHERE sessionEntryId = :sessionEntryId")
    fun getBySession(sessionEntryId: Long): Flow<List<SessionEntryPart>>

    @Query("DELETE FROM session_entry_part WHERE sessionEntryId = :sessionEntryId AND partId = :partId")
    suspend fun delete(sessionEntryId: Long, partId: Long)
}