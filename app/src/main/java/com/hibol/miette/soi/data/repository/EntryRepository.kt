package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class EntryRepository(private val db: SoiDatabase) {

    // ── Lecture ──────────────────────────────────────────────────────────────

    fun getAllByProfile(profileId: Long): Flow<List<Entry>> =
        db.entryDao().getAllByProfile(profileId)

    fun getByType(profileId: Long, type: String): Flow<List<Entry>> =
        db.entryDao().getByType(profileId, type)

    fun getByDateRange(profileId: Long, from: Long, to: Long): Flow<List<Entry>> =
        db.entryDao().getByDateRange(profileId, from, to)

    fun getById(id: Long): Flow<Entry?> =
        db.entryDao().getById(id)

    fun getEmotionsForEntry(entryId: Long): Flow<List<EntryEmotion>> =
        db.entryEmotionDao().getByEntry(entryId)

    fun getTagsForEntry(entryId: Long): Flow<List<Tag>> =
        db.entryTagDao().getTagsForEntry(entryId)

    fun getDreamDetail(entryId: Long): Flow<DreamEntry?> =
        db.dreamEntryDao().getById(entryId)

    // ── Création ─────────────────────────────────────────────────────────────

    suspend fun createDreamEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        memoryQuality: String,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ): Long {
        val entryId = insertBaseEntry(profileId, "dream", entryDate, text, isBlurred)
        db.dreamEntryDao().insert(DreamEntry(id = entryId, memoryQuality = memoryQuality))
        saveEmotionsAndTags(entryId, emotionIds, tagLabels)
        return entryId
    }

    suspend fun createSessionEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ): Long {
        val entryId = insertBaseEntry(profileId, "session", entryDate, text, isBlurred)
        db.sessionEntryDao().insert(SessionEntry(id = entryId))
        saveEmotionsAndTags(entryId, emotionIds, tagLabels)
        return entryId
    }

    suspend fun createEventEntry(
        profileId: Long,
        entryDate: Long,
        text: String?,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ): Long {
        val entryId = insertBaseEntry(profileId, "life_event", entryDate, text, isBlurred)
        db.eventEntryDao().insert(EventEntry(id = entryId))
        saveEmotionsAndTags(entryId, emotionIds, tagLabels)
        return entryId
    }

    // ── Mise à jour ───────────────────────────────────────────────────────────

    suspend fun updateDreamEntry(
        entryId: Long,
        text: String?,
        memoryQuality: String,
        entryDate: Long,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ) {
        if (!updateBaseEntry(entryId, text, entryDate, isBlurred)) return
        db.dreamEntryDao().update(DreamEntry(id = entryId, memoryQuality = memoryQuality))
        replaceEmotionsAndTags(entryId, emotionIds, tagLabels)
    }

    suspend fun updateSessionEntry(
        entryId: Long,
        text: String?,
        entryDate: Long,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ) {
        if (!updateBaseEntry(entryId, text, entryDate, isBlurred)) return
        replaceEmotionsAndTags(entryId, emotionIds, tagLabels)
    }

    suspend fun updateEventEntry(
        entryId: Long,
        text: String?,
        entryDate: Long,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>,
        isBlurred: Boolean = false
    ) {
        if (!updateBaseEntry(entryId, text, entryDate, isBlurred)) return
        replaceEmotionsAndTags(entryId, emotionIds, tagLabels)
    }

    // ── Suppression ───────────────────────────────────────────────────────────

    suspend fun delete(id: Long) =
        db.entryDao().delete(id)

    // ── Helpers privés ────────────────────────────────────────────────────────

    private suspend fun insertBaseEntry(
        profileId: Long,
        entryType: String,
        entryDate: Long,
        text: String?,
        isBlurred: Boolean
    ): Long = db.entryDao().insert(
        Entry(
            profileId = profileId,
            entryType = entryType,
            entryDate = entryDate,
            text = text,
            isBlurred = if (isBlurred) 1 else 0
        )
    )

    // Retourne false si l'entrée n'existe plus (cas rare : suppression concurrente)
    private suspend fun updateBaseEntry(
        entryId: Long,
        text: String?,
        entryDate: Long,
        isBlurred: Boolean
    ): Boolean {
        val existing = db.entryDao().getById(entryId).first() ?: return false
        db.entryDao().update(
            existing.copy(
                text = text,
                entryDate = entryDate,
                isBlurred = if (isBlurred) 1 else 0,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    private suspend fun saveEmotionsAndTags(
        entryId: Long,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>
    ) {
        emotionIds.forEach { (emotionId, intensity) ->
            db.entryEmotionDao().insert(
                EntryEmotion(entryId = entryId, emotionId = emotionId, intensity = intensity)
            )
        }
        tagLabels.forEach { label ->
            val tagId = TagRepository(db).getOrCreate(label)
            db.entryTagDao().insert(EntryTag(entryId = entryId, tagId = tagId))
        }
    }

    private suspend fun replaceEmotionsAndTags(
        entryId: Long,
        emotionIds: List<Pair<Long, Int>>,
        tagLabels: List<String>
    ) {
        db.entryEmotionDao().deleteAllForEntry(entryId)
        db.entryTagDao().deleteAllForEntry(entryId)
        saveEmotionsAndTags(entryId, emotionIds, tagLabels)
    }
}