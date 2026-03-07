package com.tutozz.blespam

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object BluetoothHelper {
    private const val BLUETOOTH_ALERT_CHANNEL_ID = "BluetoothAlertChannel"
    private const val BLUETOOTH_ALERT_NOTIFICATION_ID = 1101

    enum class BluetoothAvailability {
        READY,
        OFF,
        UNSUPPORTED,
        PERMISSION_DENIED
    }

    val bluetoothAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    fun availability(context: Context): BluetoothAvailability {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED
        ) {
            return BluetoothAvailability.PERMISSION_DENIED
        }

        val adapter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
            bluetoothManager?.adapter
        } else {
            @Suppress("DEPRECATION")
            BluetoothAdapter.getDefaultAdapter()
        }

        if (adapter == null) {
            return BluetoothAvailability.UNSUPPORTED
        }

        return try {
            if (adapter.isEnabled) {
                BluetoothAvailability.READY
            } else {
                BluetoothAvailability.OFF
            }
        } catch (_: SecurityException) {
            BluetoothAvailability.PERMISSION_DENIED
        }
    }

    @StringRes
    fun toStatusMessageRes(status: BluetoothAvailability): Int? {
        return when (status) {
            BluetoothAvailability.OFF -> R.string.bluetooth_enable_notification
            BluetoothAvailability.UNSUPPORTED -> R.string.bluetooth_not_supported
            BluetoothAvailability.PERMISSION_DENIED -> R.string.bluetooth_permission_denied
            BluetoothAvailability.READY -> null
        }
    }

    fun showBluetoothOffNotification(context: Context) {
        ensureAlertChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, BLUETOOTH_ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.bluetooth_enable_notification))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(context, context.getString(R.string.bluetooth_enable_notification), Toast.LENGTH_SHORT).show()
            return
        }

        NotificationManagerCompat.from(context).notify(BLUETOOTH_ALERT_NOTIFICATION_ID, notification)
    }

    private fun ensureAlertChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(BLUETOOTH_ALERT_CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            BLUETOOTH_ALERT_CHANNEL_ID,
            context.getString(R.string.ble_spam_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.ble_spam_channel_description)
        }

        manager.createNotificationChannel(channel)
    }
}
