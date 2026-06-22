package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.hibol.miette.soi.data.entity.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {

    @Insert
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("DELETE FROM entry WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM entry WHERE id = :id")
    fun getById(id: Long): Flow<Entry?>

    @Query("""
        SELECT * FROM entry 
        WHERE profileId = :profileId 
        ORDER BY entryDate DESC
    """)
    fun getAllByProfile(profileId: Long): Flow<List<Entry>>

    @Query("""
        SELECT * FROM entry 
        WHERE profileId = :profileId 
        AND entryType = :type 
        ORDER BY entryDate DESC
    """)
    fun getByType(profileId: Long, type: String): Flow<List<Entry>>

    @Query("""
        SELECT * FROM entry
        WHERE profileId = :profileId
        AND entryDate BETWEEN :from AND :to
        ORDER BY entryDate ASC
    """)
    fun getByDateRange(profileId: Long, from: Long, to: Long): Flow<List<Entry>>

    // observedEntities : Room sait qu'il doit invalider ce Flow quand entry change
    @RawQuery(observedEntities = [Entry::class])
    fun searchRaw(query: SupportSQLiteQuery): Flow<List<Entry>>
}