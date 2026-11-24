package pl.kmazurek.larder.application.service

/**
 * Port: pushes a message to the household (ntfy on the Pi, or nothing).
 */
interface NotificationService {
    fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    )

    /**
     * A notification for the household: title, body, priority 1 (min) to 5 (max), emoji tags.
     * Returns false when nothing was sent (not configured, or the push failed); never throws.
     */
    fun notify(
        title: String,
        message: String,
        priority: Int = 3,
        tags: List<String> = emptyList(),
    ): Boolean
}
