package com.psdk.geetas

import com.psdk.geetas.model.ErrorResponse
import com.psdk.geetas.model.HealthResponse
import com.psdk.geetas.model.SttRequest
import com.psdk.geetas.model.TtsRequest
import com.psdk.geetas.service.GeminiService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }

    val geminiService = GeminiService()

    routing {
        get("/") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    service = "GeeTAS Backend API",
                    apiKeyConfigured = geminiService.isApiKeyConfigured,
                    defaultTtsModel = GeminiService.DEFAULT_TTS_MODEL,
                    defaultSttModel = GeminiService.DEFAULT_STT_MODEL
                )
            )
        }

        get("/health") {
            call.respond(
                HealthResponse(
                    status = "ok",
                    service = "GeeTAS Backend API",
                    apiKeyConfigured = geminiService.isApiKeyConfigured,
                    defaultTtsModel = GeminiService.DEFAULT_TTS_MODEL,
                    defaultSttModel = GeminiService.DEFAULT_STT_MODEL
                )
            )
        }

        route("/api") {
            post("/tts") {
                try {
                    val request = call.receive<TtsRequest>()
                    if (request.text.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(error = "Text parameter cannot be empty for TTS generation")
                        )
                        return@post
                    }

                    val response = geminiService.generateSpeech(request)
                    call.respond(response)
                } catch (e: IllegalStateException) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(error = e.message ?: "API key configuration error")
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = e.message ?: "Invalid request parameters")
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(
                            error = "Failed to generate speech",
                            details = e.localizedMessage ?: e.message
                        )
                    )
                }
            }

            post("/stt") {
                try {
                    val request = call.receive<SttRequest>()
                    if (request.audioBase64.isBlank()) {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            ErrorResponse(error = "audioBase64 parameter cannot be empty for STT transcription")
                        )
                        return@post
                    }

                    val response = geminiService.transcribeSpeech(request)
                    call.respond(response)
                } catch (e: IllegalStateException) {
                    call.respond(
                        HttpStatusCode.Unauthorized,
                        ErrorResponse(error = e.message ?: "API key configuration error")
                    )
                } catch (e: IllegalArgumentException) {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ErrorResponse(error = e.message ?: "Invalid request parameters")
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ErrorResponse(
                            error = "Failed to transcribe audio",
                            details = e.localizedMessage ?: e.message
                        )
                    )
                }
            }
        }
    }
}
