package com.psdk.geetas.service

import com.google.genai.kotlin.Client
import com.google.genai.kotlin.types.Blob
import com.google.genai.kotlin.types.Content
import com.google.genai.kotlin.types.GenerateContentConfig
import com.google.genai.kotlin.types.MultiSpeakerVoiceConfig
import com.google.genai.kotlin.types.Part
import com.google.genai.kotlin.types.PrebuiltVoiceConfig
import com.google.genai.kotlin.types.ReplicatedVoiceConfig
import com.google.genai.kotlin.types.SpeakerVoiceConfig
import com.google.genai.kotlin.types.SpeechConfig
import com.google.genai.kotlin.types.SpeechMetadata
import com.google.genai.kotlin.types.VoiceConfig
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

    suspend fun generateSingleSpeakerSpeech(request: TtsRequest): TtsResponse {
        val client = getClient()
        val modelToUse = request.model?.takeIf { it.isNotBlank() } ?: DEFAULT_TTS_MODEL

        val prompt = request.text
        val content = Content.fromText(prompt)
        val speechMetadata = Content(
            parts = listOf(Part(speechMetadata = SpeechMetadata(
                style = request.style
            )))
        )
        val generationConfig =
            GenerateContentConfig(
                speechConfig = SpeechConfig(
                voiceConfig = VoiceConfig(prebuiltVoiceConfig = PrebuiltVoiceConfig(
                 voiceName = request.voice
                )
                ),
                    languageCode = request.languageCode
            ))

        val response = client.models.generateContent(
            model = modelToUse,
            contents = listOf(content,speechMetadata),
            config = generationConfig
        )

        var audioBase64: String? = null
        var returnedMimeType: String? = "audio/wav"

        response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
            part.inlineData?.data?.let { bytes ->
                audioBase64 = Base64.getEncoder().encodeToString(bytes)
                returnedMimeType = part.inlineData?.mimeType ?: "audio/wav"
            }
        }

        return TtsResponse(
            text = response.text,
            audioBase64 = audioBase64,
            mimeType = returnedMimeType,
            modelUsed = modelToUse
        )
    }

    suspend fun generateMultiSpeakerSpeech(request: List<TtsRequest>): TtsResponse {
        val client = getClient()
        val modelToUse = DEFAULT_TTS_MODEL

        val speechMetadata = createMultiSpeechMetaDataParts(request)
        val speakerVoiceConfigs = createMultiSpeechConfig(request)

        val speechData = Content(
            parts = speechMetadata
        )
        val generateContentConfig = GenerateContentConfig(
            speechConfig = SpeechConfig(
                multiSpeakerVoiceConfig = MultiSpeakerVoiceConfig(
                    speakerVoiceConfigs = speakerVoiceConfigs
                )
            )
        )

        val response = client.models.generateContent(
            model = modelToUse,
            contents = listOf(speechData),
            config = generateContentConfig
        )

        var audioBase64: String? = null
        var returnedMimeType: String? = "audio/wav"

        response.candidates?.firstOrNull()?.content?.parts?.forEach { part ->
            part.inlineData?.data?.let { bytes ->
                audioBase64 = Base64.getEncoder().encodeToString(bytes)
                returnedMimeType = part.inlineData?.mimeType
            }
        }

        return TtsResponse(
            text = response.text,
            audioBase64 = audioBase64,
            mimeType = returnedMimeType,
            modelUsed = modelToUse
        )
    }

    private fun createMultiSpeechMetaDataParts(
        list: List<TtsRequest>
    ): List<Part>? {
        val partList: MutableList<Part>? = mutableListOf()

        list.forEach {
            partList?.add(
                Part(
                    text = it.text
                )
            )
            partList?.add(
                Part(
                    speechMetadata = SpeechMetadata (
                        speaker = it.speaker,
                        style = it.voice
                    )
                )
            )
        }
        return partList?.toList()
    }

    private fun createMultiSpeechConfig(
        list: List<TtsRequest>
    ): List<SpeakerVoiceConfig>? {
        val configList: MutableList<SpeakerVoiceConfig>? = mutableListOf()

        list.forEach {
            configList?.add(
                SpeakerVoiceConfig(
                    speaker = it.speaker,
                    voiceConfig = VoiceConfig(
                        prebuiltVoiceConfig = PrebuiltVoiceConfig(
                            voiceName = it.voice
                        )
                    )
                )
            )
        }
        return configList?.toList()
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

