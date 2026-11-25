package pl.kmazurek.larder.application.usecase.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import pl.kmazurek.larder.application.usecase.pantry.CreateDefaultPantry
import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.model.user.UserRole
import pl.kmazurek.larder.domain.repository.UserRepository
import pl.kmazurek.larder.infrastructure.security.PasswordEncoderService

/**
 * Use Case: Register a new user and create their default pantry
 */
@Service
class RegisterUser(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
    private val createDefaultLibrary: CreateDefaultPantry,
    @Value("\${app.registration.enabled:true}") private val registrationEnabled: Boolean = true,
) {
    @Transactional
    fun execute(
        email: String,
        password: String,
        name: String,
    ): User {
        if (!registrationEnabled) {
            throw RegistrationDisabledException("Registration is disabled")
        }

        val userEmail = Email(email)

        if (userRepository.existsByEmail(userEmail)) {
            throw EmailAlreadyExistsException("Email $email is already registered")
        }

        val hashedPassword = passwordEncoder.hash(password)
        val user =
            User.create(
                email = userEmail,
                passwordHash = hashedPassword,
                name = name,
                role = UserRole.USER,
            )

        val saved = userRepository.save(user)
        createDefaultLibrary.execute(saved)
        return saved
    }
}

class EmailAlreadyExistsException(message: String) : RuntimeException(message)

class RegistrationDisabledException(message: String) : RuntimeException(message)
