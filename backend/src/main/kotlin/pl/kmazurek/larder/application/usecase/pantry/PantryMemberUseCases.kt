package pl.kmazurek.larder.application.usecase.pantry

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.larder.application.dto.PantryDto
import pl.kmazurek.larder.application.dto.PantryMemberDto
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.repository.ProductFilter
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.UserRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy

/**
 * Use Case: members of a pantry (visible to every member), owner first
 */
@Service
class ListPantryMembers(
    private val pantryRepository: PantryRepository,
    private val userRepository: UserRepository,
    private val pantryAccessGuard: PantryAccessGuard,
) {
    fun execute(
        userId: UserId,
        pantryId: PantryId,
    ): List<PantryMemberDto> {
        pantryAccessGuard.requireAccess(userId, pantryId)
        return pantryRepository.findMembers(pantryId)
            .mapNotNull { member ->
                userRepository.findById(member.userId)?.let { user ->
                    PantryMemberDto(user.id.toString(), user.name, user.email.value, member.role)
                }
            }
            .sortedWith(compareBy({ if (it.role == PantryRole.OWNER) 0 else 1 }, { it.name.lowercase() }))
    }
}

/**
 * Use Case (owner): add an existing, active user to the pantry by e-mail.
 * A pantry of their own that they never used (no products, nobody else in it) is removed on the way: it would only
 * stand between them and the household's pantry they are joining.
 */
@Service
class InvitePantryMember(
    private val pantryRepository: PantryRepository,
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val accessPolicy: PantryAccessPolicy,
) {
    @Transactional
    fun execute(
        ownerId: UserId,
        pantryId: PantryId,
        email: String,
    ): PantryMemberDto {
        requireOwner(pantryAccessGuard, accessPolicy, ownerId, pantryId)
        val user =
            userRepository.findByEmail(Email(email.trim()))?.takeIf { it.isActive }
                ?: throw PantryMemberNotFoundException("No active account with this e-mail")
        if (pantryRepository.findMemberRole(pantryId, user.id) != null) {
            throw AlreadyPantryMemberException("${user.name} is already a member of this pantry")
        }
        pantryRepository.addMember(PantryMember(pantryId, user.id, PantryRole.MEMBER))
        removeUnusedOwnPantries(user.id, joined = pantryId)
        return PantryMemberDto(user.id.toString(), user.name, user.email.value, PantryRole.MEMBER)
    }

    private fun removeUnusedOwnPantries(
        userId: UserId,
        joined: PantryId,
    ) {
        pantryRepository.findByMember(userId)
            .filter { it.ownerId == userId && it.id != joined }
            .filter { pantryRepository.findMembers(it.id).size == 1 }
            .filter { productRepository.findByPantry(it.id, ProductFilter(includeArchived = true)).isEmpty() }
            .forEach { pantryRepository.delete(it.id) }
    }
}

/**
 * Use Case: the owner removes a member, or a member leaves. What they added stays in the pantry.
 */
@Service
class RemovePantryMember(
    private val pantryRepository: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val accessPolicy: PantryAccessPolicy,
) {
    fun execute(
        requesterId: UserId,
        pantryId: PantryId,
        memberId: UserId,
    ) {
        pantryAccessGuard.requireAccess(requesterId, pantryId)
        val memberRole =
            pantryRepository.findMemberRole(pantryId, memberId)
                ?: throw PantryMemberNotFoundException("This user is not a member of the pantry")
        if (!accessPolicy.canRemoveMember(requesterId, pantryId, memberId)) {
            throw CannotRemovePantryMemberException(
                if (memberRole == PantryRole.OWNER) "The owner cannot leave or be removed" else "Only the owner can remove other members",
            )
        }
        pantryRepository.removeMember(pantryId, memberId)
    }
}

/**
 * Use Case (owner): rename the pantry, e.g. "Spiżarnia rodzinna"
 */
@Service
class RenamePantry(
    private val pantryRepository: PantryRepository,
    private val pantryAccessGuard: PantryAccessGuard,
    private val accessPolicy: PantryAccessPolicy,
) {
    fun execute(
        ownerId: UserId,
        pantryId: PantryId,
        name: String,
    ): PantryDto {
        val pantry = requireOwner(pantryAccessGuard, accessPolicy, ownerId, pantryId)
        val saved = pantryRepository.save(pantry.rename(name))
        return PantryDto.fromDomain(saved, PantryRole.OWNER, pantryRepository.findMembers(pantryId).size)
    }
}
