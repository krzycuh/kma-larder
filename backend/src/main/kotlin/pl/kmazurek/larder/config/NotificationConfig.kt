package pl.kmazurek.larder.config

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.client.RestClient
import pl.kmazurek.larder.application.service.NotificationService
import pl.kmazurek.larder.infrastructure.notification.NoOpNotificationService
import pl.kmazurek.larder.infrastructure.notification.NtfyNotificationService
import java.net.ProxySelector
import java.net.http.HttpClient
import java.time.Duration

/**
 * Household notifications over ntfy: app.notifications.ntfy.url + topic (+ token). Without a URL nothing is sent.
 */
@Configuration
@EnableScheduling
class NotificationConfig(
    @Value("\${app.notifications.ntfy.url:}") private val url: String,
    @Value("\${app.notifications.ntfy.topic:kma-larder}") private val topic: String,
    @Value("\${app.notifications.ntfy.token:}") private val token: String,
    @Value("\${app.notifications.ntfy.timeout:5s}") private val timeout: Duration,
) {
    private val logger = LoggerFactory.getLogger(NotificationConfig::class.java)

    @Bean
    fun notificationService(restClientBuilder: RestClient.Builder): NotificationService {
        val baseUrl = url.trim().trimEnd('/')
        if (baseUrl.isEmpty()) {
            logger.info("Notifications: ntfy not configured (APP_NTFY_URL empty), nothing will be sent")
            return NoOpNotificationService()
        }
        logger.info("Notifications: ntfy at {} topic {} (token {})", baseUrl, topic, if (token.isBlank()) "none" else "set")
        val httpClient =
            HttpClient.newBuilder()
                .connectTimeout(timeout)
                .proxy(ProxySelector.getDefault())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        val requestFactory = JdkClientHttpRequestFactory(httpClient).apply { setReadTimeout(timeout) }
        val restClient = restClientBuilder.clone().baseUrl(baseUrl).requestFactory(requestFactory).build()
        return NtfyNotificationService(restClient, topic.trim(), token.trim().takeIf { it.isNotEmpty() })
    }
}
