package com.example.ironwall

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.logging.*
import kotlinx.serialization.json.Json

val httpClient = HttpClient(OkHttp) {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
            encodeDefaults = true
        })
    }
    install(WebSockets)
    install(Logging) {
        level = LogLevel.ALL
    }

    // Add this to prevent exceptions from being thrown on error responses
    expectSuccess = false
}