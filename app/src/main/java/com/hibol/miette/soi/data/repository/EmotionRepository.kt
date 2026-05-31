package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Emotion
import kotlinx.coroutines.flow.Flow

class EmotionRepository(private val db: SoiDatabase) {

    fun getAllEmotions(): Flow<List<Emotion>> =
        db.emotionDao().getAllEmotions()

    fun getPrimaryEmotions(): Flow<List<Emotion>> =
        db.emotionDao().getEmotionsByLevel(1)

    fun getChildren(parentId: Long): Flow<List<Emotion>> =
        db.emotionDao().getChildren(parentId)

    suspend fun insertAll(emotions: List<Emotion>) =
        db.emotionDao().insertAll(emotions)
}