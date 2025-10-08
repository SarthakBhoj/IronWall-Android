package com.example.ironwall

import android.app.Application
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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

@Serializable
data class ChatMessage(
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val status: String = "SENT",
    val timestamp: String,
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

data class DisplayMessage(
    val message: ChatMessage,
    val decryptedContent: String,
    val isDecrypting: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val link = "http://10.47.187.147:8888/"

    private var stompClient: StompClient? = null
    private val _rawMessages = mutableStateListOf<ChatMessage>()
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
                val safeMessage = message.copy(id = message.id ?: UUID.randomUUID().toString())

                Log.d(TAG, "📥 Received message from ${safeMessage.senderId}: ${safeMessage.content}")

                viewModelScope.launch {
                    addIncomingMessage(safeMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }
    }

    private suspend fun addIncomingMessage(message: ChatMessage) {
        if (_rawMessages.any { it.id == message.id }) return
        _rawMessages.add(message)

        val safeMsg = message.copy(content = message.content ?: "[Empty]")
        Log.d(TAG, "📩 Showing received message: ${safeMsg.content}")

        if (_displayMessages.none { it.message.id == safeMsg.id }) {
            _displayMessages.add(
                DisplayMessage(
                    message = safeMsg,
                    decryptedContent = safeMsg.content,
                    isDecrypting = false
                )
            )
        }
    }

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

    suspend fun fetchChatHistory(
        httpClient: HttpClient,
        senderId: String,
        receiverId: String
    ) {
        currentHttpClient = httpClient
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get("${link}chat/history/$senderId/$receiverId")
                val responseText = response.bodyAsText()
                val history = Json.decodeFromString<List<ChatMessage>>(responseText)

                _rawMessages.clear()
                _displayMessages.clear()

                history.forEach { msg ->
                    val safeMsg = msg.copy(id = msg.id ?: UUID.randomUUID().toString())

                    if (_rawMessages.none { it.id == safeMsg.id }) _rawMessages.add(safeMsg)

                    if (_displayMessages.none { it.message.id == safeMsg.id }) {
                        _displayMessages.add(
                            DisplayMessage(
                                message = safeMsg,
                                decryptedContent = safeMsg.content,
                                isDecrypting = false
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error fetching history: ${e.message}", e)
            }
        }
    }

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

            withContext(Dispatchers.Main) {
                if (_displayMessages.none { it.message.id == message.id }) {
                    _rawMessages.add(message)
                    _displayMessages.add(
                        DisplayMessage(
                            message = message,
                            decryptedContent = content,
                            isDecrypting = false
                        )
                    )
                }
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
