package pl.kmazurek.larder.infrastructure.persistence.mapper

import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.HashedPassword
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.model.user.UserRole
import pl.kmazurek.larder.infrastructure.persistence.entity.UserJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.UserRoleJpa

/**
 * Mapper between domain User and JPA entity (anti-corruption layer)
 */
object UserMapper {
    fun toDomain(jpa: UserJpaEntity): User {
        return User(
            id = UserId(jpa.id),
            email = Email(jpa.email),
            passwordHash = HashedPassword(jpa.passwordHash),
            name = jpa.name,
            role = toDomainRole(jpa.role),
            avatarUrl = jpa.avatarUrl,
            isActive = jpa.isActive,
            createdAt = jpa.createdAt,
            updatedAt = jpa.updatedAt,
        )
    }

    fun toJpa(domain: User): UserJpaEntity {
        return UserJpaEntity(
            id = domain.id.value,
            email = domain.email.value,
            passwordHash = domain.passwordHash.value,
            name = domain.name,
            role = toJpaRole(domain.role),
            avatarUrl = domain.avatarUrl,
            isActive = domain.isActive,
            createdAt = domain.createdAt,
            updatedAt = domain.updatedAt,
        )
    }

    private fun toDomainRole(jpaRole: UserRoleJpa): UserRole =
        when (jpaRole) {
            UserRoleJpa.USER -> UserRole.USER
            UserRoleJpa.ADMIN -> UserRole.ADMIN
        }

    private fun toJpaRole(domainRole: UserRole): UserRoleJpa =
        when (domainRole) {
            UserRole.USER -> UserRoleJpa.USER
            UserRole.ADMIN -> UserRoleJpa.ADMIN
        }
}
