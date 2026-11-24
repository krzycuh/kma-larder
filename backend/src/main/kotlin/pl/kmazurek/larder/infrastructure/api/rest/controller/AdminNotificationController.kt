package pl.kmazurek.larder.infrastructure.api.rest.controller

import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.larder.application.usecase.notification.SendExpiryDigests
import pl.kmazurek.larder.infrastructure.logging.AuditLogger

/**
 * Admin: send the expiry digest now (to check the ntfy set-up without waiting for Sunday)
 */
@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
class AdminNotificationController(
    private val sendExpiryDigests: SendExpiryDigests,
    private val auditLogger: AuditLogger,
) {
    @PostMapping("/digest")
    fun sendDigest(): ResponseEntity<Map<String, Int>> {
        val sent = sendExpiryDigests.execute()
        auditLogger.log("DIGEST_SENT_MANUALLY", details = mapOf("sent" to sent))
        return ResponseEntity.ok(mapOf("sent" to sent))
    }
}
