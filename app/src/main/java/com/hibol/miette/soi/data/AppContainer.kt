package com.hibol.miette.soi.data

import android.content.Context
import com.hibol.miette.soi.data.db.SoiDatabase
import com.hibol.miette.soi.data.repository.EmotionRepository
import com.hibol.miette.soi.data.repository.EntryRepository
import com.hibol.miette.soi.data.repository.PartRepository
import com.hibol.miette.soi.data.repository.ProfileRepository
import com.hibol.miette.soi.data.repository.TagRepository

class AppContainer(context: Context) {
    val database = SoiDatabase.getInstance(context)

    val profileRepository = ProfileRepository(database)
    val emotionRepository = EmotionRepository(database)
    val tagRepository = TagRepository(database)
    val entryRepository = EntryRepository(database)
    val partRepository = PartRepository(database)
}