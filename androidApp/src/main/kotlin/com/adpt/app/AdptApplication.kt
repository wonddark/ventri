package com.adpt.app

import android.app.Application
import com.adpt.shared.db.AdptDatabase
import com.adpt.shared.db.DatabaseDriverFactory
import com.adpt.shared.db.createDatabase

class AdptApplication : Application() {

    lateinit var database: AdptDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        database = createDatabase(DatabaseDriverFactory(this))
    }
}
