package pl.kmazurek.larder.application.usecase.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pl.kmazurek.larder.domain.model.user.Email
import pl.kmazurek.larder.domain.model.user.User
import pl.kmazurek.larder.domain.model.user.UserRole
import pl.kmazurek.larder.domain.repository.UserRepository
import pl.kmazurek.larder.infrastructure.security.PasswordEncoderService

/**
 * Use Case: Register a new user. The account starts without a pantry: the user creates one or is added to
 * a household's pantry by its owner.
 */
@Service
class RegisterUser(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoderService,
    @Value("\${app.registration.enabled:true}") private val registrationEnabled: Boolean = true,
) {
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

        return userRepository.save(user)
    }
}

class EmailAlreadyExistsException(message: String) : RuntimeException(message)

class RegistrationDisabledException(message: String) : RuntimeException(message)
