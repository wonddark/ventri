package com.ventri.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.ventri.app.MainActivity
import com.ventri.app.R
import com.ventri.app.Routes
import com.ventri.shared.db.Item

object NotificationHelper {

    const val CHANNEL_ID = "critical_stock"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Critical Stock Alerts",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily alerts when items are critically low on stock"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun showCriticalNotification(context: Context, items: List<Item>) {
        val itemIds = items.map { it.id }.toTypedArray()
        val names = items.joinToString(", ") { it.name }

        val contentText = if (items.size == 1) {
            "${items[0].name} is critically low on stock"
        } else {
            "${items.size} items are critically low: $names"
        }
        val actionLabel = if (items.size == 1) "Add to Shopping List" else "Add All to Shopping List"

        // Tap on the notification body → open Overview screen
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, Routes.OVERVIEW)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Action button → AddToShoppingListReceiver
        val actionIntent = Intent(context, AddToShoppingListReceiver::class.java).apply {
            putExtra(AddToShoppingListReceiver.EXTRA_ITEM_IDS, itemIds)
        }
        val actionPendingIntent = PendingIntent.getBroadcast(
            context, 1, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Stock Alert")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(tapPendingIntent)
            .addAction(0, actionLabel, actionPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    fun showSuccessNotification(context: Context, count: Int) {
        val text = if (count == 1) "1 item added to your shopping list"
                   else "$count items added to your shopping list"

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NAVIGATE_TO, Routes.SHOPPING)
        }
        val tapPendingIntent = PendingIntent.getActivity(
            context, 0, tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Shopping List Updated")
            .setContentText(text)
            .setContentIntent(tapPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }
}
