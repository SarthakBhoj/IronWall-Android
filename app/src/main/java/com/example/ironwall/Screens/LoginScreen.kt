package com.example.ironwall.Screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ironwall.R
import com.example.ironwall.Screen
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ironwall.ViewModels.UserVM
import com.example.ironwall.httpClient

import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    userViewModel: UserVM = viewModel()
) {
    val users by userViewModel.users.collectAsState(initial = emptyList())
    val lastUser = users.lastOrNull()
    var username by remember { mutableStateOf(lastUser?.username ?: "") }
    var pin by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // 🔹 State for showing blocked popup
    var showBlockedDialog by remember { mutableStateOf(false) }

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
            Image(
                painter = painterResource(id = R.drawable.applogo),
                contentDescription = "App Logo",
                modifier = Modifier.size(100.dp),
                contentScale = ContentScale.Crop
            )

            Text(
                text = "IronWall",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 30.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Enter Username") },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it },
                label = { Text("Enter the 6-digit PIN") },
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )

            TextButton(
                onClick = { navController.navigate(Screen.Registration.route) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.Start)
            ) {
                Text("New User?", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                onClick = {
                    scope.launch {
                        if (username.isBlank() || pin.isBlank()) {
                            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                            return@launch
                        }

                        val result = userViewModel.loginUser(httpClient, username, pin)
                        when (result) {
                            "SUCCESS" -> {
                                Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                                navController.navigate(Screen.TokenEntry.route) {
                                    popUpTo(Screen.Login.route) { inclusive = true }
                                }
                            }

                            "BLOCKED" -> {
                                // Show revoked popup
                                showBlockedDialog = true
                            }

                            else -> {
                                Log.d("Error : ", result)
                                Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            ) {
                Text("Enter", color = Color.White, fontSize = 20.sp)
            }
        }

        // 🚫 Revoked Authorization Dialog
        if (showBlockedDialog) {
            AlertDialog(
                onDismissRequest = { showBlockedDialog = false },
                title = {
                    Text(
                        text = "Access Revoked",
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Your authorization has been revoked by Headquarters.\nPlease contact your commanding officer for assistance.",
                        color = Color.White
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showBlockedDialog = false
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    ) {
                        Text("OK", color = Color.White)
                    }
                },
                containerColor = Color.DarkGray,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}