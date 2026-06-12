package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.EntryTag
import com.hibol.miette.soi.data.entity.Tag
import com.hibol.miette.soi.data.entity.TagCount
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryTagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entryTag: EntryTag)

    @Query("DELETE FROM entry_tag WHERE entryId = :entryId")
    suspend fun deleteAllForEntry(entryId: Long)

    @Query("SELECT * FROM entry_tag WHERE entryId = :entryId")
    fun getByEntry(entryId: Long): Flow<List<EntryTag>>

    @Query("SELECT tag.* FROM tag INNER JOIN entry_tag ON tag.id = entry_tag.tagId WHERE entry_tag.entryId = :entryId")
    fun getTagsForEntry(entryId: Long): Flow<List<Tag>>

    @Query("""
        SELECT t.label AS label, COUNT(*) AS count
        FROM entry_tag et
        INNER JOIN tag t ON et.tagId = t.id
        INNER JOIN entry e ON et.entryId = e.id
        WHERE e.profileId = :profileId AND e.entryDate >= :fromMillis AND e.entryDate < :toMillis
        GROUP BY et.tagId
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopTagsByProfile(profileId: Long, fromMillis: Long, toMillis: Long, limit: Int): Flow<List<TagCount>>
}