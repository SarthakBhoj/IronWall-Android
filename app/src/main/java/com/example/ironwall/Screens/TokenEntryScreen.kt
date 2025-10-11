package com.example.ironwall.Screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ironwall.Screen
import com.example.ironwall.ViewModels.UserVM
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.launch

@Composable
fun TokenEntryScreen(
    navController: NavController,
    totpSecret: String? = null,
    userViewModel: UserVM = viewModel(),
    modifier: Modifier = Modifier
) {
    val users by userViewModel.users.collectAsState(initial = emptyList())
    val lastUser = users.lastOrNull()
    var username by remember { mutableStateOf(lastUser?.username ?: "") }
    var authToken by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val httpClient = remember {
        HttpClient(OkHttp) {
            install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
                json(kotlinx.serialization.json.Json {
                    prettyPrint = true
                    isLenient = true
                    ignoreUnknownKeys = true
                })
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ✅ Show TOTP Secret (if available)
            totpSecret?.takeIf { it.isNotEmpty() }?.let { secret ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color.Cyan),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .clickable {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                val clip = ClipData.newPlainText("TOTP Secret", secret)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT)
                                    .show()
                            }
                    ) {
                        Text(
                            text = "Your Authenticator Code: $secret",
                            color = Color.Cyan,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Icon",
                            tint = Color.Cyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Use this code in your Authenticator app to generate OTP",
                    color = Color.White,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Text(
                text = "Two-Factor Authentication",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 24.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ✅ Username field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp)
            )

            // ✅ OTP Field
            OutlinedTextField(
                value = authToken,
                onValueChange = { if (it.length <= 6) authToken = it },
                label = { Text("Auth Token") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    unfocusedLabelColor = Color.Gray,
                    focusedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ✅ Submit Button
            Button(
                onClick = {
                    scope.launch {
                        if (username.isEmpty()) {
                            Toast.makeText(context, "Enter your username", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val otpInt = authToken.toIntOrNull()
                        if (otpInt == null) {
                            Toast.makeText(context, "Enter a valid number", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        // Fetch lastUser to get pin and userId
                        val lastUserLocal = users.lastOrNull()
                        val userId = lastUserLocal?.userId
                        val pin = lastUserLocal?.pin

                        val result = userViewModel.verifyOtp(httpClient, username, otpInt)
                        if (result == "Verification successful!") {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.TokenEntry.route) { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
            ) {
                Text("Submit", color = Color.White, fontSize = 20.sp)
            }

        }
    }
}
