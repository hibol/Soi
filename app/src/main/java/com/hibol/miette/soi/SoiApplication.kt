package com.hibol.miette.soi

import android.app.Application
import com.hibol.miette.soi.data.db.DatabaseInitializer
import com.hibol.miette.soi.data.db.SoiDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoiApplication : Application() {

    val database by lazy { SoiDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer.populate(database)
        }
    }
}