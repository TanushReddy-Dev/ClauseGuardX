package com.clauseguard.core.data.network

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.clientantes.extensions.online
import io.ktor.client.features.contentnegotiation.json
import io.ktor.client.features.header
import io.ktor.client.request.DefaultRequest
import io.ktor.client.timeout.HttpTimeout
import io.ktor.serialization.kotlinx.json.*
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlin.time.experimental.duration

object KtorClient {

    @Volatile
    private var httpClient: HttpClient? = null

    fun getClient(context: Context): HttpClient {
        return httpClient ?: synchronized(this) {
            httpClient?.takeIf { it != null }
                ?: run {
                    val timeout = HttpTimeout(
                        connection = 5_000L,
                        socket = 10_000L,
                    )

                    HttpClient(
                        Android,
                        request = DefaultRequest(),
                        timeout = timeout,
                    ).apply {
                        // JSON content negotiation with ignore-unknown-keys tolerance
                        // and pretty-printing for debugging
                        install(json(Json {
                            ignoreUnknownKeys = true
                            prettyPrint = true
                        }))

                        // Base URL for the FastAPI backend.
                        // In debug/emulator environments, point to the host's localhost
                        // via the special IP 10.0.2.2 which forwards to the host machine.
                        // In production release builds, this would be the deployed API URL.
                        baseUrl = "http://10.0.2.2:8000/api/v1"
                    }

                    httpClient
                }
    }

    /** Optional: close the client when the app shuts down */
    fun closeClient() {
        httpClient?.close()
        httpClient = null
    }
}