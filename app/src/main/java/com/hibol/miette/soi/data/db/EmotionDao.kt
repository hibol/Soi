package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.hibol.miette.soi.data.entity.Emotion
import kotlinx.coroutines.flow.Flow

@Dao
interface EmotionDao {

    @Insert
    suspend fun insertAll(emotions: List<Emotion>)

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