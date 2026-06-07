package com.hibol.miette.soi.data.repository

import androidx.room.withTransaction
import com.hibol.miette.soi.data.db.EntryPartRow
import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryPart
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.entity.PartTraitLink
import com.hibol.miette.soi.data.util.PartDetector
import kotlinx.coroutines.flow.Flow

class PartRepository(private val db: SoiDatabase) {

    // ── Parts ────────────────────────────────────────────────────────────────

    fun getAllByProfile(profileId: Long): Flow<List<Part>> =
        db.partDao().getAllByProfile(profileId)

    fun getById(id: Long): Flow<Part?> =
        db.partDao().getById(id)

    fun searchByName(profileId: Long, search: String): Flow<List<Part>> =
        db.partDao().searchByName(profileId, search)

    suspend fun create(part: Part): Long =
        db.partDao().insert(part)

    suspend fun update(part: Part) =
        db.partDao().update(part)

    suspend fun delete(id: Long) =
        db.partDao().delete(id)

    // ── Traits ───────────────────────────────────────────────────────────────

    suspend fun getAllPresetTraits(): List<PartTrait> =
        db.partTraitDao().getAllPresetOnce()

    fun observePresetTraits(): Flow<List<PartTrait>> =
        db.partTraitDao().observePreset()

    fun getTraitsForPart(partId: Long): Flow<List<PartTrait>> =
        db.partTraitDao().getTraitsForPart(partId)

    suspend fun getTraitsForPartOnce(partId: Long): List<PartTrait> =
        db.partTraitDao().getTraitsForPartOnce(partId)

    suspend fun syncTraits(partId: Long, traitIds: Set<Long>) {
        db.withTransaction {
            db.partTraitDao().unlinkAllTraits(partId)
            for (traitId in traitIds) {
                db.partTraitDao().linkTrait(PartTraitLink(partId, traitId))
            }
        }
    }

    // Cherche un trait existant (preset ou manual) ; en crée un nouveau si absent
    suspend fun getOrCreateManualTrait(label: String): PartTrait {
        val trimmed = label.trim()
        db.partTraitDao().findByLabel(trimmed)?.let { return it }
        val id = db.partTraitDao().insert(PartTrait(label = trimmed, source = "manual"))
        return PartTrait(id = id, label = trimmed, source = "manual")
    }

    // ── EntryPart ─────────────────────────────────────────────────────────────

    fun getPartsForEntry(entryId: Long): Flow<List<EntryPartRow>> =
        db.entryPartDao().getPartsForEntry(entryId)

    suspend fun detectAndLinkParts(entryId: Long, text: String, profileId: Long) {
        if (text.isBlank()) return
        val parts = db.partDao().getAllByProfileOnce(profileId)
        val detected = PartDetector.detect(text, parts)
        val existing = db.entryPartDao().getPartIdsForEntryOnce(entryId).toSet()
        for (part in detected) {
            if (part.id !in existing) {
                db.entryPartDao().insert(EntryPart(entryId = entryId, partId = part.id))
            }
        }
    }

    suspend fun confirmEntryPart(entryId: Long, partId: Long) =
        db.entryPartDao().confirm(entryId, partId)

    suspend fun unconfirmEntryPart(entryId: Long, partId: Long) =
        db.entryPartDao().unconfirm(entryId, partId)

    suspend fun removeEntryPart(entryId: Long, partId: Long) =
        db.entryPartDao().delete(entryId, partId)

    suspend fun linkEntryPartExplicit(entryId: Long, partId: Long) =
        db.entryPartDao().insert(EntryPart(entryId = entryId, partId = partId, source = "explicit"))

    // Alias pour le bouton refresh — même logique additive que detectAndLink
    suspend fun redetectParts(entryId: Long, text: String, profileId: Long) =
        detectAndLinkParts(entryId, text, profileId)

    fun getExplicitEntriesForPart(partId: Long): Flow<List<Entry>> =
        db.entryPartDao().getExplicitEntriesForPart(partId)

}
