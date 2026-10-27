package com.example.ai

import android.util.Log
import com.example.data.model.AiRole
import com.example.data.model.GeminiModel
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiChatService"

data class ChatTurn(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: String,
    val isStreaming: Boolean = false
)

class GeminiChatService {

    // Cache generative models by role and model name
    private val models = mutableMapOf<String, GenerativeModel>()

    private fun getModel(role: AiRole, model: GeminiModel): GenerativeModel {
        val key = "${role.id}_${model.modelName}"
        return models.getOrPut(key) {
            Firebase.ai.generativeModel(
                modelName = model.modelName,
                systemInstruction = content {
                    text(role.systemInstruction)
                }
            )
        }
    }

    /**
     * Sends a message in a multi-turn chat stream.
     * Yields incremental chunks as they arrive from Gemini API.
     */
    fun sendMessageStream(
        role: AiRole,
        model: GeminiModel,
        history: List<ChatTurn>,
        userMessage: String
    ): Flow<String> = flow {
        val generativeModel = getModel(role, model)

        // Build multi-turn history contents
        val historyContents = history.mapNotNull { turn ->
            when (turn.role) {
                "user" -> content("user") { text(turn.text) }
                "model" -> content("model") { text(turn.text) }
                else -> null
            }
        }

        try {
            val chat = generativeModel.startChat(history = historyContents)
            withTimeout(60_000L) {
                chat.sendMessageStream(userMessage).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        emit(chunkText)
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini inference timed out", e)
            emit("\n[Response timed out after 60 seconds. Please try again.]")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation failed: ${e.message}", e)
            emit("\n[Gemini Error: ${e.localizedMessage ?: "Unable to complete request"}]")
        }
    }

    /**
     * Rapid single-prompt helper using Gemini 3.5 Flash-Lite (e.g. for smart replies or quick tone polish)
     */
    suspend fun generateQuickReply(prompt: String): String {
        return try {
            val model = Firebase.ai.generativeModel("gemini-3.5-flash-lite")
            withTimeout(30_000L) {
                val response = model.generateContent(prompt)
                response.text?.trim() ?: "Sounds good!"
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Quick reply fallback: ${e.message}")
            "Got it!"
        }
    }
}
