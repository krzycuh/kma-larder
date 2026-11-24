package pl.kmazurek.larder.infrastructure.notification

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.service.NotificationService

/**
 * Default notification adapter: logs only. Replace with ntfy/e-mail when needed.
 */
@Service
class NoOpNotificationService : NotificationService {
    private val logger = LoggerFactory.getLogger(NoOpNotificationService::class.java)

    override fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    ) {
        logger.info("New user registered: {} <{}>", userName, userEmail)
    }
}
