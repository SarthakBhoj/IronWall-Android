package com.example.ironwall

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ironwall.ViewModels.ChatViewModel
import com.example.ironwall.ViewModels.DisplayMessage
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUserId: String,
    receiverId: String,
    receiverName: String,
    httpClient: HttpClient,
    navController: NavController
) {
    val context = LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(context.applicationContext as Application) as T
            }
        }
    )

    // Observe state from ViewModel
    val messages = chatViewModel.messages // List<DisplayMessage>
    val connectionState by chatViewModel.connectionState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Local UI state
    var messageText by remember { mutableStateOf("") }

    // Connect WebSocket and fetch history when screen loads
    LaunchedEffect(Unit) {
        chatViewModel.connectWebSocket(currentUserId)
        chatViewModel.fetchChatHistory(httpClient, currentUserId, receiverId)
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = receiverName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when (connectionState) {
                                is ChatViewModel.ConnectionState.Connected -> "Connected"
                                is ChatViewModel.ConnectionState.Connecting -> "Connecting..."
                                is ChatViewModel.ConnectionState.Disconnected -> "Disconnected"
                                is ChatViewModel.ConnectionState.Error -> {
                                    val error = connectionState as ChatViewModel.ConnectionState.Error
                                    "Error: ${error.message}"
                                }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (connectionState) {
                                is ChatViewModel.ConnectionState.Connected -> Color.Green
                                is ChatViewModel.ConnectionState.Connecting -> Color.Yellow
                                is ChatViewModel.ConnectionState.Disconnected -> Color.Gray
                                is ChatViewModel.ConnectionState.Error -> Color.Red
                            }
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Type a message...") },
                        maxLines = 4,
                        shape = RoundedCornerShape(24.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                coroutineScope.launch {
                                    chatViewModel.sendMessage(
                                        senderId = currentUserId,
                                        receiverId = receiverId,
                                        content = messageText
                                    )
                                    messageText = "" // Clear input
                                }
                            }
                        },
                        enabled = messageText.isNotBlank() &&
                                connectionState is ChatViewModel.ConnectionState.Connected
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send Message",
                            tint = if (messageText.isNotBlank() &&
                                connectionState is ChatViewModel.ConnectionState.Connected)
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // Show loading state
                messages.isEmpty() && connectionState is ChatViewModel.ConnectionState.Connecting -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading messages...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }

                // Show empty state
                messages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No messages yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Send a message to start the conversation with $receiverName",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Show messages list
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        state = listState,
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        itemsIndexed(messages) { index, displayMessage ->
                            key("${displayMessage.message.id}-${displayMessage.message.timestamp}-$index") {
                                MessageBubble(
                                    displayMessage = displayMessage,
                                    isOwnMessage = displayMessage.message.senderId == currentUserId
                                )
                            }
                        }
                    }


                }
            }
        }
    }

    // Cleanup when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.disconnect()
        }
    }
}

@Composable
fun MessageBubble(
    displayMessage: DisplayMessage,
    isOwnMessage: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isOwnMessage) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isOwnMessage) 12.dp else 4.dp,
                bottomEnd = if (isOwnMessage) 4.dp else 12.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isOwnMessage)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                // Message content
                if (displayMessage.isDecrypting) {
                    // Show loading indicator while decrypting
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Decrypting...",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                } else {
                    // Show decrypted content
                    Text(
                        text = displayMessage.decryptedContent,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Timestamp and status row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Timestamp
                    Text(
                        text = formatTimestamp(displayMessage.message.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = if (isOwnMessage)
                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )

                    // Status (only for own messages)
                    if (isOwnMessage) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = displayMessage.message.status,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// Helper function to format timestamp
private fun formatTimestamp(timestamp: String): String {
    return try {
        val millis = timestamp.toLongOrNull() ?: return "Invalid time"
        val date = Date(millis)
        val now = Date()

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())

        // If message is from today, show only time
        val calendar = Calendar.getInstance()
        calendar.time = now
        val todayDay = calendar.get(Calendar.DAY_OF_YEAR)
        val todayYear = calendar.get(Calendar.YEAR)

        calendar.time = date
        val messageDay = calendar.get(Calendar.DAY_OF_YEAR)
        val messageYear = calendar.get(Calendar.YEAR)

        if (todayDay == messageDay && todayYear == messageYear) {
            timeFormat.format(date)
        } else {
            "${dateFormat.format(date)} ${timeFormat.format(date)}"
        }
    } catch (e: Exception) {
        "Invalid time"
    }
}


