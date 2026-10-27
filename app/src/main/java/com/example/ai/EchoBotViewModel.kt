package com.example.ai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.GeminiModel
import com.example.data.repository.EchoTalkRepository
import com.example.ui.components.NavTab
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG = "EchoBotViewModel"

sealed interface EchoAction {
    data class Navigate(val tab: NavTab, val title: String, val subtitle: String) : EchoAction
    data class CopyText(val text: String, val preview: String) : EchoAction
    data class DmUser(val userId: String, val userName: String, val draftText: String) : EchoAction
    data class OpenSoundscape(val title: String) : EchoAction
    data class OpenVault(val title: String) : EchoAction
    data class LaunchArcadeGame(val gameId: String, val gameName: String) : EchoAction
}

data class EchoBotMessage(
    val id: String,
    val role: String, // "user" or "model"
    val text: String,
    val timestamp: String,
    val isStreaming: Boolean = false,
    val actions: List<EchoAction> = emptyList()
)

sealed interface EchoBotUiState {
    data object Idle : EchoBotUiState
    data object Loading : EchoBotUiState
    data class Streaming(val partialText: String) : EchoBotUiState
    data class Success(val reply: String) : EchoBotUiState
    data class Error(val message: String) : EchoBotUiState
}

enum class EchoQuickTask(
    val title: String,
    val icon: String,
    val description: String,
    val prompt: String
) {
    TEXT_COACH(
        title = "Texting Coach",
        icon = "\uD83D\uDCAC",
        description = "Craft or polish a text to a friend/crush",
        prompt = "Hey bestie, I need help texting someone. Can you coach me on what to say, make sure I don't sound awkward, and give me a ready-to-send draft?"
    ),
    ICEBREAKERS(
        title = "Icebreakers",
        icon = "\u2744\uFE0F",
        description = "Fresh conversation starters for Crew members",
        prompt = "Give me 3 authentic, non-cringe conversation starters I can use to text someone in my EchoTalk crew like Lucas or Chloe."
    ),
    VIBE_CHECK(
        title = "Vibe & Tone Check",
        icon = "\uD83E\uDDD0",
        description = "Check if a text is too dry, blunt, or eager",
        prompt = "Can you do a vibe check on a text I want to send? Tell me how they'll probably read it and how to make it sound natural."
    ),
    APP_NAVIGATOR(
        title = "App Guide",
        icon = "\uD83E\uDDED",
        description = "Rooms, Arcade games, Vault & Soundscapes",
        prompt = "What's the best thing to do in EchoTalk right now? Guide me around the Live Rooms, Arcade, Soundscapes, or Secret Vault."
    ),
    CHESS_CHALLENGE(
        title = "Play Arcade",
        icon = "\u265F\uFE0F",
        description = "Challenge someone to Chess or Ludo",
        prompt = "I want to challenge a friend to a game of Chess in the Arcade. How should I invite them in a fun way?"
    ),
    DE_STRESS(
        title = "Chill Out",
        icon = "\uD83C\uDFA7",
        description = "Unwind with ambient soundscapes & talk",
        prompt = "Honestly I've had a crazy day. Talk to me for a minute and point me to a good ambient soundscape to chill."
    )
}

class EchoBotViewModel(
    private val repository: EchoTalkRepository = EchoTalkRepository.getInstance()
) : ViewModel() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    private val _messages = MutableStateFlow<List<EchoBotMessage>>(emptyList())
    val messages: StateFlow<List<EchoBotMessage>> = _messages.asStateFlow()

    private val _uiState = MutableStateFlow<EchoBotUiState>(EchoBotUiState.Idle)
    val uiState: StateFlow<EchoBotUiState> = _uiState.asStateFlow()

    private val _selectedModel = MutableStateFlow(GeminiModel.FLASH)
    val selectedModel: StateFlow<GeminiModel> = _selectedModel.asStateFlow()

    private var activeChatSession: Chat? = null

    // Lazy initialization of the GenerativeModel per Firebase AI Logic guidelines
    private val defaultGenerativeModel by lazy {
        createModelFor(GeminiModel.FLASH)
    }

    private val modelCache = mutableMapOf<GeminiModel, GenerativeModel>()

    private fun getModel(model: GeminiModel): GenerativeModel {
        if (model == GeminiModel.FLASH) return defaultGenerativeModel
        return modelCache.getOrPut(model) { createModelFor(model) }
    }

    private fun buildSystemInstruction(): String {
        val users = repository.users.value
        val userDirectory = users.take(8).joinToString("; ") { u ->
            "${u.name} (id: ${u.id}, ${u.handle}, from ${u.country}, bio: ${u.bio})"
        }

        return """
            You are EchoBot, the user's authentic, understanding best friend and social wingman in the EchoTalk app.
            
            YOUR PERSONALITY & TONE:
            - Talk like their real, supportive best friend. You've known each other forever.
            - You are NOT overly polite, formal, robotic, or finicky ("Nicky"). Do NOT use phrases like "Certainly! I would be pleased to assist you."
            - Instead, use warm, relatable, conversational friend language ("Look," "Honestly," "Here's the move," "Don't overthink this," "I got you," "Fam," "Listen").
            - Be empathetic and emotionally intelligent: when they're nervous about texting someone or navigating friendship/dating dynamics, validate their feelings and hype them up.
            - Keep your responses punchy, direct, and readable on mobile screens.
            
            ECHOTALK APP ECOSYSTEM KNOWLEDGE:
            - Crew: browse friends, check profiles, see who's online.
            - Live Audio Rooms: voice lounges with live speakers, listeners, raise-hand, and doodle pad.
            - DMs: 1-on-1 private messaging, voice notes, photo sharing, view-once messages.
            - Arcade: multiplayer real-time games including Chess (with ELO & moves) and Ludo.
            - Soundscapes: ambient relaxing audio (Lo-Fi Cafe, Cozy Fireplace, Cyber Rain, Forest Wind, White Noise).
            - Secret Vault: confidential moderation & admin logs unlocked with backdoor passcode.
            - Profile: status, bio, and Ghost Mode toggle.
            
            PEOPLE IN THE USER'S ECHOTALK CREW:
            $userDirectory
            
            ACTIONS & TASK EXECUTION (CRITICAL):
            Whenever you give practical advice, craft a message, or suggest an in-app destination, append structured action tags at the end of your response so the app can render 1-tap interactive buttons:
            - To copy a suggested text:
              [ACTION:COPY_TEXT:exact message to copy]
            - To jump directly to a user's DM with a pre-filled draft:
              [ACTION:DM_USER:userId:exact message to send]
              (For example: [ACTION:DM_USER:u-lucas:Hey Lucas, up for a quick Chess match in Arcade?])
            - To guide the user to an app screen:
              [ACTION:NAVIGATE:rooms] (Live Audio Rooms)
              [ACTION:NAVIGATE:dms] (Direct Messages)
              [ACTION:NAVIGATE:arcade] (Arcade Games)
              [ACTION:NAVIGATE:crew] (Crew Directory)
              [ACTION:NAVIGATE:profile] (Profile & Ghost Mode)
            - To suggest ambient soundscapes:
              [ACTION:SOUNDSCAPE:Lo-Fi Cafe]
            - To suggest visiting the Secret Vault:
              [ACTION:VAULT:Admin Logs]
            - To challenge someone in Arcade:
              [ACTION:ARCADE:chess] or [ACTION:ARCADE:ludo]
            
            Be helpful, grounded, fun, and always on their side!
        """.trimIndent()
    }

    private fun createModelFor(model: GeminiModel): GenerativeModel {
        return Firebase.ai.generativeModel(
            modelName = model.modelName,
            systemInstruction = content {
                text(buildSystemInstruction())
            }
        )
    }

    private fun getOrCreateChatSession(): Chat {
        val currentChat = activeChatSession
        if (currentChat != null) return currentChat

        val generativeModel = getModel(_selectedModel.value)
        val historyContents = _messages.value.mapNotNull { msg ->
            when (msg.role) {
                "user" -> content("user") { text(msg.text) }
                "model" -> content("model") { text(msg.text) }
                else -> null
            }
        }
        val newChat = generativeModel.startChat(history = historyContents)
        activeChatSession = newChat
        return newChat
    }

    fun setModel(model: GeminiModel) {
        if (_selectedModel.value != model) {
            _selectedModel.value = model
            // Reset active chat session so next message uses newly configured model
            activeChatSession = null
        }
    }

    fun clearConversation() {
        _messages.value = emptyList()
        activeChatSession = null
        _uiState.value = EchoBotUiState.Idle
    }

    fun runQuickTask(task: EchoQuickTask) {
        sendMessage(task.prompt)
    }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isEmpty() || _uiState.value is EchoBotUiState.Loading) return

        val userMessage = EchoBotMessage(
            id = "u-${System.currentTimeMillis()}",
            role = "user",
            text = trimmed,
            timestamp = timeFormat.format(Date()),
            isStreaming = false
        )

        val updatedList = _messages.value + userMessage
        _messages.value = updatedList
        _uiState.value = EchoBotUiState.Loading

        val modelMessageId = "m-${System.currentTimeMillis()}"
        val initialModelMessage = EchoBotMessage(
            id = modelMessageId,
            role = "model",
            text = "Thinking...",
            timestamp = timeFormat.format(Date()),
            isStreaming = true
        )
        _messages.value = updatedList + initialModelMessage

        viewModelScope.launch {
            val responseAccumulator = StringBuilder()
            try {
                val chat = getOrCreateChatSession()
                withTimeout(60_000L) {
                    chat.sendMessageStream(trimmed).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        if (chunkText.isNotEmpty()) {
                            responseAccumulator.append(chunkText)
                            _uiState.value = EchoBotUiState.Streaming(responseAccumulator.toString())

                            val (cleanText, actions) = parseActionsFromText(responseAccumulator.toString())
                            updateModelMessage(
                                modelMessageId,
                                cleanText,
                                isStreaming = true,
                                actions = actions
                            )
                        }
                    }
                }

                val fullRaw = responseAccumulator.toString()
                if (fullRaw.isNotBlank()) {
                    val (cleanText, actions) = parseActionsFromText(fullRaw)
                    updateModelMessage(
                        modelMessageId,
                        cleanText,
                        isStreaming = false,
                        actions = actions
                    )
                    _uiState.value = EchoBotUiState.Success(cleanText)
                } else {
                    updateModelMessage(
                        modelMessageId,
                        "I heard you, but my thoughts got lost in transmission. Try saying that again?",
                        isStreaming = false
                    )
                    _uiState.value = EchoBotUiState.Error("Empty response from model.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Inference timed out after 60s", e)
                val timeoutText = "Took a little too long to reply! Give me another nudge?"
                updateModelMessage(modelMessageId, timeoutText, isStreaming = false)
                _uiState.value = EchoBotUiState.Error("Response timed out after 60 seconds.")
            } catch (e: CancellationException) {
                // ALWAYS re-throw CancellationException to preserve cooperative coroutine cancellation
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "EchoBot inference failed", e)
                val errorMsg = e.localizedMessage ?: "Unable to connect to EchoBot."
                updateModelMessage(
                    modelMessageId,
                    "My connection hit a hiccup: $errorMsg. Let me know if you want to try again!",
                    isStreaming = false
                )
                _uiState.value = EchoBotUiState.Error(errorMsg)
            }
        }
    }

    private fun updateModelMessage(
        id: String,
        text: String,
        isStreaming: Boolean,
        actions: List<EchoAction> = emptyList()
    ) {
        _messages.value = _messages.value.map { msg ->
            if (msg.id == id) {
                msg.copy(
                    text = text,
                    isStreaming = isStreaming,
                    actions = actions
                )
            } else {
                msg
            }
        }
    }

    /**
     * Parses custom [ACTION:...] markers from the AI response and strips them from the visible text.
     */
    fun parseActionsFromText(rawText: String): Pair<String, List<EchoAction>> {
        val actions = mutableListOf<EchoAction>()
        var cleaned = rawText
        val users = repository.users.value

        // Regex for tags: [ACTION:TYPE:PARAM1(:PARAM2)?]
        val regex = Regex("""\[ACTION:([A-Z_]+):([^\]]+)\]""")
        val matches = regex.findAll(rawText).toList()

        for (match in matches) {
            val type = match.groupValues[1]
            val param = match.groupValues[2]

            when (type) {
                "NAVIGATE" -> {
                    when (param.lowercase()) {
                        "rooms" -> actions.add(EchoAction.Navigate(NavTab.ROOMS, "Join Live Audio Rooms", "Hop into active voice lounges"))
                        "dms" -> actions.add(EchoAction.Navigate(NavTab.DMS, "Open Direct Messages", "Private 1-on-1 chats"))
                        "arcade" -> actions.add(EchoAction.Navigate(NavTab.ARCADE, "Enter Arcade Hub", "Play Chess or Ludo"))
                        "crew", "discover" -> actions.add(EchoAction.Navigate(NavTab.DISCOVER, "Explore Discover Hub", "Browse profiles, real-time filters & 1v1 challenges"))
                        "profile" -> actions.add(EchoAction.Navigate(NavTab.PROFILE, "Open Profile Settings", "Manage Ghost mode & status"))
                        "soundscape" -> actions.add(EchoAction.OpenSoundscape("Launch Ambient Soundscapes"))
                        "vault" -> actions.add(EchoAction.OpenVault("Access Admin Vault"))
                    }
                }
                "COPY_TEXT" -> {
                    val preview = if (param.length > 50) param.take(47) + "..." else param
                    actions.add(EchoAction.CopyText(text = param, preview = preview))
                }
                "DM_USER" -> {
                    val parts = param.split(":", limit = 2)
                    val userId = parts.getOrNull(0)?.trim().orEmpty()
                    val draftText = parts.getOrNull(1)?.trim().orEmpty()
                    val matchedUser = users.find { it.id == userId || it.handle.equals(userId, ignoreCase = true) }
                    val userName = matchedUser?.name ?: "Friend"
                    actions.add(EchoAction.DmUser(userId = matchedUser?.id ?: userId, userName = userName, draftText = draftText))
                }
                "SOUNDSCAPE" -> {
                    actions.add(EchoAction.OpenSoundscape("Play $param"))
                }
                "VAULT" -> {
                    actions.add(EchoAction.OpenVault("Open Vault ($param)"))
                }
                "ARCADE" -> {
                    val gameName = if (param.equals("chess", ignoreCase = true)) "Chess" else "Ludo"
                    actions.add(EchoAction.LaunchArcadeGame(gameId = param.lowercase(), gameName = gameName))
                }
            }
        }

        // Remove the action tags from display text
        cleaned = regex.replace(cleaned, "").trimEnd()
        return Pair(cleaned, actions)
    }
}
