package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Part
import com.hibol.miette.soi.data.entity.SessionEntryPart
import kotlinx.coroutines.flow.Flow

class PartRepository(private val db: SoiDatabase) {

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

    suspend fun linkToSession(sessionEntryId: Long, partId: Long, source: String = "suggested") =
        db.sessionEntryPartDao().insert(
            SessionEntryPart(
                sessionEntryId = sessionEntryId,
                partId = partId,
                source = source
            )
        )

    suspend fun confirmLink(sessionEntryId: Long, partId: Long) =
        db.sessionEntryPartDao().confirm(sessionEntryId, partId)

    fun getPartsForSession(sessionEntryId: Long): Flow<List<SessionEntryPart>> =
        db.sessionEntryPartDao().getBySession(sessionEntryId)
}