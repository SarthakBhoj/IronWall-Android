package com.example.ironwall.Screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.ironwall.R
import com.example.ironwall.Screen
import com.example.ironwall.UserVM
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ironwall.InfoDB.UserTable


@Composable
fun RegistrationScreen(
    navController: androidx.navigation.NavController,
    httpClient: HttpClient,
    modifier: Modifier = Modifier,
    userViewModel: UserVM = viewModel()
) {
    var username by remember { mutableStateOf("") }
    var userID by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var totpSecret by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val tempImageUri = remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) imageUri = tempImageUri.value
    }



    val photoBytes = imageUri?.let { uriToBytes(context, it) } ?: ByteArray(0)

    // Main Box
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Column with focus clearing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() },
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

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "New User Registration",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 20.sp,
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Profile Image Upload
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.DarkGray)
                    .clickable {
                        val file = File(
                            context.cacheDir,
                            "temp_image_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
                        )
                        val uri: Uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        tempImageUri.value = uri
                        cameraLauncher.launch(uri)
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Selected Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Image Placeholder",
                        tint = Color.Gray,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))
            Text("Upload your photo", color = Color.White)
            Spacer(modifier = Modifier.height(20.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // UserID Field
            OutlinedTextField(
                value = userID,
                onValueChange = { userID = it },
                label = { Text("Enter UserID") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // PIN Field
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 6) pin = it },
                label = { Text("Set 4-digit PIN") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.Gray,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.Gray,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Register Button
            // --- Inside your Register Button onClick ---
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                onClick = {
                    scope.launch {
                        val response = userViewModel.registerUser(
                            httpClient,
                            username,
                            pin,
                            userID,
                            photoBytes
                        )
                        Log.d("Secret : ",response.secret.toString())
                        totpSecret = response.secret
                        if (!response.secret.isNullOrEmpty()) {
                            val user = UserTable(
                                id = 0, // Room auto generates if @PrimaryKey(autoGenerate = true)
                                username = username,
                                userId = userID,
                                pin = pin
                            )
                            userViewModel.addUser(user)

//                            navController.navigate(Screen.TokenEntry.route)
                            navController.navigate(Screen.TokenEntry.route + "?totpSecret=${response.secret}") {
                                popUpTo(Screen.Registration.route) { inclusive = true }
                            }
//                            navController.navigate(Screen.Login.route + "?totpSecret=${totpSecret ?: ""}") {
//                                popUpTo(Screen.Registration.route) { inclusive = true }
//                            }
                        } else {
                            // Optional: show error if registration failed
                            Log.e("Registration", "Failed: ${response.message}")
                        }
                    }
                    Log.d("onClick : ","in button click")
                }
            ) {
                Text("Register", color = Color.White, fontSize = 20.sp)
            }


            // Login Redirect
            TextButton(
                onClick = {
                    navController.navigate(Screen.TokenEntry.route) {
                        popUpTo(Screen.Registration.route) { inclusive = true }
                    }
                },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("Already registered? Login", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

// Helper function to convert Uri to ByteArray
fun uriToBytes(context: Context, uri: Uri): ByteArray {
    val bitmap: Bitmap = if (Build.VERSION.SDK_INT < 28) {
        MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
    } else {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source)
    }

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
    return outputStream.toByteArray()
}
