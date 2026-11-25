package pl.kmazurek.larder.infrastructure.notification

import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import pl.kmazurek.larder.application.service.NotificationService

/**
 * ntfy adapter: POST {url}/{topic} with the message as the body and the title, priority and tags as headers
 * (https://docs.ntfy.sh/publish/). A token is sent when configured (ntfy with auth-default-access: deny-all).
 */
class NtfyNotificationService(
    private val restClient: RestClient,
    private val topic: String,
    private val token: String?,
) : NotificationService {
    private val logger = LoggerFactory.getLogger(NtfyNotificationService::class.java)

    override fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    ) {
        notify("KMA Larder: nowe konto", "$userName <$userEmail>", priority = 2, tags = listOf("bust_in_silhouette"))
    }

    override fun notify(
        title: String,
        message: String,
        priority: Int,
        tags: List<String>,
    ): Boolean =
        try {
            restClient.post()
                .uri("/{topic}", topic)
                .contentType(MediaType(MediaType.TEXT_PLAIN, Charsets.UTF_8))
                .headers { headers ->
                    // ntfy reads headers as ASCII; the title keeps Polish letters via RFC 2047 (ntfy decodes it)
                    headers.set("Title", encodeHeader(title))
                    headers.set("Priority", priority.coerceIn(1, 5).toString())
                    if (tags.isNotEmpty()) headers.set("Tags", tags.joinToString(","))
                    headers.set("Markdown", "yes")
                    token?.takeIf { it.isNotBlank() }?.let { headers.setBearerAuth(it) }
                }
                .body(message.toByteArray(Charsets.UTF_8))
                .retrieve()
                .toBodilessEntity()
            logger.info("ntfy: sent '{}' to topic {}", title, topic)
            true
        } catch (ex: RestClientException) {
            logger.warn("ntfy: could not send '{}' to topic {}: {}", title, topic, ex.message)
            false
        }

    companion object {
        /** RFC 2047 "encoded word" for a header value with non-ASCII characters; plain ASCII stays as is. */
        fun encodeHeader(value: String): String =
            if (value.all { it.code < 128 }) {
                value
            } else {
                "=?UTF-8?B?" + java.util.Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8)) + "?="
            }
    }
}
