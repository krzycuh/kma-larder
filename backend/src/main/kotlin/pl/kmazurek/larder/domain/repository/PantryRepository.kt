package pl.kmazurek.larder.domain.repository

import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.UserId

/**
 * Repository interface (port) for the Pantry aggregate and its membership
 */
interface PantryRepository {
    fun findById(id: PantryId): Pantry?

    fun findByMember(userId: UserId): List<Pantry>

    fun save(pantry: Pantry): Pantry

    fun findMemberRole(
        pantryId: PantryId,
        userId: UserId,
    ): PantryRole?

    fun addMember(member: PantryMember)

    fun findMembers(pantryId: PantryId): List<PantryMember>

    fun removeMember(
        pantryId: PantryId,
        userId: UserId,
    )
}
