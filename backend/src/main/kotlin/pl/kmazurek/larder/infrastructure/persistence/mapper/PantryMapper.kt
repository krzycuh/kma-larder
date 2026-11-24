package pl.kmazurek.larder.infrastructure.persistence.mapper

import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryMemberJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryMemberKey

object PantryMapper {
    fun toDomain(jpa: PantryJpaEntity) =
        Pantry(
            id = PantryId(jpa.id),
            name = jpa.name,
            ownerId = UserId(jpa.ownerId),
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )

    fun toJpa(domain: Pantry) =
        PantryJpaEntity(
            id = domain.id.value,
            name = domain.name,
            ownerId = domain.ownerId.value,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )

    fun memberToDomain(jpa: PantryMemberJpaEntity) =
        PantryMember(
            pantryId = PantryId(jpa.key.pantryId),
            userId = UserId(jpa.key.userId),
            role = PantryRole.valueOf(jpa.role),
        )

    fun memberToJpa(domain: PantryMember) =
        PantryMemberJpaEntity(
            key = PantryMemberKey(domain.pantryId.value, domain.userId.value),
            role = domain.role.name,
        )
}
