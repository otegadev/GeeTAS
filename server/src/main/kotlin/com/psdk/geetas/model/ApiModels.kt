package com.psdk.geetas.model

import kotlinx.serialization.Serializable

@Serializable
data class TtsRequest(
    val text: String,
    val model: String? = null,
    val voice: String? = null
)

@Serializable
data class TtsResponse(
    val text: String? = null,
    val audioBase64: String? = null,
    val mimeType: String? = "audio/mp3",
    val modelUsed: String
)

@Serializable
data class SttRequest(
    val audioBase64: String,
    val mimeType: String = "audio/wav",
    val prompt: String? = null,
    val model: String? = null
)

@Serializable
data class SttResponse(
    val text: String,
    val modelUsed: String
)

@Serializable
data class ErrorResponse(
    val error: String,
    val details: String? = null
)

@Serializable
data class HealthResponse(
    val status: String,
    val service: String,
    val apiKeyConfigured: Boolean,
    val defaultTtsModel: String,
    val defaultSttModel: String
)
