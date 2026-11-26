package pl.kmazurek.larder.application.usecase.pantry

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.larder.application.dto.PantryDto
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryMember
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
     * Same pantries with the user's role and the member count; the default pantry first, then the oldest.
     * The default is the one the user picked, or the oldest pantry while they picked none (or left the picked one).
     */
    fun summaries(userId: UserId): List<PantryDto> {
        val pantries = execute(userId)
        val memberships = pantryRepository.findMemberships(userId).associateBy { it.pantryId }
        val defaultId = memberships.values.firstOrNull { it.isDefault }?.pantryId ?: pantries.firstOrNull()?.id
        return pantries
            .map { pantry ->
                PantryDto.fromDomain(
                    pantry = pantry,
                    myRole = memberships[pantry.id]?.role ?: PantryRole.MEMBER,
                    memberCount = pantryRepository.findMembers(pantry.id).size,
                    isDefault = pantry.id == defaultId,
                )
            }
            .sortedBy { if (it.isDefault) 0 else 1 }
    }
}

/**
 * Use Case: a user creates a pantry and becomes its owner. Nobody gets one automatically: a new account either
 * creates the household's pantry or is added to an existing one.
 */
@Service
class CreatePantry(
    private val pantryRepository: PantryRepository,
) {
    @Transactional
    fun execute(
        userId: UserId,
        name: String,
    ): PantryDto {
        val isFirst = pantryRepository.findMemberships(userId).isEmpty()
        val pantry = pantryRepository.save(Pantry.create(name, userId))
        pantryRepository.addMember(PantryMember(pantry.id, userId, PantryRole.OWNER))
        return PantryDto.fromDomain(pantry, PantryRole.OWNER, memberCount = 1, isDefault = isFirst)
    }
}

/**
 * Use Case (owner): delete the pantry with its products, batches and history, for every member.
 */
@Service
class DeletePantry(
    private val pantryRepository: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val accessPolicy: PantryAccessPolicy,
) {
    fun execute(
        ownerId: UserId,
        pantryId: PantryId,
    ) {
        requireOwner(pantryAccessGuard, accessPolicy, ownerId, pantryId)
        pantryRepository.delete(pantryId)
    }
}

/**
 * Use Case: any member picks the pantry their pages open on. A personal choice, other members keep theirs.
 */
@Service
class SetDefaultPantry(
    private val pantryRepository: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
    ) {
        pantryAccessGuard.requireAccess(userId, pantryId)
        pantryRepository.setDefault(userId, pantryId)
    }
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

internal fun requireOwner(
    guard: PantryAccessGuard,
    accessPolicy: PantryAccessPolicy,
    userId: UserId,
    pantryId: PantryId,
): Pantry {
    val pantry = guard.requireAccess(userId, pantryId)
    if (!accessPolicy.canManageMembers(userId, pantryId)) {
        throw PantryAccessDeniedException("Only the owner can manage this pantry")
    }
    return pantry
}
