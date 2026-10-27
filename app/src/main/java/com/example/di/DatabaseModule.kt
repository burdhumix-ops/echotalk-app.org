package com.example.di

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.ChatMessage
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * DatabaseModule manages the Firebase Firestore client instance and provides
 * real-time chat data storage and observation APIs for EchoTalk.
 */
object DatabaseModule {

    private const val TAG = "DatabaseModule"

    @Volatile
    private var firestoreInstance: FirebaseFirestore? = null

    /**
     * Initializes and provides the FirebaseFirestore client.
     * Honors the custom database ID from R.string.firestore_database_id if defined.
     */
    fun provideFirestore(context: Context): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: run {
                val appContext = context.applicationContext
                val dbId = try {
                    appContext.getString(R.string.firestore_database_id).trim()
                } catch (e: Exception) {
                    Log.w(TAG, "firestore_database_id resource not found, using default: ${e.message}")
                    ""
                }

                val instance = if (dbId.isNotEmpty() && dbId != "(default)") {
                    Log.d(TAG, "Initializing Firestore with custom databaseId: $dbId")
                    FirebaseFirestore.getInstance(dbId)
                } else {
                    Log.d(TAG, "Initializing Firestore with default database")
                    FirebaseFirestore.getInstance()
                }

                try {
                    val settings = FirebaseFirestoreSettings.Builder()
                        .setPersistenceEnabled(true)
                        .build()
                    instance.firestoreSettings = settings
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to apply firestoreSettings (may already be initialized): ${e.message}")
                }

                instance.also { firestoreInstance = it }
            }
        }
    }

    /**
     * Direct factory method to obtain Firestore for an explicit databaseId.
     */
    fun provideFirestore(databaseId: String): FirebaseFirestore {
        return if (databaseId.isNotEmpty() && databaseId != "(default)") {
            FirebaseFirestore.getInstance(databaseId)
        } else {
            FirebaseFirestore.getInstance()
        }
    }

    /**
     * Retrieves the singleton Firestore instance. Initializes the default instance if not already set.
     */
    fun getInstance(): FirebaseFirestore {
        return firestoreInstance ?: synchronized(this) {
            firestoreInstance ?: FirebaseFirestore.getInstance().also { firestoreInstance = it }
        }
    }

    /**
     * Internal setter for unit testing and mocks.
     */
    fun setTestInstance(instance: FirebaseFirestore?) {
        firestoreInstance = instance
    }

    // =========================================================================
    // Real-Time Chat Storage Collection References & Helpers
    // =========================================================================

    /**
     * Returns the Firestore collection reference for messages in a given audio/text chat room.
     * Path: rooms/{roomId}/messages
     */
    fun getRoomMessagesCollection(roomId: String): CollectionReference {
        return getInstance().collection("rooms").document(roomId).collection("messages")
    }

    /**
     * Generates a deterministic chat ID for a 1v1 direct message conversation between two users.
     */
    fun getConversationId(userId1: String, userId2: String): String {
        return listOf(userId1, userId2).sorted().joinToString("_")
    }

    /**
     * Returns the Firestore collection reference for direct messages between two users.
     * Path: direct_chats/{conversationId}/messages
     */
    fun getDirectMessagesCollection(userId1: String, userId2: String): CollectionReference {
        val convId = getConversationId(userId1, userId2)
        return getInstance().collection("direct_chats").document(convId).collection("messages")
    }

    /**
     * Asynchronously writes a room message to Firestore.
     */
    fun saveRoomMessage(
        roomId: String,
        message: ChatMessage,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val docId = if (message.id.isNotBlank()) message.id else "rm-${System.currentTimeMillis()}"
        val finalMsg = if (message.id.isBlank()) message.copy(id = docId) else message

        getRoomMessagesCollection(roomId).document(docId).set(finalMsg)
            .addOnSuccessListener {
                Log.d(TAG, "Saved room message $docId to Firestore")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save room message $docId to Firestore", exception)
                onFailure?.invoke(exception)
            }
    }

    /**
     * Asynchronously writes a direct message to Firestore.
     */
    fun saveDirectMessage(
        senderId: String,
        recipientId: String,
        message: ChatMessage,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        val docId = if (message.id.isNotBlank()) message.id else "dm-${System.currentTimeMillis()}"
        val finalMsg = if (message.id.isBlank()) message.copy(id = docId) else message

        getDirectMessagesCollection(senderId, recipientId).document(docId).set(finalMsg)
            .addOnSuccessListener {
                Log.d(TAG, "Saved DM $docId to Firestore")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to save DM $docId to Firestore", exception)
                onFailure?.invoke(exception)
            }
    }

    /**
     * Observes real-time messages for a room as a Kotlin Coroutine Flow.
     */
    fun observeRoomMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listenerRegistration = getRoomMessagesCollection(roomId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to room messages: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatMessage::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error deserializing ChatMessage from ${doc.id}", e)
                            null
                        }
                    }
                    trySend(messages)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    /**
     * Observes real-time direct messages between two users as a Kotlin Coroutine Flow.
     */
    fun observeDirectMessages(userId1: String, userId2: String): Flow<List<ChatMessage>> = callbackFlow {
        val listenerRegistration = getDirectMessagesCollection(userId1, userId2)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error listening to direct messages: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toObject(ChatMessage::class.java)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error deserializing DM from ${doc.id}", e)
                            null
                        }
                    }
                    trySend(messages)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }

    // =========================================================================
    // Real-Time Arcade Multiplayer Synchronization (Chess & Ludo)
    // =========================================================================

    /**
     * Returns the Firestore collection reference for arcade games.
     * Path: arcade_games/{gameId}
     */
    fun getArcadeGamesCollection(): CollectionReference {
        return getInstance().collection("arcade_games")
    }

    /**
     * Synchronizes arcade game state (board, turns, moves) to Firestore.
     */
    fun saveArcadeGameState(
        gameState: com.example.data.model.ArcadeGameState,
        onSuccess: (() -> Unit)? = null,
        onFailure: ((Exception) -> Unit)? = null
    ) {
        if (gameState.gameId.isBlank()) return
        getArcadeGamesCollection().document(gameState.gameId).set(gameState)
            .addOnSuccessListener {
                Log.d(TAG, "Synced arcade game state for ${gameState.gameId}")
                onSuccess?.invoke()
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to sync arcade game ${gameState.gameId}", exception)
                onFailure?.invoke(exception)
            }
    }

    /**
     * Observes real-time multiplayer moves and state changes for an arcade game.
     */
    fun observeArcadeGameState(gameId: String): Flow<com.example.data.model.ArcadeGameState?> = callbackFlow {
        if (gameId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }
        val listenerRegistration = getArcadeGamesCollection().document(gameId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error observing game $gameId: ${error.message}", error)
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    try {
                        val state = snapshot.toObject(com.example.data.model.ArcadeGameState::class.java)
                        trySend(state)
                    } catch (e: Exception) {
                        Log.w(TAG, "Error deserializing ArcadeGameState for $gameId", e)
                    }
                } else {
                    trySend(null)
                }
            }

        awaitClose {
            listenerRegistration.remove()
        }
    }
}
