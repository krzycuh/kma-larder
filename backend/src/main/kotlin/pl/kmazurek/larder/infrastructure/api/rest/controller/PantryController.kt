package pl.kmazurek.larder.infrastructure.api.rest.controller

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pl.kmazurek.larder.application.dto.InvitePantryMemberRequest
import pl.kmazurek.larder.application.dto.PantryDto
import pl.kmazurek.larder.application.dto.PantryMemberDto
import pl.kmazurek.larder.application.dto.RenamePantryRequest
import pl.kmazurek.larder.application.usecase.pantry.InvitePantryMember
import pl.kmazurek.larder.application.usecase.pantry.ListMyPantries
import pl.kmazurek.larder.application.usecase.pantry.ListPantryMembers
import pl.kmazurek.larder.application.usecase.pantry.RemovePantryMember
import pl.kmazurek.larder.application.usecase.pantry.RenamePantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.infrastructure.logging.AuditLogger

/**
 * REST Controller for pantries and their members
 */
@RestController
@RequestMapping("/api/pantries")
class PantryController(
    private val listMyPantries: ListMyPantries,
    private val listPantryMembers: ListPantryMembers,
    private val invitePantryMember: InvitePantryMember,
    private val removePantryMember: RemovePantryMember,
    private val renamePantry: RenamePantry,
    private val auditLogger: AuditLogger,
) {
    @GetMapping
    fun myPantries(authentication: Authentication): ResponseEntity<List<PantryDto>> =
        ResponseEntity.ok(listMyPantries.summaries(authentication.userId()))

    @PatchMapping("/{pantryId}")
    fun rename(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @Valid @RequestBody request: RenamePantryRequest,
    ): ResponseEntity<PantryDto> {
        val dto = renamePantry.execute(authentication.userId(), PantryId.fromString(pantryId), request.name)
        auditLogger.log("PANTRY_RENAMED", details = mapOf("pantryId" to pantryId))
        return ResponseEntity.ok(dto)
    }

    @GetMapping("/{pantryId}/members")
    fun members(
        authentication: Authentication,
        @PathVariable pantryId: String,
    ): ResponseEntity<List<PantryMemberDto>> =
        ResponseEntity.ok(listPantryMembers.execute(authentication.userId(), PantryId.fromString(pantryId)))

    @PostMapping("/{pantryId}/members")
    fun invite(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @Valid @RequestBody request: InvitePantryMemberRequest,
    ): ResponseEntity<PantryMemberDto> {
        val member = invitePantryMember.execute(authentication.userId(), PantryId.fromString(pantryId), request.email)
        auditLogger.log("PANTRY_MEMBER_ADDED", details = mapOf("pantryId" to pantryId, "userId" to member.userId))
        return ResponseEntity.status(HttpStatus.CREATED).body(member)
    }

    /**
     * Owner removes a member, or a member leaves (userId = self).
     */
    @DeleteMapping("/{pantryId}/members/{userId}")
    fun removeMember(
        authentication: Authentication,
        @PathVariable pantryId: String,
        @PathVariable userId: String,
    ): ResponseEntity<Void> {
        removePantryMember.execute(authentication.userId(), PantryId.fromString(pantryId), UserId.fromString(userId))
        auditLogger.log("PANTRY_MEMBER_REMOVED", details = mapOf("pantryId" to pantryId, "userId" to userId))
        return ResponseEntity.noContent().build()
    }
}
