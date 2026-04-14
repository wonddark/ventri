package com.ventri.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ventri.app.VentriApplication
import com.ventri.shared.model.ItemPriority
import com.ventri.shared.model.Severity
import com.ventri.shared.util.deltaToSeverity
import com.ventri.shared.util.estimatedDepletionDate
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
        val app = applicationContext as VentriApplication
        val db = app.database
        val thresholds = app.prefs.thresholdConfig.value
        val now = Clock.System.now().toEpochMilliseconds()

        val criticalItems = withContext(Dispatchers.IO) {
            db.itemQueries.selectAll().executeAsList().filter { item ->
                val depletion = item.estimatedDepletionDate()
                if (depletion == null) {
                    item.priority == ItemPriority.High || item.priority == ItemPriority.Highest
                } else {
                    deltaToSeverity(depletion - now, thresholds) == Severity.Critical
                }
            }
        }

        if (criticalItems.isNotEmpty()) {
            NotificationHelper.showCriticalNotification(applicationContext, criticalItems)
        }

        return Result.success()
    }
}
