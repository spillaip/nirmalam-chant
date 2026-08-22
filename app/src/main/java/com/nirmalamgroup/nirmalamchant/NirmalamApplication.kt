package com.nirmalamgroup.nirmalamchant

import android.app.Application
import com.nirmalamgroup.nirmalamchant.data.AppDatabase
import com.nirmalamgroup.nirmalamchant.data.ChantRepository

class NirmalamApplication : Application() {
    val repository: ChantRepository by lazy {
        ChantRepository(AppDatabase.getInstance(this).chantDao())
    }
}
