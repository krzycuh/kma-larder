package pl.kmazurek.larder.infrastructure.notification

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.client.RestClient

class NtfyNotificationServiceTest {
    private lateinit var server: MockWebServer

    @BeforeEach
    fun setup() {
        server = MockWebServer()
        server.start()
    }

    @AfterEach
    fun tearDown() = server.shutdown()

    private fun service(token: String?) =
        NtfyNotificationService(RestClient.builder().baseUrl(server.url("/").toString().trimEnd('/')).build(), "kma-larder", token)

    @Test
    fun `posts the message to the topic with title, priority, tags and token`() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        assertTrue(service("tk_secret").notify("Spiżarnia: 2 partie po terminie", "- 2 × Fasola", priority = 4, tags = listOf("jar")))

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/kma-larder", request.path)
        assertEquals("- 2 × Fasola", request.body.readUtf8())
        assertEquals("=?UTF-8?B?U3Bpxbxhcm5pYTogMiBwYXJ0aWUgcG8gdGVybWluaWU=?=", request.getHeader("Title"))
        assertEquals("4", request.getHeader("Priority"))
        assertEquals("jar", request.getHeader("Tags"))
        assertEquals("Bearer tk_secret", request.getHeader("Authorization"))
    }

    @Test
    fun `ascii titles are sent as they are and no token means no auth header`() {
        server.enqueue(MockResponse().setResponseCode(200))

        service(null).notify("KMA Larder: test", "ok")

        val request = server.takeRequest()
        assertEquals("KMA Larder: test", request.getHeader("Title"))
        assertEquals(null, request.getHeader("Authorization"))
    }

    @Test
    fun `a failing server is reported as not sent, not thrown`() {
        server.enqueue(MockResponse().setResponseCode(403))
        assertFalse(service("tk").notify("x", "y"))
    }
}
