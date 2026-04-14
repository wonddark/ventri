package com.ventri.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.ventri.app.notifications.CriticalItemsWorker
import com.ventri.app.notifications.NotificationHelper
import com.ventri.app.preferences.AppPreferencesRepository
import com.ventri.app.preferences.NotificationFrequency
import com.ventri.shared.db.VentriDatabase
import com.ventri.shared.db.DatabaseDriverFactory
import com.ventri.shared.db.createDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

private val Application.dataStore by preferencesDataStore(name = "app_prefs")

class VentriApplication : Application() {

    lateinit var database: VentriDatabase
        private set

    lateinit var prefs: AppPreferencesRepository
        private set

    val pendingNavTarget = MutableStateFlow<String?>(null)
    val pendingShoppingError = MutableStateFlow<String?>(null)

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        database = createDatabase(DatabaseDriverFactory(this))
        prefs = AppPreferencesRepository(dataStore, applicationScope)
        NotificationHelper.createNotificationChannel(this)

        // Schedule immediately with whatever is stored, then reschedule on every change.
        scheduleStockCheck(prefs.notificationFrequency.value)
        applicationScope.launch {
            prefs.notificationFrequency.drop(1).collect { freq ->
                scheduleStockCheck(freq)
            }
        }
    }

    private fun scheduleStockCheck(frequency: NotificationFrequency) {
        val intervalHours = when (frequency) {
            NotificationFrequency.OncePerDay -> 24L
            NotificationFrequency.TwicePerDay -> 12L
        }
        val request = PeriodicWorkRequestBuilder<CriticalItemsWorker>(
            intervalHours, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            CriticalItemsWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }
}
