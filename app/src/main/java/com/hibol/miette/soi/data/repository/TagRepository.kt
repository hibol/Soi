package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.Tag
import kotlinx.coroutines.flow.Flow

class TagRepository(private val db: SoiDatabase) {

    fun getAllTags(): Flow<List<Tag>> =
        db.tagDao().getAllTags()

    fun searchTags(search: String): Flow<List<Tag>> =
        db.tagDao().searchTags(search)

    suspend fun getOrCreate(label: String): Long {
        val trimmed = label.trim().lowercase()
        db.tagDao().insert(Tag(label = trimmed))
        return db.tagDao().getByLabel(trimmed)!!.id
    }
}