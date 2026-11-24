package pl.kmazurek.larder.infrastructure.persistence

import org.springframework.stereotype.Component
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryId
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.infrastructure.persistence.entity.PantryMemberKey
import pl.kmazurek.larder.infrastructure.persistence.mapper.PantryMapper

@Component
class PantryRepositoryImpl(
    private val jpaRepository: JpaPantryRepository,
    private val memberRepository: JpaPantryMemberRepository,
) : PantryRepository {
    override fun findById(id: PantryId): Pantry? = jpaRepository.findById(id.value).map { PantryMapper.toDomain(it) }.orElse(null)

    override fun findAll(): List<Pantry> = jpaRepository.findAll().map { PantryMapper.toDomain(it) }.sortedBy { it.createdAt }

    override fun findByMember(userId: UserId): List<Pantry> = jpaRepository.findByMember(userId.value).map { PantryMapper.toDomain(it) }

    override fun save(pantry: Pantry): Pantry = PantryMapper.toDomain(jpaRepository.save(PantryMapper.toJpa(pantry)))

    override fun findMemberRole(
        pantryId: PantryId,
        userId: UserId,
    ): PantryRole? =
        memberRepository.findById(PantryMemberKey(pantryId.value, userId.value))
            .map { PantryRole.valueOf(it.role) }
            .orElse(null)

    override fun addMember(member: PantryMember) {
        memberRepository.save(PantryMapper.memberToJpa(member))
    }

    override fun findMembers(pantryId: PantryId): List<PantryMember> =
        memberRepository.findAllByKeyPantryIdOrderByCreatedAtAsc(pantryId.value).map { PantryMapper.memberToDomain(it) }

    override fun removeMember(
        pantryId: PantryId,
        userId: UserId,
    ) = memberRepository.deleteById(PantryMemberKey(pantryId.value, userId.value))
}
