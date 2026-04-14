package com.ventri.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.ventri.app.VentriApplication
import com.ventri.app.MainActivity
import com.ventri.app.Routes
import com.ventri.shared.model.AddToShoppingListResult
import com.ventri.shared.util.addToShoppingList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AddToShoppingListReceiver : BroadcastReceiver() {

    companion object {
        const val EXTRA_ITEM_IDS = "extra_item_ids"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val itemIds = intent.getStringArrayExtra(EXTRA_ITEM_IDS) ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as VentriApplication
                var addedCount = 0

                itemIds.forEach { itemId ->
                    when (app.database.addToShoppingList(itemId)) {
                        is AddToShoppingListResult.Success -> addedCount++
                        AddToShoppingListResult.AlreadyInList -> addedCount++ // already there = goal achieved
                        AddToShoppingListResult.ItemNotFound -> { /* skip */ }
                    }
                }

                NotificationHelper.showSuccessNotification(context, addedCount)
            } catch (e: Exception) {
                handleError(context, "Could not add items to the shopping list: ${e.message}")
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleError(context: Context, message: String) {
        val app = context.applicationContext as VentriApplication
        app.pendingShoppingError.value = message
        app.pendingNavTarget.value = Routes.SHOPPING
        NotificationManagerCompat.from(context).cancel(NotificationHelper.NOTIFICATION_ID)
        context.startActivity(
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
    }
}
