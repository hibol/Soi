package com.hibol.miette.soi.data.repository

import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.entity.CycleDay
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CycleDayRepository(private val db: SoiDatabase) {

    suspend fun toggle(profileId: Long, date: LocalDate) {
        val epochDay = date.toEpochDay()
        val inserted = db.cycleDayDao().insert(CycleDay(profileId = profileId, epochDay = epochDay))
        if (inserted == -1L) db.cycleDayDao().delete(profileId, epochDay)
    }

    fun getForMonth(profileId: Long, month: YearMonth): Flow<List<LocalDate>> {
        val from = month.atDay(1).toEpochDay()
        val to = month.atEndOfMonth().toEpochDay()
        return db.cycleDayDao().getEpochDaysInRange(profileId, from, to)
            .map { epochs -> epochs.map { LocalDate.ofEpochDay(it) } }
    }
}
