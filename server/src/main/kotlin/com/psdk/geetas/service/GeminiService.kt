package com.psdk.geetas.service

import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Blob
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.Part
import com.psdk.geetas.model.SttRequest
import com.psdk.geetas.model.SttResponse
import com.psdk.geetas.model.TtsRequest
import com.psdk.geetas.model.TtsResponse
import java.util.Base64

class GeminiService {

    companion object {
        const val DEFAULT_TTS_MODEL = "gemini-3.8-flash-tts"
        const val DEFAULT_STT_MODEL = "gemini-3.5-transcribe"
        const val ENV_API_KEY_NAME = "GEETAS_GEMINI_API_KEY"
    }

    private val apiKey: String? = System.getenv(ENV_API_KEY_NAME)
        ?: System.getenv("GEMINI_API_KEY")

    val isApiKeyConfigured: Boolean
        get() = !apiKey.isNullOrBlank()

    private fun getClient(): Client {
        val key = apiKey
        if (key.isNullOrBlank()) {
            throw IllegalStateException(
                "Gemini API key is not configured. Please set the '$ENV_API_KEY_NAME' environment variable."
            )
        }
        return Client(apiKey = key)
    }

    suspend fun generateSpeech(request: TtsRequest): TtsResponse {
        val client = getClient()
        val modelToUse = request.model?.takeIf { it.isNotBlank() } ?: DEFAULT_TTS_MODEL

        val prompt = if (!request.voice.isNullOrBlank()) {
            "Synthesize speech for the following text using voice '${request.voice}':\n${request.text}"
        } else {
            request.text
        }

        val content = Content.fromText(prompt)

        val response = client.models.generateContent(
            model = modelToUse,
            contents = listOf(content)
        )

        var audioBase64: String? = null
        var returnedMimeType: String? = "audio/mp3"

        response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
            part.inlineData?.data?.let { bytes ->
                audioBase64 = Base64.getEncoder().encodeToString(bytes)
                returnedMimeType = part.inlineData?.mimeType ?: "audio/mp3"
            }
        }

        return TtsResponse(
            text = response.text,
            audioBase64 = audioBase64,
            mimeType = returnedMimeType,
            modelUsed = modelToUse
        )
    }

    suspend fun transcribeSpeech(request: SttRequest): SttResponse {
        val client = getClient()
        val modelToUse = request.model?.takeIf { it.isNotBlank() } ?: DEFAULT_STT_MODEL

        val audioBytes = try {
            Base64.getDecoder().decode(request.audioBase64.trim())
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid Base64 string provided for audio: ${e.message}")
        }

        val audioPart = Part(
            inlineData = Blob(
                data = audioBytes,
                mimeType = request.mimeType.ifBlank { "audio/wav" }
            )
        )

        val promptText = request.prompt?.takeIf { it.isNotBlank() }
            ?: "Transcribe the given audio recording accurately."

        val promptPart = Part(text = promptText)

        val content = Content(
            parts = listOf(audioPart, promptPart),
            role = "user"
        )

        val response = client.models.generateContent(
            model = modelToUse,
            contents = listOf(content)
        )

        val transcriptionText = response.text ?: ""

        return SttResponse(
            text = transcriptionText,
            modelUsed = modelToUse
        )
    }
}
