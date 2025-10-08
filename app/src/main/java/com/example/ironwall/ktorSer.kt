package com.example.ironwall

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


// Request/Response models for contact operations
@Serializable
data class ContactRequest(
    val name: String,
    val phoneNumber: String,
    val role: String
)

@Serializable
data class ContactResponse(
    val id: String? = null,           // If UserAccount has an id field
    val username: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null,
    val photoId: String? = null,
    val status: String? = null,
    val enabled: Boolean = false       // default false if backend can return null
)

// ✅ This is only for JSON "user" part
@Serializable
data class RegistrationRequest(
    @SerialName("uID") val userId: String,
    val username: String,
    val pin: String
)
@Serializable
// ✅ This matches your backend RegisterResponse
data class RegisterResponseDto(
    val message: String,
    val secret: String?,  // TOTP secret
    val qrUrl: String?    // QR URL for Google Authenticator
)
@Serializable
data class VerifyOtpRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class UserResponse(val userID: String, val username: String)

@Serializable
data class TokenResponse(
    val secret: String? = null,
    val message: String? = null
)


@kotlinx.serialization.Serializable
data class OtpVerifyRequest(
    val username: String,
    val otp: Int
)
@kotlinx.serialization.Serializable
data class UserAccountDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val phoneNumber: String? = null,
    val role: String? = null,
    val photoId: String? = null
)



