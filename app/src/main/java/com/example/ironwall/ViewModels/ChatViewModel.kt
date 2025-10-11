package com.example.ironwall.ViewModels


import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent
import java.util.UUID


// --- ChatMessage Data Classes ---
@Serializable
data class ChatMessage(
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val status: String = "SENT",
    val timestamp: String,
    // Add a field to track if this is decrypted content
    val isDecrypted: Boolean = false
)

@Serializable
data class DecryptRequest(
    val messageId: String,
    val receiverId: String
)

@Serializable
data class DecryptedMessage(
    val content: String
)

// UI Message wrapper that includes decrypted content
data class DisplayMessage(
    val message: ChatMessage,
    val decryptedContent: String,
    val isDecrypting: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val link = "http://10.47.187.147:8888/"

    private var stompClient: StompClient? = null

    // Store raw messages
    private val _rawMessages = mutableStateListOf<ChatMessage>()

    // Store display messages with decrypted content
    private val _displayMessages = mutableStateListOf<DisplayMessage>()
    val messages: SnapshotStateList<DisplayMessage> get() = _displayMessages

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUserId: String? = null
    private var currentHttpClient: HttpClient? = null

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Connecting : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    fun connectWebSocket(userId: String) {
        currentUserId = userId
        _connectionState.value = ConnectionState.Connecting

        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://10.47.187.147:8888/chat-websocket"
        )

        stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "✅ WebSocket connected")
                    _connectionState.value = ConnectionState.Connected
                    subscribeToMessages(userId)
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "🔌 WebSocket closed")
                    _connectionState.value = ConnectionState.Disconnected
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "❌ WebSocket error: ${event.exception?.message}")
                    _connectionState.value = ConnectionState.Error(
                        event.exception?.message ?: "Unknown error"
                    )
                }
                else -> {}
            }
        }

        stompClient?.connect()
    }

    private fun subscribeToMessages(userId: String) {
        stompClient?.topic("/topic/messages/$userId")?.subscribe { topicMessage ->
            try {
                val message = Json.decodeFromString<ChatMessage>(topicMessage.payload)
                val safeMessage = message.copy(
                    id = message.id ?: UUID.randomUUID().toString()
                )

                Log.d(TAG, "📥 Received message from ${safeMessage.senderId}: ${safeMessage.content}")

                viewModelScope.launch {
                    addIncomingMessage(safeMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }
    }

    /**
     * Add incoming message and decrypt if needed
     */
    private suspend fun addIncomingMessage(message: ChatMessage) {
        // Skip if already exists in display messages
        if (_displayMessages.any { it.message.id == message.id }) return

        _rawMessages.add(message)

        // Show message as-is
        _displayMessages.add(
            DisplayMessage(
                message = message,
                decryptedContent = message.content, // show content directly
                isDecrypting = false
            )
        )
    }



    /**
     * Decrypt a message from another user
     */
    private suspend fun decryptMessage(
        httpClient: HttpClient,
        messageId: String,
        receiverId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔓 Decrypting message $messageId")
            val response = httpClient.post(link + "chat/decrypt") {
                contentType(ContentType.Application.Json)
                setBody(DecryptRequest(messageId, receiverId))
            }

            if (response.status.value in 200..299) {
                val decrypted = response.body<DecryptedMessage>().content
                Log.d(TAG, "✅ Message decrypted: $decrypted")
                decrypted
            } else {
                Log.e(TAG, "❌ Failed to decrypt: ${response.status.value}")
                "[Failed to decrypt]"
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Decryption error: ${e.message}", e)
            "[Error: ${e.localizedMessage}]"
        }
    }

    /**
     * Fetch chat history
     */
    suspend fun fetchChatHistory(
        httpClient: HttpClient,
        senderId: String,
        receiverId: String
    ) {
        currentHttpClient = httpClient

        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "📚 Fetching chat history: $senderId <-> $receiverId")

                val response = httpClient.get("${link}chat/history/$senderId/$receiverId")
                val responseText = response.bodyAsText()

                Log.d(TAG, "Response: $responseText")

                val history = Json.decodeFromString<List<ChatMessage>>(responseText)

                Log.d(TAG, "✅ Loaded ${history.size} messages from history")

                _rawMessages.clear()
                _displayMessages.clear()

                // Process each historical message
                history.forEach { msg ->
                    val safeMsg = msg.copy(id = msg.id ?: UUID.randomUUID().toString())
                    _rawMessages.add(safeMsg)

                    // Display content as-is (no decryption)
                    _displayMessages.add(
                        DisplayMessage(
                            message = safeMsg,
                            decryptedContent = safeMsg.content,
                            isDecrypting = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching history: ${e.message}", e)
            }
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String
    ): ChatMessage = withContext(Dispatchers.IO) {
        val message = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            status = "SENT",
            timestamp = System.currentTimeMillis().toString()
        )

        try {
            Log.d(TAG, "📤 Sending message: $content")
            val json = Json.encodeToString(message)

            stompClient?.send("/app/send", json)?.subscribe(
                {
                    Log.d(TAG, "✅ Message sent successfully")
                },
                { error ->
                    Log.e(TAG, "❌ Error sending message: ${error.message}")
                }
            )

            // Add to our local list immediately (don't wait for echo from server)
            withContext(Dispatchers.Main) {
                _rawMessages.add(message)
                _displayMessages.add(
                    DisplayMessage(
                        message = message,
                        decryptedContent = content, // Show our own message as plaintext
                        isDecrypting = false
                    )
                )
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error preparing message: ${e.message}", e)
        }

        message
    }

    fun disconnect() {
        stompClient?.disconnect()
        stompClient = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }
}


/////////////////////////////////////