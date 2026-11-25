package pl.kmazurek.larder.domain.service

import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.PantryRepository

/**
 * Domain service: the single place deciding who may read/modify a pantry.
 * Any member may use the pantry (products, batches, consumption); only the owner manages members and the pantry itself.
 */
class PantryAccessPolicy(
    private val pantryRepository: PantryRepository,
) {
    fun canAccess(
        userId: UserId,
        pantryId: PantryId,
    ): Boolean = pantryRepository.findMemberRole(pantryId, userId) != null

    fun canManageMembers(
        userId: UserId,
        pantryId: PantryId,
    ): Boolean = pantryRepository.findMemberRole(pantryId, userId) == PantryRole.OWNER

    /**
     * The owner may remove anyone but themselves; a member may only leave (remove themselves).
     * The owner can never be removed: the pantry would be left without anyone managing it.
     */
    fun canRemoveMember(
        requesterId: UserId,
        pantryId: PantryId,
        memberId: UserId,
    ): Boolean {
        val memberRole = pantryRepository.findMemberRole(pantryId, memberId) ?: return false
        if (memberRole == PantryRole.OWNER) return false
        return requesterId == memberId || canManageMembers(requesterId, pantryId)
    }
}
