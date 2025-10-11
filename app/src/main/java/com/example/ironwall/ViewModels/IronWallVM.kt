package com.example.ironwall.ViewModels

    import android.app.Application
    import android.util.Log

    import androidx.lifecycle.AndroidViewModel
    import androidx.lifecycle.viewModelScope
    import com.example.ironwall.ContactResponse
    import com.example.ironwall.InfoDB.AppDatabase
    import com.example.ironwall.InfoDB.UserTable
    import com.example.ironwall.LoginRequest
    import com.example.ironwall.OtpVerifyRequest
    import com.example.ironwall.RegisterResponseDto
    import com.example.ironwall.TokenResponse
    import com.example.ironwall.UserAccountDto
    import io.ktor.client.HttpClient
    import io.ktor.client.call.body

    import io.ktor.client.request.forms.MultiPartFormDataContent
    import io.ktor.client.request.forms.formData
    import io.ktor.client.request.forms.submitFormWithBinaryData
    import io.ktor.client.request.get
    import io.ktor.client.request.post
    import io.ktor.client.request.setBody
    import io.ktor.client.statement.HttpResponse
    import io.ktor.client.statement.bodyAsText
    import io.ktor.http.Headers
    import io.ktor.http.HttpHeaders

    import io.ktor.http.contentType
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import kotlinx.serialization.json.Json
    import kotlinx.serialization.encodeToString
    import io.ktor.http.ContentType
    import kotlinx.coroutines.flow.firstOrNull


class UserVM(application: Application) : AndroidViewModel(application) {

        // ===== DATABASE =====
        private val db = AppDatabase.getDatabase(application)
        private val dao = db.userDao()
        private val link = "http://10.47.187.147:8888/"

        // Flow of all users (for local cache)
        val users = dao.getAllUsers()

        private val json = Json { ignoreUnknownKeys = true }

        // ===== FETCH CONTACTS =====
        suspend fun fetchContacts(httpClient: HttpClient, userEmail: String): List<UserAccountDto> {
            return withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse = httpClient.get(link + "api/users/contacts/$userEmail")
                    if (response.status.value in 200..299) {
                        val bodyText = response.bodyAsText()
                        Log.d("FetchContacts", "Response: $bodyText")
                        json.decodeFromString(bodyText)
                    } else {
                        Log.e("FetchContacts", "Failed: ${response.status.value}")
                        emptyList()
                    }
                } catch (e: Exception) {
                    Log.e("FetchContacts", "Error: ${e.localizedMessage}")
                    emptyList()
                }
            }
        }

        // ===== GET LAST USER =====
        suspend fun getLastUser(): UserTable? = withContext(Dispatchers.IO) {
            dao.getLastUser().firstOrNull()
        }

        // ===== REGISTER USER =====
        suspend fun registerUser(
            httpClient: HttpClient,
            username: String,
            pin: String,
            userId: String,
            photoBytes: ByteArray?
        ): RegisterResponseDto = withContext(Dispatchers.IO) { // Changed return type to RegisterResponseDto to access TOTP
            val TAG = "RegisterUser"
            try {

                Log.d(TAG, "registerUser() called: userId=$userId, username=$username, photoBytes=${photoBytes?.size}")
                val response: HttpResponse = httpClient.post(link + "auth/register") {
                    setBody(
                        MultiPartFormDataContent(
                            formData {
                                append("username", username)
                                append("pin", pin)
                                append("userId", userId)
                                if (photoBytes != null && photoBytes.isNotEmpty()) {
                                    append("photo", photoBytes, Headers.build {
                                        append(HttpHeaders.ContentType, "image/jpeg")
                                        append(HttpHeaders.ContentDisposition, "filename=profile.jpg")
                                    })
                                }
                            }
                        )
                    )
                }
                Log.d("registerUser", "Response status: ${response.status.value}")
                Log.d("Response : "," ${response.toString()}")
                Log.d("Secret from VM : ","${response.body<RegisterResponseDto>().secret}")
                if (response.status.value in 199..299) {
                    val registerResponse = response.body<RegisterResponseDto>()
                    Log.d("in Response : ","code can reach here ${registerResponse.secret}")
                    // <-- This object should include something like: registerResponse.totpSecret
                    registerResponse
                } else {
                    val text = response.bodyAsText()
                    RegisterResponseDto(
                        message = "Registration failed: ${response.status.value} - $text",
                        secret = null,
                        qrUrl = null
                    )
                }
            } catch (e: Exception) {
                RegisterResponseDto(
                    message = "An error occurred: ${e.localizedMessage}",
                    secret = null,
                    qrUrl = null
                )
            }
        }


    suspend fun loginUser(httpClient: HttpClient, username: String, pin: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val response: HttpResponse = httpClient.post(link + "auth/login") {
                    contentType(ContentType.Application.Json)
                    setBody(LoginRequest(username = username, password = pin))
                }

                if (response.status.value in 200..299) {
                    val tokenResponse = response.body<TokenResponse>()

                    // 🔹 Check if user is blocked
                    when (tokenResponse.userStatus?.uppercase()) {
                        "BLOCKED" -> return@withContext "BLOCKED"   // revoke access
                        "PENDING" -> return@withContext "PENDING"   // optional: show pending popup
                        else -> return@withContext "SUCCESS"       // normal login
                    }

                } else {
                    val errorText = response.bodyAsText()
                    return@withContext "Login failed: ${response.status.value} - $errorText"
                }
            } catch (e: Exception) {
                return@withContext "An error occurred: ${e.localizedMessage}"
            }
        }
    }





    suspend fun verifyOtp(httpClient: HttpClient, username: String, otp: Int): String {
            return withContext(Dispatchers.IO) {
                try {
                    val response: HttpResponse = httpClient.post(link + "auth/verify-otp") {
                        contentType(ContentType.Application.Json)
                        setBody(Json.encodeToString(OtpVerifyRequest(username, otp)))
                    }

                    if (response.status.value in 200..299) {
                        val responseBody = response.bodyAsText()
                        // use json with ignoreUnknownKeys
                        val userAccount = json.decodeFromString<UserAccountDto>(responseBody)

                        dao.insert(
                            UserTable(
                                username = userAccount.username,
                                userId = userAccount.id,
                                pin = "" // placeholder
                            )
                        )
                        "Verification successful!"
                    } else {
                        val text = response.bodyAsText()
                        "Verification failed: $text"
                    }
                } catch (e: Exception) {
                    "An error occurred: ${e.localizedMessage}"
                }
            }
        }





        suspend fun getUser(username: String): UserTable? {
            return dao.getUserByUsername(username)
        }

        suspend fun addContact(
            httpClient: HttpClient,
            name: String,
            phoneNumber: String,
            role: String,
            photoBytes: ByteArray?
        ): ContactResponse = withContext(Dispatchers.IO) {

            val response = httpClient.submitFormWithBinaryData(
                url = link + "api/users/add-contact",
                formData = formData {
                    append("name", name)
                    append("phoneNumber", phoneNumber)
                    append("role", role)
                    if (photoBytes != null && photoBytes.isNotEmpty()) {
                        append("photo", photoBytes, Headers.build {
                            append(HttpHeaders.ContentType, "image/jpeg")
                            append(HttpHeaders.ContentDisposition, "filename=contact.jpg")
                        })
                    }
                }
            )

            if (response.status.value in 200..299) {
                val text = response.bodyAsText()
                Json { ignoreUnknownKeys = true }.decodeFromString<ContactResponse>(text)
            } else {
                throw RuntimeException("Failed to add contact: ${response.status.value}")
            }
        }







        // ===== CRUD OPERATIONS =====

        fun deleteUser(user: UserTable) {
            viewModelScope.launch {
                dao.delete(user)
            }
        }

        fun addUser(user: UserTable) {
            viewModelScope.launch {
                dao.insert(user)
            }
        }

        fun updateUser(id: Int, newUsername: String, newUserId: String,newPin : String) {
            viewModelScope.launch {
                dao.update(UserTable(id = id, username = newUsername, userId = newUserId, pin = newPin))
            }
        }
    }