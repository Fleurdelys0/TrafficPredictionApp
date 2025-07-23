package com.example.trafficprediction

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // We check if the message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
            // Here we would handle the data message.
            // For example, for a traffic alert:
            // val title = remoteMessage.data["title"]
            // val body = remoteMessage.data["body"]
            // if (title != null && body != null) {
            //     sendNotification(title, body, "traffic_alerts_channel")
            // }
        }

        // We check if the message contains a notification payload.
        // Notifications sent from the FCM console usually arrive here.
        // If we're sending data messages from a Cloud Function, the data block above is more relevant.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            it.body?.let { body ->
                val title = it.title ?: getString(R.string.app_name)
                // We need to decide which channel to use by default or look for channel info in the message.
                sendNotification(title, body, "traffic_alerts_channel") // Example channel
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d("MyFCMService", "Refreshed token: $token")
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        // TODO: We need to implement this method to send the token to our app server.
        Log.d(TAG, "sendRegistrationTokenToServer($token)")
    }

    private fun sendNotification(title: String, messageBody: String, channelId: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // We use the app icon here.
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // A notification channel is required for Android Oreo and above.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // We assume the channel is already created in MainApplication,
            // but we can add a check/creation here as well (it should be idempotent).
            // For example, if it wasn't created in MainApplication or just to be sure:
            var channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                val defaultChannelName =
                    if (channelId == "weather_alerts_channel") "Weather Alerts" else "Traffic Alerts"
                val defaultChannelDescription =
                    if (channelId == "weather_alerts_channel") "Notifications for sudden weather changes" else "Notifications for traffic on your favorite routes"
                channel = NotificationChannel(
                    channelId,
                    defaultChannelName,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = defaultChannelDescription
                }
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "$defaultChannelName channel created.")
            } else {
                Log.d(TAG, "${channel.name} channel already exists.")
            }
        }

        val notificationId = System.currentTimeMillis().toInt() // We generate a unique ID for each notification.
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Notification sent: ID $notificationId, Title: $title")
    }
}
