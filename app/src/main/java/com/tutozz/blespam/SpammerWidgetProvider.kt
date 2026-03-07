package com.tutozz.blespam

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.Toast

class SpammerWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        
        val spammerTypes = when (intent.action) {
            ACTION_TOGGLE_APPLE -> listOf("iOS Crash", "Apple Action Modal", "Apple Device Popup", "Apple 'Not Your Device'")
            ACTION_TOGGLE_ANDROID -> listOf("Android Fast Pair", "Samsung Buds", "Samsung Watch", "Xiaomi Quick Connect")
            ACTION_TOGGLE_WINDOWS -> listOf("Windows Swift Pair")
            else -> emptyList()
        }

        if (spammerTypes.isNotEmpty()) {
            // Check if ANY of the requested spammers for this group are running
            val isRunning = spammerTypes.any { SpamService.isSpammerRunning(it) }
            
            if (isRunning) {
                // Stop all in the group
                spammerTypes.forEach { SpamService.stopSpammer(context, it) }
            } else {
                val bluetoothAvailability = BluetoothHelper.availability(context)
                if (bluetoothAvailability != BluetoothHelper.BluetoothAvailability.READY) {
                    if (bluetoothAvailability == BluetoothHelper.BluetoothAvailability.OFF) {
                        BluetoothHelper.showBluetoothOffNotification(context)
                    } else {
                        BluetoothHelper.toStatusMessageRes(bluetoothAvailability)?.let { resId ->
                            Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
                        }
                    }
                    return
                }
                // Start all in the group
                spammerTypes.forEach { SpamService.startSpammer(context, it) }
            }
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, SpammerWidgetProvider::class.java)
            )
            onUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    companion object {
        const val ACTION_TOGGLE_APPLE = "com.tutozz.blespam.widget.TOGGLE_APPLE"
        const val ACTION_TOGGLE_ANDROID = "com.tutozz.blespam.widget.TOGGLE_ANDROID"
        const val ACTION_TOGGLE_WINDOWS = "com.tutozz.blespam.widget.TOGGLE_WINDOWS"

        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_spammer)

            setupButton(
                context, views,
                R.id.widget_btn_apple, R.id.widget_icon_apple,
                listOf("iOS Crash", "Apple Action Modal", "Apple Device Popup", "Apple 'Not Your Device'"),
                R.drawable.ic_apple, R.drawable.ic_apple_active, ACTION_TOGGLE_APPLE
            )
            setupButton(
                context, views,
                R.id.widget_btn_android, R.id.widget_icon_android,
                listOf("Android Fast Pair", "Samsung Buds", "Samsung Watch", "Xiaomi Quick Connect"),
                R.drawable.ic_android, R.drawable.ic_android_active, ACTION_TOGGLE_ANDROID
            )
            setupButton(
                context, views,
                R.id.widget_btn_windows, R.id.widget_icon_windows,
                listOf("Windows Swift Pair"),
                R.drawable.ic_windows, R.drawable.ic_windows_active, ACTION_TOGGLE_WINDOWS
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun setupButton(
            context: Context,
            views: RemoteViews,
            buttonId: Int,
            iconId: Int,
            spammerTypes: List<String>,
            defaultIconRes: Int,
            activeIconRes: Int,
            action: String
        ) {
            val isRunning = spammerTypes.any { SpamService.isSpammerRunning(it) }

            if (isRunning) {
                views.setImageViewResource(iconId, activeIconRes)
            } else {
                views.setImageViewResource(iconId, defaultIconRes)
            }

            val intent = Intent(context, SpammerWidgetProvider::class.java).apply {
                this.action = action
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                action.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            views.setOnClickPendingIntent(buttonId, pendingIntent)
        }
    }
}
