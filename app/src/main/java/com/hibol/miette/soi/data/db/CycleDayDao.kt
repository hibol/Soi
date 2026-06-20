package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hibol.miette.soi.data.entity.CycleDay
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDayDao {

    // Retourne l'id inséré, ou -1 si la ligne existait déjà (UNIQUE conflict)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(cycleDay: CycleDay): Long

    @Query("DELETE FROM cycle_day WHERE profileId = :profileId AND epochDay = :epochDay")
    suspend fun delete(profileId: Long, epochDay: Long)

    @Query("""
        SELECT epochDay FROM cycle_day
        WHERE profileId = :profileId AND epochDay >= :from AND epochDay <= :to
    """)
    fun getEpochDaysInRange(profileId: Long, from: Long, to: Long): Flow<List<Long>>
}
