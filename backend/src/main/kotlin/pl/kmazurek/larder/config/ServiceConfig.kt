package pl.kmazurek.larder.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import pl.kmazurek.larder.domain.repository.PantryRepository
import pl.kmazurek.larder.domain.service.PantryAccessPolicy
import java.time.Clock
import java.time.ZoneId

/**
 * Configuration for domain services (plain classes, registered as beans here)
 */
@Configuration
class ServiceConfig {
    @Bean
    fun pantryAccessPolicy(pantryRepository: PantryRepository): PantryAccessPolicy = PantryAccessPolicy(pantryRepository)

    /** "Today" for best-before dates is the household's day, not UTC's */
    @Bean
    fun clock(): Clock = Clock.system(ZoneId.of("Europe/Warsaw"))
}
