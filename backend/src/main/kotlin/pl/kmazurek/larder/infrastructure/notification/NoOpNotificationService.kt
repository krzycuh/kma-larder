package pl.kmazurek.larder.infrastructure.notification

import org.slf4j.LoggerFactory
import pl.kmazurek.larder.application.service.NotificationService

/**
 * Default notification adapter: logs only. Used when ntfy is not configured.
 */
class NoOpNotificationService : NotificationService {
    private val logger = LoggerFactory.getLogger(NoOpNotificationService::class.java)

    override fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    ) {
        logger.info("New user registered: {} <{}>", userName, userEmail)
    }

    override fun notify(
        title: String,
        message: String,
        priority: Int,
        tags: List<String>,
    ): Boolean {
        logger.info("Notification (not sent, ntfy not configured): {} – {}", title, message.lineSequence().firstOrNull())
        return false
    }
}
