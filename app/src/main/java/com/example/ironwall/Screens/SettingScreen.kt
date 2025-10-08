package com.example.ironwall.Screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(navController: NavController) {
    var biometricAuthEnabled by remember { mutableStateOf(false) }
    var secureScreenshotsEnabled by remember { mutableStateOf(false) }
    var disappearingMessagesEnabled by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = "Settings", color = Color.White, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        content = { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(paddingValues)
            ) {
                // Security Section
                item {
                    SettingsSectionHeader("Security")
                }
                item {
                    SettingsToggleItem(
                        title = "Biometric Authentication",
                        description = "Enable fingerprint or face ID for login",
                        isChecked = biometricAuthEnabled,
                        onCheckedChange = { biometricAuthEnabled = it }
                    )
                }
                item {
                    SettingsToggleItem(
                        title = "Secure Screenshots",
                        description = "Prevent screenshots of the app's content",
                        isChecked = secureScreenshotsEnabled,
                        onCheckedChange = { secureScreenshotsEnabled = it }
                    )
                }
                item {
                    SettingsButtonItem(
                        title = "Change PIN",
                        onClick = { /* Navigate to PIN change screen */ }
                    )
                }

                // Privacy Section
                item {
                    SettingsSectionHeader("Privacy")
                }
                item {
                    SettingsToggleItem(
                        title = "Disappearing Messages",
                        description = "Automatically delete messages after a set time",
                        isChecked = disappearingMessagesEnabled,
                        onCheckedChange = { disappearingMessagesEnabled = it }
                    )
                }
                item {
                    SettingsButtonItem(
                        title = "Manage Data Usage",
                        onClick = { /* Navigate to data usage screen */ }
                    )
                }

                // Account Section
                item {
                    SettingsSectionHeader("Account")
                }
                item {
                    SettingsButtonItem(
                        title = "Logout",
                        onClick = { /* Handle logout logic and navigate to login screen */ }
                    )
                }
            }
        }
    )
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    Divider(color = Color.DarkGray, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun SettingsToggleItem(
    title: String,
    description: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 18.sp)
            Text(text = description, color = Color.Gray, fontSize = 12.sp)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Gray,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color.DarkGray,
            )
        )
    }
}

@Composable
fun SettingsButtonItem(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 18.sp)
    }
}