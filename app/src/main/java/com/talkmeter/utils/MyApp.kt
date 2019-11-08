package com.talkmeter.utils

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.talkmeter.BuildConfig
import com.talkmeter.db.AppDatabase

class MyApp : Application() {
    var db: AppDatabase? = null

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)

        base?.let{
            db = AppDatabase.getInstance(it)
        }
    }
}