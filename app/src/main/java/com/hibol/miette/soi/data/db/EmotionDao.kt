package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hibol.miette.soi.data.entity.Emotion
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionDao {

    @Insert
    suspend fun insertAll(emotions: List<Emotion>)

    @Insert
    suspend fun insert(emotion: Emotion): Long

    @Update
    suspend fun update(emotion: Emotion)

    @Query("DELETE FROM emotion WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM emotion")
    suspend fun getAllOnce(): List<Emotion>

    @Query("SELECT * FROM emotion ORDER BY label ASC")
    fun getAllEmotions(): Flow<List<Emotion>>

    @Query("SELECT * FROM emotion WHERE valence >= 0 ORDER BY label ASC")
    fun getPositiveEmotions(): Flow<List<Emotion>>

    @Query("SELECT * FROM emotion WHERE valence < 0 ORDER BY label ASC")
    fun getNegativeEmotions(): Flow<List<Emotion>>

    @Query("SELECT * FROM emotion WHERE level = :level ORDER BY label ASC")
    fun getEmotionsByLevel(level: Int): Flow<List<Emotion>>

    @Query("SELECT * FROM emotion WHERE parentId = :parentId ORDER BY label ASC")
    fun getChildren(parentId: Long): Flow<List<Emotion>>
}