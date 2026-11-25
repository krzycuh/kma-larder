package pl.kmazurek.larder.infrastructure.persistence.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "pantries")
class PantryJpaEntity(
    @Id
    @Column(columnDefinition = "UUID")
    var id: UUID = UUID.randomUUID(),
    @Column(nullable = false, length = 255)
    var name: String,
    @Column(name = "owner_id", nullable = false, columnDefinition = "UUID")
    var ownerId: UUID,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)

@Embeddable
class PantryMemberKey(
    @Column(name = "pantry_id", columnDefinition = "UUID")
    var pantryId: UUID = UUID.randomUUID(),
    @Column(name = "user_id", columnDefinition = "UUID")
    var userId: UUID = UUID.randomUUID(),
) : Serializable {
    override fun equals(other: Any?): Boolean = other is PantryMemberKey && other.pantryId == pantryId && other.userId == userId

    override fun hashCode(): Int = 31 * pantryId.hashCode() + userId.hashCode()
}

@Entity
@Table(name = "pantry_members")
class PantryMemberJpaEntity(
    @EmbeddedId
    var key: PantryMemberKey,
    @Column(nullable = false, length = 20)
    var role: String,
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),
)
