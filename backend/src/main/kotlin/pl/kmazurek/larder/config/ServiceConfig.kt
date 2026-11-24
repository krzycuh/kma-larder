package pl.kmazurek.larder.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy

/**
 * Configuration for domain services (plain classes, registered as beans here)
 */
@Configuration
class ServiceConfig {
    @Bean
    fun libraryAccessPolicy(pantryRepository: PantryRepository): PantryAccessPolicy = PantryAccessPolicy(pantryRepository)
}
