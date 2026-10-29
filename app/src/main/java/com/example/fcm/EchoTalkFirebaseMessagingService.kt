package com.example.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.EchoTalkRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * EchoTalkFirebaseMessagingService: Handles Firebase Cloud Messaging (FCM) events
 * for incoming chat messages and token refreshes.
 */
class EchoTalkFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val TAG = "EchoTalkFCM"
        const val CHANNEL_ID = "echotalk_messages_channel"
        const val CHANNEL_NAME = "EchoTalk Chat Messages"
        const val CHANNEL_DESCRIPTION = "Real-time push notifications for incoming chat messages and 1v1 challenges"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 150, 100, 200)
                    setShowBadge(true)
                }

                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created: $CHANNEL_ID")
            }
        }

        fun showNotification(
            context: Context,
            title: String,
            message: String,
            senderId: String? = null
        ) {
            createNotificationChannel(context)

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("ACTIVE_PARTNER_ID", senderId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                (System.currentTimeMillis() % 10000).toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 150, 100, 200))
                .build()

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val notificationId = (System.currentTimeMillis() % 100000).toInt()
            notificationManager?.notify(notificationId, notification)
            Log.d(TAG, "Notification posted: $title - $message")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "New FCM Registration Token: $token")
        EchoTalkRepository.getInstance().setFcmToken(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "FCM Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "New Message on EchoTalk"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["text"]
            ?: "You have a new message waiting."

        val senderId = remoteMessage.data["sender_id"] ?: "u-unknown"
        val msgId = remoteMessage.data["message_id"] ?: "fcm-${System.currentTimeMillis()}"
        val chatMsg = com.example.data.model.ChatMessage(
            id = msgId,
            sender = title,
            text = body,
            timestamp = "Just now",
            status = "seen"
        )
        EchoTalkRepository.getInstance().cacheIncomingMessage(senderId, chatMsg)

        showNotification(applicationContext, title, body, senderId)
    }
}
