package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.Tag
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: Tag): Long

    @Query("SELECT * FROM tag ORDER BY label ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tag WHERE label LIKE :search || '%' ORDER BY label ASC")
    fun searchTags(search: String): Flow<List<Tag>>

    @Query("SELECT * FROM tag WHERE label = :label LIMIT 1")
    suspend fun getByLabel(label: String): Tag?
}