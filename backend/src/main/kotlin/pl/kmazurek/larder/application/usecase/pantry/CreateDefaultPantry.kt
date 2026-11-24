package pl.kmazurek.larder.application.usecase.pantry

import org.springframework.stereotype.Service
import pl.kmazurek.larder.domain.model.pantry.Pantry
import pl.kmazurek.larder.domain.model.pantry.PantryMember
import pl.kmazurek.larder.domain.model.pantry.PantryRole
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.repository.PantryRepository

/**
 * Use Case: create the personal pantry of a freshly registered user.
 * The only place where Identity & Access touches the Pantry context.
 */
@Service
class CreateDefaultPantry(
    private val pantryRepository: PantryRepository,
) {
    fun execute(user: User): Pantry {
        val pantry = pantryRepository.save(Pantry.create(defaultName(user.name), user.id))
        pantryRepository.addMember(PantryMember(pantry.id, user.id, PantryRole.OWNER))
        return pantry
    }

    private fun defaultName(userName: String): String {
        val firstName = userName.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
        return if (firstName.isBlank()) "Moja spiżarnia" else "Spiżarnia $firstName"
    }
}
