package com.hibol.miette.soi.data.repository

import androidx.room.withTransaction
import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.PartTrait
import com.hibol.miette.soi.data.entity.PartTraitLink
import com.hibol.miette.soi.data.entity.SessionEntryPart
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

    // ── SessionEntryPart ─────────────────────────────────────────────────────

    suspend fun linkToSession(sessionEntryId: Long, partId: Long, source: String = "suggested") =
        db.sessionEntryPartDao().insert(
            SessionEntryPart(sessionEntryId = sessionEntryId, partId = partId, source = source)
        )

    suspend fun confirmLink(sessionEntryId: Long, partId: Long) =
        db.sessionEntryPartDao().confirm(sessionEntryId, partId)

    fun getPartsForSession(sessionEntryId: Long): Flow<List<SessionEntryPart>> =
        db.sessionEntryPartDao().getBySession(sessionEntryId)
}
