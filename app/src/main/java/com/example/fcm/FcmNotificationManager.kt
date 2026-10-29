package com.example.fcm

import android.content.Context
import android.util.Log
import com.example.data.repository.EchoTalkRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FcmNotificationManager: Manages FCM registration, push notifications simulation,
 * and user token status.
 */
object FcmNotificationManager {

    private const val TAG = "FcmNotificationManager"
    private val _fcmToken = MutableStateFlow<String?>("fcm_token_${System.currentTimeMillis()}")
    val fcmToken: StateFlow<String?> = _fcmToken.asStateFlow()

    fun initialize(context: Context) {
        EchoTalkFirebaseMessagingService.createNotificationChannel(context)

        try {
            FirebaseMessaging.getInstance().token
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val token = task.result
                        _fcmToken.value = token
                        EchoTalkRepository.getInstance().setFcmToken(token)
                        Log.i(TAG, "Fetched FCM Token: $token")
                    } else {
                        Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    }
                }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseMessaging not initialized in current environment: ${e.message}")
        }
    }

    /**
     * Sends a local notification to simulate an incoming push message.
     */
    fun simulateIncomingPush(
        context: Context,
        senderName: String,
        message: String,
        senderId: String? = null
    ) {
        EchoTalkFirebaseMessagingService.showNotification(
            context = context,
            title = "New message from $senderName",
            message = message,
            senderId = senderId
        )
    }
}
