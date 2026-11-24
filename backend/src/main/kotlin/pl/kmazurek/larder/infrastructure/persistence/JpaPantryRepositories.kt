package pl.kmazurek.larder.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryMemberJpaEntity
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryMemberKey
import java.util.UUID

@Repository
interface JpaPantryRepository : JpaRepository<PantryJpaEntity, UUID> {
    @Query(
        """
        SELECT l FROM PantryJpaEntity l
        WHERE l.id IN (SELECT m.key.pantryId FROM PantryMemberJpaEntity m WHERE m.key.userId = :userId)
        ORDER BY l.createdAt ASC
        """,
    )
    fun findByMember(
        @Param("userId") userId: UUID,
    ): List<PantryJpaEntity>
}

@Repository
interface JpaPantryMemberRepository : JpaRepository<PantryMemberJpaEntity, PantryMemberKey> {
    fun findAllByKeyPantryIdOrderByCreatedAtAsc(pantryId: UUID): List<PantryMemberJpaEntity>
}
