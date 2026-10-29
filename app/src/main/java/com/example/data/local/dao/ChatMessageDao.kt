package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LocalChatMessageEntity
import kotlinx.coroutines.flow.Flow

/**
 * ChatMessageDao: Data Access Object for local chat message caching,
 * providing reactive Flow queries and offline-first message persistence.
 */
@Dao
interface ChatMessageDao {

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    fun getMessagesForConversationFlow(conversationId: String): Flow<List<LocalChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    suspend fun getMessagesForConversation(conversationId: String): List<LocalChatMessageEntity>

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    fun getAllMessagesFlow(): Flow<List<LocalChatMessageEntity>>

    @Query("SELECT * FROM chat_messages ORDER BY timestampMillis ASC")
    suspend fun getAllMessages(): List<LocalChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: String): LocalChatMessageEntity?

    @Query("SELECT COUNT(*) FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun getMessageCount(conversationId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: LocalChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<LocalChatMessageEntity>)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE chat_messages SET viewed = 1 WHERE id = :messageId")
    suspend fun markViewOnceBurned(messageId: String)

    @Query("DELETE FROM chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteConversation(conversationId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAllMessages()
}
