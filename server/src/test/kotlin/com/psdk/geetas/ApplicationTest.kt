package com.psdk.geetas

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ApplicationTest {

    @Test
    fun testHealthEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("GeeTAS Backend API"))
        assertTrue(responseText.contains("gemini-3.8-flash-tts"))
        assertTrue(responseText.contains("gemini-3.5-transcribe"))
    }

    @Test
    fun testRootEndpoint() = testApplication {
        application {
            module()
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        val responseText = response.bodyAsText()
        assertTrue(responseText.contains("GeeTAS Backend API"))
    }

    @Test
    fun testTtsEndpointEmptyTextReturnsBadRequest() = testApplication {
        application {
            module()
        }
        val response = client.post("/api/tts") {
            contentType(ContentType.Application.Json)
            setBody("""{"text": "   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("Text parameter cannot be empty"))
    }

    @Test
    fun testSttEndpointEmptyAudioReturnsBadRequest() = testApplication {
        application {
            module()
        }
        val response = client.post("/api/stt") {
            contentType(ContentType.Application.Json)
            setBody("""{"audioBase64": "   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("audioBase64 parameter cannot be empty"))
    }
}
