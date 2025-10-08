package com.example.ironwall


import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
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

@Serializable
data class ChatMessage(
    val id: String? = null,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val status: String = "SENT",
    val timestamp: String
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

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "ChatViewModel"
    private val link = "http://10.172.8.147:8888/"

    private var stompClient: StompClient? = null
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var currentUserId: String? = null

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Connecting : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    fun connectWebSocket(userId: String) {
        currentUserId = userId
        _connectionState.value = ConnectionState.Connecting

        // Create STOMP client
        stompClient = Stomp.over(
            Stomp.ConnectionProvider.OKHTTP,
            "ws://10.172.8.147:8888/chat-websocket"
        )

        stompClient?.lifecycle()?.subscribe { lifecycleEvent ->
            when (lifecycleEvent.type) {
                LifecycleEvent.Type.OPENED -> {
                    Log.d(TAG, "WebSocket connected")
                    _connectionState.value = ConnectionState.Connected
                    subscribeToMessages(userId)
                }
                LifecycleEvent.Type.CLOSED -> {
                    Log.d(TAG, "WebSocket closed")
                    _connectionState.value = ConnectionState.Disconnected
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e(TAG, "WebSocket error: ${lifecycleEvent.exception}")
                    _connectionState.value = ConnectionState.Error(
                        lifecycleEvent.exception?.message ?: "Unknown error"
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
                Log.d(TAG, "Received message: $message")

                viewModelScope.launch {
                    val currentMessages = _messages.value.toMutableList()
                    currentMessages.add(message)
                    _messages.value = currentMessages
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing message: ${e.message}")
            }
        }
    }

    fun sendMessage(
        senderId: String,
        receiverId: String,
        content: String
    ) {
        viewModelScope.launch {
            try {
                val message = ChatMessage(
                    senderId = senderId,
                    receiverId = receiverId,
                    content = content,
                    status = "SENT",
                    timestamp = System.currentTimeMillis().toString()
                )

                val json = Json.encodeToString(message)
                stompClient?.send("/app/send", json)?.subscribe(
                    {
                        Log.d(TAG, "Message sent successfully")
                    },
                    { error ->
                        Log.e(TAG, "Error sending message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing message: ${e.message}")
            }
        }
    }

    suspend fun decryptMessage(
        httpClient: HttpClient,
        messageId: String,
        receiverId: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.post(link + "chat/decrypt") {
                contentType(ContentType.Application.Json)
                setBody(DecryptRequest(messageId, receiverId))
            }

            if (response.status.value in 200..299) {
                val decrypted = response.body<DecryptedMessage>()
                decrypted.content
            } else {
                "Failed to decrypt message"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Decryption error: ${e.message}")
            "Error: ${e.localizedMessage}"
        }
    }

    suspend fun fetchChatHistory(
        httpClient: HttpClient,
        senderId: String,
        receiverId: String
    ) = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get(link + "chat/history/$senderId/$receiverId")

            if (response.status.value in 200..299) {
                val history = response.body<List<ChatMessage>>()
                _messages.value = history
            } else {
                Log.e(TAG, "Failed to fetch chat history")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching history: ${e.message}")
        }
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