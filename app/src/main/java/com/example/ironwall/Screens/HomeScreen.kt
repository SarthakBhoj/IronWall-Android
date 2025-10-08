package com.example.ironwall.Screens

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ironwall.Screen
import com.example.ironwall.UserVM

// Data class and dummy data
data class ChatItem(val name: String, val lastMessage: String, val time: String)

val dummyChats = listOf(
    ChatItem("Family Group", "Meeting tonight at 8 PM", "10:30 AM"),
    ChatItem("Veterans Network", "Remember to fill out the form", "Yesterday"),
    ChatItem("Alpha Squad", "Ops report submitted.", "2 days ago"),
    ChatItem("Security Team", "New threat identified.", "2 days ago"),
    ChatItem("Team Blue", "Update on the new protocol.", "3 days ago"),
    ChatItem("Spouse Support", "Looking for local resources.", "3 days ago"),
)
// adding comments
@Composable
fun HomeScreen(navController: NavController) {
    val userViewModel: UserVM = viewModel()
    val users by userViewModel.users.collectAsState(initial = emptyList())
    val lastUser = users.lastOrNull()
    val displayUsername = lastUser?.username ?: "Guest"

    Scaffold(
        topBar = {
            // Pass displayUsername to HomeHeader
            HomeHeader(navController = navController, displayUsername = displayUsername)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddContact.route) },
                containerColor = Color.DarkGray,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New chat")
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(dummyChats) { index, chat ->
                        ChatItemRow(chat = chat, navController = navController)
                    }
                }
            }
        }
    )
}

// HomeHeader now takes displayUsername as parameter
@Composable
fun HomeHeader(navController: NavController, displayUsername: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "IW",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Welcome, $displayUsername",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = "Online",
                    color = Color.Green,
                    fontSize = 12.sp
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(
                onClick = { navController.navigate(Screen.Setting.route) }
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun ChatItemRow(chat: ChatItem, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(Screen.Chat.route) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.DarkGray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = chat.name.first().toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = chat.name,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = chat.lastMessage,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = chat.time,
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
