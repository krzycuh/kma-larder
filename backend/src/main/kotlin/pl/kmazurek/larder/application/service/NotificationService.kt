package pl.kmazurek.larder.application.service

interface NotificationService {
    fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    )
}
