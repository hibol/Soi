package com.hibol.miette.soi.data.db

import androidx.room.Dao
import androidx.room.Insert
import com.hibol.miette.soi.data.entity.EventEntry

@Dao
interface EventEntryDao {

    @Insert
    suspend fun insert(eventEntry: EventEntry)
}