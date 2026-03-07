package com.tutozz.blespam

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        Log.d("BLESpam", "FCM Message from: ${message.from}")

        if (!NotificationAudienceHelper.shouldDeliverToCurrentUser(this, message.data)) {
            Log.d("BLESpam", "Notification filtered out by audience rules: ${message.data}")
            return
        }

        if (message.data.isNotEmpty()) {
            Log.d("BLESpam", "Data payload: ${message.data}")

            val title = message.data["title"]
            val body = message.data["message"] ?: message.data["body"]

            showNotification(title, body, message.data)
            return
        }

        message.notification?.let {
            showNotification(it.title, it.body, message.data)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("BLESpam", "New FCM token: $token")
        val sharedPref = getSharedPreferences("AppSettings", Context.MODE_PRIVATE)
        sharedPref.edit().putString("fcm_token", token).apply()
        updateTokenInFirebase(token)
    }

    private fun updateTokenInFirebase(token: String) {
        val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        val database = FirebaseDatabase.getInstance()
        val tokensRef = database.getReference("fcm_tokens")
        val language = NotificationAudienceHelper.getSelectedLanguage(this)
        val country = NotificationAudienceHelper.getSelectedCountry(this)

        val tokenData = mapOf(
            "token" to token,
            "device_id" to deviceId,
            "timestamp" to System.currentTimeMillis(),
            "language" to language,
            "country" to country
        )

        tokensRef.child(deviceId).setValue(tokenData)
            .addOnSuccessListener {
                Log.d("BLESpam", "Token updated in Firebase Database")
            }
            .addOnFailureListener { e ->
                Log.e("BLESpam", "Failed to update token", e)
            }
    }

    private fun showNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = "ble_spam_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(title ?: "BLE Spam")
            .setContentText(messageBody ?: "")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BLE Spam Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Уведомления BLE Spam"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
