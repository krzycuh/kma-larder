package pl.kmazurek.larder.application.usecase.pantry

import org.springframework.stereotype.Service
import pl.kmazurek.larder.application.dto.PantryDto
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy

/**
 * Use Case: pantries the current user is a member of
 */
@Service
class ListMyPantries(
    private val pantryRepository: PantryRepository,
) {
    fun execute(userId: UserId): List<Pantry> = pantryRepository.findByMember(userId).sortedBy { it.createdAt }

    /**
     * Same pantries with the user's role and the member count; the user's own pantry first.
     */
    fun summaries(userId: UserId): List<PantryDto> =
        execute(userId)
            .map { pantry ->
                val members = pantryRepository.findMembers(pantry.id)
                val myRole = members.firstOrNull { it.userId == userId }?.role ?: PantryRole.MEMBER
                PantryDto.fromDomain(pantry, myRole, members.size)
            }
            .sortedBy { if (it.myRole == PantryRole.OWNER) 0 else 1 }
}

/**
 * Shared guard: loads a pantry the user may access, or fails with 404 / 403
 */
@Service
class PantryAccessGuard(
    private val pantryRepository: PantryRepository,
    private val accessPolicy: PantryAccessPolicy,
) {
    fun requireAccess(
        userId: UserId,
        pantryId: PantryId,
    ): Pantry {
        val pantry = pantryRepository.findById(pantryId) ?: throw PantryNotFoundException("Pantry not found")
        if (!accessPolicy.canAccess(userId, pantryId)) {
            throw PantryAccessDeniedException("You are not a member of this pantry")
        }
        return pantry
    }
}
