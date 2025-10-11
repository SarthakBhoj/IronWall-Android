import android.app.Application
import android.content.ContextWrapper
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.AndroidViewModel
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class StatusViewModel(application: Application) : AndroidViewModel(application) {
    private val wsUrl = "ws://10.47.187.147:8888/chat-websocket"
    private var stompClient: StompClient? = null
    private var topicSubscription: Disposable? = null

    private val _userStatus = MutableStateFlow<Map<String, String>>(emptyMap())
    val userStatus: StateFlow<Map<String, String>> = _userStatus

    private var reconnectJob: Job? = null
    private val reconnectDelay = 5000L // 5 seconds delay

    /**
     * Connect to the WebSocket and subscribe to user-status updates
     */
    fun connectForStatusUpdates(currentUserEmail: String, onBlocked: () -> Unit) {
        Log.d("StatusVM", "🔗 Attempting to connect to WebSocket at $wsUrl")
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, wsUrl)

        stompClient?.lifecycle()?.subscribe { event ->
            when (event.type) {
                LifecycleEvent.Type.OPENED -> Log.d("StatusVM", "✅ WebSocket OPENED")
                LifecycleEvent.Type.CLOSED -> {
                    Log.d("StatusVM", "🔌 WebSocket CLOSED, scheduling reconnect...")
                    scheduleReconnect(currentUserEmail, onBlocked)
                }
                LifecycleEvent.Type.ERROR -> {
                    Log.e("StatusVM", "❌ WebSocket ERROR: ${event.exception?.message}")
                    scheduleReconnect(currentUserEmail, onBlocked)
                }
                else -> {}
            }
        }

        stompClient?.connect()

        topicSubscription = stompClient?.topic("/topic/user-status")?.subscribe { topicMessage ->
            try {
                val payload = topicMessage.payload
                val json = JSONObject(payload)
                val email = json.optString("email")
                val status = json.optString("status")

                if (email.isNotEmpty() && status.isNotEmpty()) {
                    _userStatus.value = mapOf("email" to email, "status" to status)
                    Log.d("StatusVM", "📩 Status update: $email -> $status")

                    if (email.equals(currentUserEmail, ignoreCase = true)) {
                        GlobalUserStatus.updateStatus(status.uppercase())
                        if (status.equals("BLOCKED", ignoreCase = true)) {
                            onBlocked()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StatusVM", "❌ Error parsing WS message: ${e.message}")
            }
        }
    }

    /**
     * Schedule reconnect with delay
     */
    private fun scheduleReconnect(currentUserEmail: String, onBlocked: () -> Unit) {
        if (reconnectJob?.isActive == true) return // already scheduled

        reconnectJob = viewModelScope.launch {
            delay(reconnectDelay)
            Log.d("StatusVM", "♻️ Reconnecting to WebSocket...")
            connectForStatusUpdates(currentUserEmail, onBlocked)
        }
    }

    /**
     * Manual disconnect
     */
    fun disconnect() {
        topicSubscription?.dispose()
        stompClient?.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    /**
     * Optional: Fetch current status from backend if WebSocket may be down
     */
    suspend fun fetchCurrentUserStatus(httpClient: HttpClient, email: String, onBlocked: () -> Unit) {
        try {
            val response: HttpResponse = httpClient.get("http://10.47.187.147:8888/api/users/status/$email")
            if (response.status.value in 200..299) {
                val statusJson = JSONObject(response.bodyAsText())
                val status = statusJson.optString("status")
                if (status.isNotEmpty()) {
                    GlobalUserStatus.updateStatus(status.uppercase())
                    if (status.equals("BLOCKED", ignoreCase = true)) {
                        onBlocked()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("StatusVM", "❌ Error fetching current status: ${e.localizedMessage}")
        }
    }
}

object GlobalUserStatus {
    private val _status = MutableStateFlow("ACTIVE")
    val status: StateFlow<String> = _status

    fun updateStatus(newStatus: String) {
        Log.d("GlobalUserStatus", "Updating global status -> $newStatus")
        _status.value = newStatus
    }
}

@Composable
fun GlobalStatusObserver(activity: ComponentActivity, content: @Composable () -> Unit) {
    val status by GlobalUserStatus.status.collectAsState()

    LaunchedEffect(status) {
        if (status == "BLOCKED") {
            Toast.makeText(activity, "Blocked! Exiting...", Toast.LENGTH_LONG).show()
            activity.finishAffinity() // now guaranteed to work
        }
    }

    content()
}

