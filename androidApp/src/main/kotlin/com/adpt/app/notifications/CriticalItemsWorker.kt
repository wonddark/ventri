package com.adpt.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.adpt.app.AdptApplication
import com.adpt.shared.model.Severity
import com.adpt.shared.util.deltaToSeverity
import com.adpt.shared.util.estimatedDepletionDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

class CriticalItemsWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val WORK_NAME = "critical_stock_check"
    }

    override suspend fun doWork(): Result {
        val db = (applicationContext as AdptApplication).database
        val now = Clock.System.now().toEpochMilliseconds()

        val criticalItems = withContext(Dispatchers.IO) {
            db.itemQueries.selectAll().executeAsList().filter { item ->
                val depletion = item.estimatedDepletionDate() ?: return@filter false
                deltaToSeverity(depletion - now) == Severity.Critical
            }
        }

        if (criticalItems.isNotEmpty()) {
            NotificationHelper.showCriticalNotification(applicationContext, criticalItems)
        }

        return Result.success()
    }
}
