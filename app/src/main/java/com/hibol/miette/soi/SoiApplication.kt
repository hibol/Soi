package com.hibol.miette.soi

import android.app.Application
import com.hibol.miette.soi.data.AppContainer
import com.hibol.miette.soi.data.db.DatabaseInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SoiApplication : Application() {

    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseInitializer.populate(container.emotionRepository)
        }
    }
}