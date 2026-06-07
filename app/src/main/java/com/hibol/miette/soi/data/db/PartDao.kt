package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hibol.miette.soi.data.entity.Part
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {

    @Insert
    suspend fun insert(part: Part): Long

    @Update
    suspend fun update(part: Part)

    @Query("DELETE FROM part WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM part WHERE profileId = :profileId ORDER BY name ASC")
    fun getAllByProfile(profileId: Long): Flow<List<Part>>

    @Query("SELECT * FROM part WHERE id = :id")
    fun getById(id: Long): Flow<Part?>

    @Query("SELECT * FROM part WHERE profileId = :profileId AND name LIKE '%' || :search || '%'")
    fun searchByName(profileId: Long, search: String): Flow<List<Part>>

    @Query("SELECT * FROM part WHERE profileId = :profileId ORDER BY name ASC")
    suspend fun getAllByProfileOnce(profileId: Long): List<Part>
}