package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.*
import kotlinx.coroutines.flow.Flow

class EntryRepository(private val db: SoiDatabase) {

    fun getAllByProfile(profileId: Long): Flow<List<Entry>> =
        db.entryDao().getAllByProfile(profileId)

    fun getByType(profileId: Long, type: String): Flow<List<Entry>> =
        db.entryDao().getByType(profileId, type)

    fun getByDateRange(profileId: Long, from: Long, to: Long): Flow<List<Entry>> =
        db.entryDao().getByDateRange(profileId, from, to)

    fun getById(id: Long): Flow<Entry?> =
        db.entryDao().getById(id)

    suspend fun createDreamEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        memoryQuality: String,
        emotionIds: List<Pair<Long, Int>>,  // Pair(emotionId, intensity)
        tagLabels: List<String>
    ): Long {
        // 1. Créer l'Entry de base
        val entryId = db.entryDao().insert(
            Entry(
                profileId = profileId,
                entryType = "dream",
                entryDate = entryDate,
                text = text
            )
        )

        // 2. Créer la table satellite
        db.dreamEntryDao().insert(DreamEntry(id = entryId, memoryQuality = memoryQuality))

        // 3. Lier les émotions
        emotionIds.forEach { (emotionId, intensity) ->
            db.entryEmotionDao().insert(EntryEmotion(entryId = entryId, emotionId = emotionId, intensity = intensity))
        }

        // 4. Lier les tags
        tagLabels.forEach { label ->
            val tagId = TagRepository(db).getOrCreate(label)
            db.entryTagDao().insert(EntryTag(entryId = entryId, tagId = tagId))
        }

        return entryId
    }

    suspend fun createSessionEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>
    ): Long {
        val entryId = db.entryDao().insert(
            Entry(
                profileId = profileId,
                entryType = "session",
                entryDate = entryDate,
                text = text
            )
        )

        db.sessionEntryDao().insert(SessionEntry(id = entryId))

        emotionIds.forEach { (emotionId, intensity) ->
            db.entryEmotionDao().insert(
                EntryEmotion(entryId = entryId, emotionId = emotionId, intensity = intensity)
            )
        }

        tagLabels.forEach { label ->
            val tagId = TagRepository(db).getOrCreate(label)
            db.entryTagDao().insert(EntryTag(entryId = entryId, tagId = tagId))
        }

        return entryId
    }

    suspend fun createEventEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>
    ): Long {
        val entryId = db.entryDao().insert(
            Entry(
                profileId = profileId,
                entryType = "life_event",
                entryDate = entryDate,
                text = text
            )
        )

        db.eventEntryDao().insert(EventEntry(id = entryId))

        emotionIds.forEach { (emotionId, intensity) ->
            db.entryEmotionDao().insert(
                EntryEmotion(entryId = entryId, emotionId = emotionId, intensity = intensity)
            )
        }

        tagLabels.forEach { label ->
            val tagId = TagRepository(db).getOrCreate(label)
            db.entryTagDao().insert(EntryTag(entryId = entryId, tagId = tagId))
        }

        return entryId
    }

    suspend fun delete(id: Long) =
        db.entryDao().delete(id)
}