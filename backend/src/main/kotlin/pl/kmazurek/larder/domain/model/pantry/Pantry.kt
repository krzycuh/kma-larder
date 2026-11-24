package pl.kmazurek.larder.domain.model.pantry

import pl.kmazurek.larder.domain.model.user.UserId
import java.time.LocalDateTime

/**
 * Pantry Aggregate Root (Pantry context)
 * A household pantry that has an owner and members. In MVP every user owns exactly one pantry;
 * family pantries only add members.
 */
data class Pantry(
    val id: PantryId,
    val name: String,
    val ownerId: UserId,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    init {
        require(name.isNotBlank()) { "Pantry name cannot be blank" }
        require(name.length <= 255) { "Pantry name cannot exceed 255 characters" }
    }

    fun rename(name: String): Pantry = copy(name = name.trim(), updatedAt = LocalDateTime.now())

    companion object {
        fun create(
            name: String,
            ownerId: UserId,
        ): Pantry {
            val now = LocalDateTime.now()
            return Pantry(
                id = PantryId.generate(),
                name = name.trim(),
                ownerId = ownerId,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}

enum class PantryRole {
    OWNER,
    MEMBER,
}

data class PantryMember(
    val pantryId: PantryId,
    val userId: UserId,
    val role: PantryRole,
)
