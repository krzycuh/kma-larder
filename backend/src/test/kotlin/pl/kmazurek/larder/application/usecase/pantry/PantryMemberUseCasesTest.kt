package pl.kmazurek.larder.application.usecase.pantry

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.HashedPassword
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.model.user.UserId
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.repository.ProductRepository
import pl.kmazurek.larder.domain.repository.UserRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy

class PantryMemberUseCasesTest {
    private lateinit var pantryRepository: PantryRepository
    private lateinit var userRepository: UserRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var guard: PantryAccessGuard
    private lateinit var policy: PantryAccessPolicy

    private val owner = User.create(Email("anna@example.com"), HashedPassword("h"), "Anna")
    private val member = User.create(Email("jan@example.com"), HashedPassword("h"), "Jan")
    private val outsider = User.create(Email("ola@example.com"), HashedPassword("h"), "Ola")
    private val blocked = User.create(Email("blocked@example.com"), HashedPassword("h"), "Zablokowany").deactivate()
    private val pantry = Pantry.create("Spiżarnia Anny", owner.id)

    @BeforeEach
    fun setup() {
        pantryRepository = mockk(relaxUnitFun = true)
        userRepository = mockk()
        productRepository = mockk()
        every { pantryRepository.findByMember(any()) } returns emptyList()
        policy = PantryAccessPolicy(pantryRepository)
        guard = PantryAccessGuard(pantryRepository, policy)
        every { pantryRepository.findById(pantry.id) } returns pantry
        every { pantryRepository.findMemberRole(pantry.id, any()) } returns null
        every { pantryRepository.findMemberRole(pantry.id, owner.id) } returns PantryRole.OWNER
        every { pantryRepository.findMemberRole(pantry.id, member.id) } returns PantryRole.MEMBER
        every { pantryRepository.findMembers(pantry.id) } returns
            listOf(PantryMember(pantry.id, member.id, PantryRole.MEMBER), PantryMember(pantry.id, owner.id, PantryRole.OWNER))
        listOf(owner, member, outsider, blocked).forEach { user ->
            every { userRepository.findById(user.id) } returns user
            every { userRepository.findByEmail(user.email) } returns user
        }
        every { userRepository.findByEmail(Email("nobody@example.com")) } returns null
    }

    private fun invite() = InvitePantryMember(pantryRepository, userRepository, productRepository, guard, policy)

    private fun remove() = RemovePantryMember(pantryRepository, guard, policy)

    @Test
    fun `owner invites an existing user by e-mail as a member`() {
        val dto = invite().execute(owner.id, pantry.id, "  ola@example.com ")

        assertEquals(outsider.id.toString(), dto.userId)
        assertEquals(PantryRole.MEMBER, dto.role)
        verify { pantryRepository.addMember(PantryMember(pantry.id, outsider.id, PantryRole.MEMBER)) }
    }

    @Test
    fun `invite fails for members, unknown or blocked accounts and existing members`() {
        assertThrows<PantryAccessDeniedException> { invite().execute(member.id, pantry.id, "ola@example.com") }
        assertThrows<PantryAccessDeniedException> { invite().execute(outsider.id, pantry.id, "ola@example.com") }
        assertThrows<PantryMemberNotFoundException> { invite().execute(owner.id, pantry.id, "nobody@example.com") }
        assertThrows<PantryMemberNotFoundException> { invite().execute(owner.id, pantry.id, "blocked@example.com") }
        assertThrows<AlreadyPantryMemberException> { invite().execute(owner.id, pantry.id, "jan@example.com") }
        verify(exactly = 0) { pantryRepository.addMember(any()) }
    }

    @Test
    fun `owner removes a member and a member can leave`() {
        remove().execute(owner.id, pantry.id, member.id)
        remove().execute(member.id, pantry.id, member.id)

        verify(exactly = 2) { pantryRepository.removeMember(pantry.id, member.id) }
    }

    @Test
    fun `the owner cannot be removed and members cannot remove others`() {
        val second = UserId.generate()
        every { pantryRepository.findMemberRole(pantry.id, second) } returns PantryRole.MEMBER

        assertThrows<CannotRemovePantryMemberException> { remove().execute(owner.id, pantry.id, owner.id) }
        assertThrows<CannotRemovePantryMemberException> { remove().execute(member.id, pantry.id, owner.id) }
        assertThrows<CannotRemovePantryMemberException> { remove().execute(member.id, pantry.id, second) }
        assertThrows<PantryAccessDeniedException> { remove().execute(outsider.id, pantry.id, member.id) }
        assertThrows<PantryMemberNotFoundException> { remove().execute(owner.id, pantry.id, outsider.id) }
        verify(exactly = 0) { pantryRepository.removeMember(any(), any()) }
    }

    @Test
    fun `members are listed for any member, owner first, but not for outsiders`() {
        val useCase = ListPantryMembers(pantryRepository, userRepository, guard)

        assertEquals(listOf("Anna", "Jan"), useCase.execute(member.id, pantry.id).map { it.name })
        assertThrows<PantryAccessDeniedException> { useCase.execute(outsider.id, pantry.id) }
    }

    @Test
    fun `only the owner renames the pantry`() {
        every { pantryRepository.save(any()) } answers { firstArg() }
        val useCase = RenamePantry(pantryRepository, guard, policy)

        assertEquals("Spiżarnia rodzinna", useCase.execute(owner.id, pantry.id, " Spiżarnia rodzinna ").name)
        assertThrows<PantryAccessDeniedException> { useCase.execute(member.id, pantry.id, "Moja") }
    }

    @Test
    fun `joining a pantry removes the invitee's own pantry only when it was never used`() {
        val unused = Pantry.create("Spiżarnia Oli", outsider.id)
        val stocked = Pantry.create("Działka", outsider.id)
        val shared = Pantry.create("Dom", outsider.id)
        every { pantryRepository.findByMember(outsider.id) } returns listOf(unused, stocked, shared, pantry)
        every { pantryRepository.findMembers(unused.id) } returns listOf(PantryMember(unused.id, outsider.id, PantryRole.OWNER))
        every { pantryRepository.findMembers(stocked.id) } returns listOf(PantryMember(stocked.id, outsider.id, PantryRole.OWNER))
        every { pantryRepository.findMembers(shared.id) } returns
            listOf(PantryMember(shared.id, outsider.id, PantryRole.OWNER), PantryMember(shared.id, member.id, PantryRole.MEMBER))
        every { productRepository.findByPantry(unused.id, any()) } returns emptyList()
        every { productRepository.findByPantry(stocked.id, any()) } returns listOf(mockk())

        invite().execute(owner.id, pantry.id, "ola@example.com")

        verify(exactly = 1) { pantryRepository.delete(any()) }
        verify { pantryRepository.delete(unused.id) }
    }

    @Test
    fun `my pantries come with my role and member count, the picked default first`() {
        val family = Pantry.create("Spiżarnia rodzinna", outsider.id)
        every { pantryRepository.findMembers(family.id) } returns
            listOf(PantryMember(family.id, outsider.id, PantryRole.OWNER), PantryMember(family.id, member.id, PantryRole.MEMBER))
        val own = Pantry.create("Spiżarnia Jana", member.id)
        every { pantryRepository.findByMember(member.id) } returns listOf(own, pantry, family)
        every { pantryRepository.findMembers(own.id) } returns listOf(PantryMember(own.id, member.id, PantryRole.OWNER))
        every { pantryRepository.findMemberships(member.id) } returns
            listOf(
                PantryMember(own.id, member.id, PantryRole.OWNER),
                PantryMember(pantry.id, member.id, PantryRole.MEMBER),
                PantryMember(family.id, member.id, PantryRole.MEMBER, isDefault = true),
            )

        val summaries = ListMyPantries(pantryRepository).summaries(member.id)

        assertEquals("Spiżarnia rodzinna", summaries.first().name)
        assertEquals(listOf(true, false, false), summaries.map { it.isDefault })
        assertEquals(
            mapOf(
                "Spiżarnia Jana" to (PantryRole.OWNER to 1),
                "Spiżarnia rodzinna" to (PantryRole.MEMBER to 2),
                "Spiżarnia Anny" to (PantryRole.MEMBER to 2),
            ),
            summaries.associate { it.name to (it.myRole to it.memberCount) },
        )
    }

    @Test
    fun `without a picked default the oldest pantry is the default, and no pantries is fine`() {
        val older = pantry.copy(createdAt = pantry.createdAt.minusDays(1))
        val newer = Pantry.create("Działka", member.id)
        every { pantryRepository.findByMember(member.id) } returns listOf(newer, older)
        every { pantryRepository.findMembers(newer.id) } returns listOf(PantryMember(newer.id, member.id, PantryRole.OWNER))
        every { pantryRepository.findMemberships(member.id) } returns
            listOf(PantryMember(newer.id, member.id, PantryRole.OWNER), PantryMember(older.id, member.id, PantryRole.MEMBER))
        every { pantryRepository.findMemberships(outsider.id) } returns emptyList()

        val summaries = ListMyPantries(pantryRepository).summaries(member.id)

        assertEquals(listOf("Spiżarnia Anny" to true, "Działka" to false), summaries.map { it.name to it.isDefault })
        assertEquals(emptyList<Any>(), ListMyPantries(pantryRepository).summaries(outsider.id))
    }

    @Test
    fun `anyone creates a pantry and owns it, the first one is the default`() {
        every { pantryRepository.save(any()) } answers { firstArg() }
        every { pantryRepository.findMemberships(outsider.id) } returns emptyList()
        every { pantryRepository.findMemberships(member.id) } returns listOf(PantryMember(pantry.id, member.id, PantryRole.MEMBER))

        val first = CreatePantry(pantryRepository).execute(outsider.id, "  Dom ")
        val second = CreatePantry(pantryRepository).execute(member.id, "Działka")

        assertEquals("Dom", first.name)
        assertEquals(PantryRole.OWNER, first.myRole)
        assertEquals(true to false, first.isDefault to second.isDefault)
        verify { pantryRepository.addMember(match { it.userId == outsider.id && it.role == PantryRole.OWNER }) }
        assertThrows<IllegalArgumentException> { CreatePantry(pantryRepository).execute(outsider.id, "   ") }
    }

    @Test
    fun `only the owner deletes the pantry`() {
        val useCase = DeletePantry(pantryRepository, guard, policy)

        assertThrows<PantryAccessDeniedException> { useCase.execute(member.id, pantry.id) }
        assertThrows<PantryAccessDeniedException> { useCase.execute(outsider.id, pantry.id) }
        verify(exactly = 0) { pantryRepository.delete(any()) }

        useCase.execute(owner.id, pantry.id)

        verify { pantryRepository.delete(pantry.id) }
    }

    @Test
    fun `any member picks the pantry as their default, outsiders cannot`() {
        val useCase = SetDefaultPantry(pantryRepository, guard)

        useCase.execute(member.id, pantry.id)

        verify { pantryRepository.setDefault(member.id, pantry.id) }
        assertThrows<PantryAccessDeniedException> { useCase.execute(outsider.id, pantry.id) }
    }
}
