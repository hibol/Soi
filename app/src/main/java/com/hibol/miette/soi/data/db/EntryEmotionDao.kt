package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.EmotionTrendPoint
import com.hibol.miette.soi.data.entity.EntryEmotion
import com.hibol.miette.soi.data.entity.TopSecondaryEmotion
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryEmotionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entryEmotion: EntryEmotion)

    @Query("DELETE FROM entry_emotion WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: Long)

    @Query("DELETE FROM entry_emotion WHERE emotionId = :emotionId")
    suspend fun deleteByEmotionId(emotionId: Long)

    @Query("SELECT * FROM entry_emotion WHERE entryId = :entryId")
    fun getByEntry(entryId: Long): Flow<List<EntryEmotion>>

    @Query("SELECT ee.* FROM entry_emotion ee JOIN entry e ON ee.entryId = e.id WHERE e.profileId = :profileId")
    fun getAllForProfile(profileId: Long): Flow<List<EntryEmotion>>

    @Query("""
        SELECT strftime('%Y-%m-%d', datetime(e.entryDate / 1000, 'unixepoch', 'localtime')) AS localDay,
               COALESCE(em.parentId, em.id) AS primaryEmotionId,
               CAST(MAX(ee.intensity) AS FLOAT) AS intensity
        FROM entry_emotion ee
        JOIN entry e ON ee.entryId = e.id
        JOIN emotion em ON ee.emotionId = em.id
        WHERE e.profileId = :profileId
          AND e.entryDate >= :fromMillis
          AND e.entryDate < :toMillis
          AND e.entryType IN (:entryTypes)
        GROUP BY localDay, COALESCE(em.parentId, em.id)
        ORDER BY localDay ASC
    """)
    fun getHeatmapData(
        profileId: Long,
        fromMillis: Long,
        toMillis: Long,
        entryTypes: List<String>
    ): Flow<List<EmotionTrendPoint>>

    @Query("""
        SELECT em.id, em.label, em.color, MAX(ee.intensity) AS maxIntensity
        FROM entry_emotion ee
        JOIN entry e ON ee.entryId = e.id
        JOIN emotion em ON ee.emotionId = em.id
        WHERE e.profileId = :profileId
          AND e.entryDate >= :fromMillis
          AND e.entryDate < :toMillis
          AND e.entryType IN (:entryTypes)
          AND em.parentId = :primaryEmotionId
        GROUP BY em.id
        ORDER BY maxIntensity DESC
        LIMIT 3
    """)
    fun getTopSecondaryEmotions(
        profileId: Long,
        fromMillis: Long,
        toMillis: Long,
        entryTypes: List<String>,
        primaryEmotionId: Long
    ): Flow<List<TopSecondaryEmotion>>
}