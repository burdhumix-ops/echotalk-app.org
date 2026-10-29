package com.example.data.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.example.data.local.dao.PendingQueueDao
import com.example.data.local.entity.PendingQueueEntity
import com.example.data.model.ChatMessage
import com.example.di.DatabaseModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * OfflineQueueManager: Uses Room database to persist outgoing messages and updates
 * while offline, and automatically flushes them once connectivity is restored.
 */
class OfflineQueueManager private constructor(
    private val context: Context,
    private val dao: PendingQueueDao
) {
    companion object {
        private const val TAG = "OfflineQueueManager"

        @Volatile
        private var instance: OfflineQueueManager? = null

        fun getInstance(context: Context): OfflineQueueManager {
            return instance ?: synchronized(this) {
                instance ?: run {
                    val db = AppRoomDatabase.getInstance(context)
                    OfflineQueueManager(context.applicationContext, db.pendingQueueDao()).also {
                        instance = it
                    }
                }
            }
        }

        fun createForTesting(context: Context, dao: PendingQueueDao): OfflineQueueManager {
            return OfflineQueueManager(context, dao)
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val _isConnected = MutableStateFlow(checkInitialConnectivity())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _isFlushing = MutableStateFlow(false)
    val isFlushing: StateFlow<Boolean> = _isFlushing.asStateFlow()

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    val pendingTasks: Flow<List<PendingQueueEntity>> = dao.getAllPendingFlow()

    init {
        registerNetworkCallback()
    }

    private fun checkInitialConnectivity(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val activeNetwork = cm?.activeNetwork
            val caps = cm?.getNetworkCapabilities(activeNetwork)
            caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        } catch (_: Exception) {
            true
        }
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager?.registerNetworkCallback(
                request,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.i(TAG, "Network connection restored. Auto-flushing pending queue...")
                        _isConnected.value = true
                        scope.launch {
                            flushQueue()
                        }
                    }

                    override fun onLost(network: Network) {
                        Log.w(TAG, "Network connection lost. Offline queuing active.")
                        _isConnected.value = false
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register NetworkCallback: ${e.message}")
        }
    }

    /**
     * Enqueue a pending chat message into Room database.
     */
    suspend fun enqueueMessage(
        recipientId: String,
        senderName: String,
        text: String,
        isViewOnce: Boolean = false,
        isDoodle: Boolean = false,
        mediaUrl: String? = null,
        isAudio: Boolean = false,
        audioDuration: String? = null
    ): PendingQueueEntity {
        val entity = PendingQueueEntity(
            id = "offline-msg-${UUID.randomUUID()}",
            taskType = "MESSAGE",
            recipientId = recipientId,
            senderName = senderName,
            text = text,
            timestamp = System.currentTimeMillis(),
            isViewOnce = isViewOnce,
            isDoodle = isDoodle,
            mediaUrl = mediaUrl,
            isAudio = isAudio,
            audioDuration = audioDuration,
            status = "PENDING"
        )
        dao.insert(entity)
        Log.d(TAG, "Enqueued pending message in Room: ${entity.id}")

        // If online, immediately trigger background flush
        if (_isConnected.value) {
            scope.launch { flushQueue() }
        }

        return entity
    }

    /**
     * Flush all pending tasks in the Room queue.
     * Returns the count of successfully dispatched tasks.
     */
    suspend fun flushQueue(onMessageDelivered: ((String, ChatMessage) -> Unit)? = null): Int {
        if (_isFlushing.value) return 0
        _isFlushing.value = true

        var dispatchedCount = 0
        try {
            val pendingList = dao.getAllPendingList()
            Log.d(TAG, "Flushing ${pendingList.size} pending tasks from Room...")

            for (task in pendingList) {
                try {
                    dao.updateStatus(task.id, "PROCESSING")

                    val chatMessage = ChatMessage(
                        id = task.id,
                        sender = task.senderName,
                        text = task.text,
                        timestamp = timeFormat.format(Date(task.timestamp)),
                        status = "delivered",
                        isViewOnce = task.isViewOnce,
                        isDoodle = task.isDoodle,
                        mediaUrl = task.mediaUrl,
                        isAudio = task.isAudio,
                        audioDuration = task.audioDuration
                    )

                    // Dispatch to Firestore if cloud persistence is available
                    try {
                        DatabaseModule.saveDirectMessage(task.senderName, task.recipientId, chatMessage)
                    } catch (e: Exception) {
                        Log.d(TAG, "Local/Firestore dispatch notice: ${e.message}")
                    }

                    // Notify callback if provided
                    onMessageDelivered?.invoke(task.recipientId, chatMessage)

                    // Remove successfully synced task from Room
                    dao.deleteById(task.id)
                    dispatchedCount++
                    Log.d(TAG, "Synced and removed pending task: ${task.id}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to flush task ${task.id}: ${e.message}")
                    dao.updateStatus(task.id, "FAILED")
                }
            }
        } finally {
            _isFlushing.value = false
        }

        return dispatchedCount
    }

    suspend fun clearQueue() {
        dao.clearAll()
    }

    suspend fun getPendingCount(): Int {
        return dao.getAllPendingList().size
    }

    fun setSimulatedConnected(connected: Boolean) {
        _isConnected.value = connected
        if (connected) {
            scope.launch { flushQueue() }
        }
    }
}
