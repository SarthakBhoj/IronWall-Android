package com.example.ironwall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    currentUserId: String,
    receiverId: String,
    receiverName: String,
    httpClient: HttpClient
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val chatViewModel: ChatViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ChatViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    val messages by chatViewModel.messages.collectAsState()
    val connectionState by chatViewModel.connectionState.collectAsState()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var decryptedMessages by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(Unit) {
        chatViewModel.connectWebSocket(currentUserId)
        chatViewModel.fetchChatHistory(httpClient, currentUserId, receiverId)
    }

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
                        Text(receiverName)
                        Text(
                            text = when (connectionState) {
                                is ChatViewModel.ConnectionState.Connected -> "Connected"
                                is ChatViewModel.ConnectionState.Connecting -> "Connecting..."
                                is ChatViewModel.ConnectionState.Disconnected -> "Disconnected"
                                is ChatViewModel.ConnectionState.Error -> "Connection Error"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = when (connectionState) {
                                is ChatViewModel.ConnectionState.Connected -> Color.Green
                                else -> Color.Gray
                            }
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
                shadowElevation = 8.dp
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
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (messageText.isNotBlank()) {
                                chatViewModel.sendMessage(
                                    senderId = currentUserId,
                                    receiverId = receiverId,
                                    content = messageText
                                )
                                messageText = ""
                            }
                        },
                        enabled = messageText.isNotBlank() &&
                                connectionState is ChatViewModel.ConnectionState.Connected
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send",
                            tint = if (messageText.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else
                                Color.Gray
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            state = listState
        ) {
            items(messages) { message ->
                MessageBubble(
                    message = message,
                    isOwnMessage = message.senderId == currentUserId,
                    decryptedContent = decryptedMessages[message.id],
                    onDecrypt = {
                        coroutineScope.launch {
                            message.id?.let { messageId ->
                                val decrypted = chatViewModel.decryptMessage(
                                    httpClient,
                                    messageId,
                                    currentUserId
                                )
                                decryptedMessages = decryptedMessages + (messageId to decrypted)
                            }
                        }
                    }
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            chatViewModel.disconnect()
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isOwnMessage: Boolean,
    decryptedContent: String?,
    onDecrypt: () -> Unit
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
            )
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                if (decryptedContent != null) {
                    Text(
                        text = decryptedContent,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = if (isOwnMessage) message.content else "[Encrypted]",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isOwnMessage) FontWeight.Normal else FontWeight.Light
                    )

                    if (!isOwnMessage) {
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = onDecrypt,
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Text("Decrypt", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = java.text.SimpleDateFormat(
                            "HH:mm",
                            java.util.Locale.getDefault()
                        ).format(java.util.Date(message.timestamp.toLongOrNull() ?: 0)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = message.status,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}