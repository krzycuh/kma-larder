package pl.kmazurek.larder.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import java.time.LocalDateTime

data class PantryDto(
    val id: String,
    val name: String,
    val ownerId: String,
    val createdAt: LocalDateTime,
    /** Role of the current user in this pantry */
    val myRole: PantryRole,
    val memberCount: Int,
    /** The pantry the current user's pages open on; exactly one of the user's pantries has it */
    val isDefault: Boolean = false,
) {
    companion object {
        fun fromDomain(
            pantry: Pantry,
            myRole: PantryRole,
            memberCount: Int,
            isDefault: Boolean = false,
        ) = PantryDto(
            id = pantry.id.toString(),
            name = pantry.name,
            ownerId = pantry.ownerId.toString(),
            createdAt = pantry.createdAt,
            myRole = myRole,
            memberCount = memberCount,
            isDefault = isDefault,
        )
    }
}

data class PantryMemberDto(
    val userId: String,
    val name: String,
    val email: String,
    val role: PantryRole,
)

data class InvitePantryMemberRequest(
    @field:NotBlank(message = "E-mail is required")
    @field:Size(max = 255, message = "E-mail cannot exceed 255 characters")
    val email: String,
)

data class CreatePantryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,
)

data class RenamePantryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name cannot exceed 255 characters")
    val name: String,
)
