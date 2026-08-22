package org.nirmalam.chant

import android.app.Application
import org.nirmalam.chant.data.AppDatabase
import org.nirmalam.chant.data.ChantRepository

class NirmalamApplication : Application() {
    val repository: ChantRepository by lazy {
        ChantRepository(AppDatabase.getInstance(this).chantDao())
    }
}
