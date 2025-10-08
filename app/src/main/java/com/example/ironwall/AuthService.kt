package com.example.ironwall

import android.provider.ContactsContract.PinnedPositions.pin
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



//suspend fun registerUser(username: String, password: String): String {
//    return withContext(Dispatchers.IO) {
//        try {
//            val response = httpClient.post("http://localhost:8888/auth/register") {
//                contentType(ContentType.Application.Json)
//                setBody(RegistrationRequest(username, pin))
//            }
//
//            // Handle the response, e.g., check the status code
//            if (response.status.value == 200) {
//                "Registration successful!"
//            } else {
//                "Registration failed with status: ${response.status}"
//            }
//        } catch (e: Exception) {
//            "An error occurred: ${e.localizedMessage}"
//        }
//    }
//}


//suspend fun verifyOtp(username: String, pin: String): String {
//    return withContext(Dispatchers.IO) {
//        try {
//            val response: HttpResponse = httpClient.post("http://localhost:8888/auth/verify-otp") {
//                contentType(ContentType.Application.Json)
//                setBody(VerifyOtpRequest(username, pin))
//            }
//            if (response.status.value == 200) {
//                "Verification successful!"
//            } else {
//                "Verification failed with status: ${response.status}"
//            }
//        } catch (e: Exception) {
//            "An error occurred: ${e.localizedMessage}"
//        }
//    }
//}

//suspend fun loginUser(username: String, pin: String): String {
//    return withContext(Dispatchers.IO) {
//        try {
//            val response: HttpResponse = httpClient.post("http://localhost:8888/auth/login") {
//                contentType(ContentType.Application.Json)
//                setBody(LoginRequest(username, pin))
//            }
//            if (response.status.value == 200) {
//                // You might parse a token from the response here
//                "Login successful!"
//            } else {
//                "Login failed with status: ${response.status}"
//            }
//        } catch (e: Exception) {
//            "An error occurred: ${e.localizedMessage}"
//        }
//    }
//}