package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.Entry
import com.hibol.miette.soi.data.entity.EntryPart
import com.hibol.miette.soi.data.entity.PartCount
import kotlinx.coroutines.flow.Flow

data class EntryPartRow(
    val partId: Long,
    val partName: String,
    val source: String
)

@Dao
interface EntryPartDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(ep: EntryPart)

    @Query("""
        SELECT ep.partId, p.name AS partName, ep.source
        FROM entry_part ep
        INNER JOIN part p ON ep.partId = p.id
        WHERE ep.entryId = :entryId
        ORDER BY p.name ASC
    """)
    fun getPartsForEntry(entryId: Long): Flow<List<EntryPartRow>>

    @Query("SELECT partId FROM entry_part WHERE entryId = :entryId")
    suspend fun getPartIdsForEntryOnce(entryId: Long): List<Long>

    @Query("UPDATE entry_part SET source = 'explicit' WHERE entryId = :entryId AND partId = :partId")
    suspend fun confirm(entryId: Long, partId: Long)

    @Query("UPDATE entry_part SET source = 'suggested' WHERE entryId = :entryId AND partId = :partId")
    suspend fun unconfirm(entryId: Long, partId: Long)

    @Query("DELETE FROM entry_part WHERE entryId = :entryId AND partId = :partId")
    suspend fun delete(entryId: Long, partId: Long)

    @Query("""
        SELECT e.* FROM entry e
        INNER JOIN entry_part ep ON e.id = ep.entryId
        WHERE ep.partId = :partId AND ep.source = 'explicit'
        ORDER BY e.entryDate DESC
    """)
    fun getExplicitEntriesForPart(partId: Long): Flow<List<Entry>>

    @Query("""
        SELECT p.name AS name, COUNT(*) AS count
        FROM entry_part ep
        INNER JOIN part p ON ep.partId = p.id
        INNER JOIN entry e ON ep.entryId = e.id
        WHERE e.profileId = :profileId
          AND e.entryDate >= :fromMillis AND e.entryDate < :toMillis
        GROUP BY ep.partId
        ORDER BY count DESC
        LIMIT :limit
    """)
    fun getTopExplicitPartsByProfile(profileId: Long, fromMillis: Long, toMillis: Long, limit: Int): Flow<List<PartCount>>
}
