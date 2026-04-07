package com.adpt.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.adpt.app.notifications.CriticalItemsWorker
import com.adpt.app.notifications.NotificationHelper
import com.adpt.shared.db.AdptDatabase
import com.adpt.shared.db.DatabaseDriverFactory
import com.adpt.shared.db.createDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.TimeUnit

class AdptApplication : Application() {

    lateinit var database: AdptDatabase
        private set

    /** Non-null while a notification-triggered navigation is pending. Cleared after navigating. */
    val pendingNavTarget = MutableStateFlow<String?>(null)

    /** Non-null when the notification action failed and an in-app error alert should be shown. */
    val pendingShoppingError = MutableStateFlow<String?>(null)

    override fun onCreate() {
        super.onCreate()
        database = createDatabase(DatabaseDriverFactory(this))
        NotificationHelper.createNotificationChannel(this)
        scheduleDailyStockCheck()
    }

    private fun scheduleDailyStockCheck() {
        val request = PeriodicWorkRequestBuilder<CriticalItemsWorker>(12, TimeUnit.HOURS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CriticalItemsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
