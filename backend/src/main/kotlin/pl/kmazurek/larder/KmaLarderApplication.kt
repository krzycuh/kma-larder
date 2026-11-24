package pl.kmazurek.larder

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching

@SpringBootApplication(exclude = [UserDetailsServiceAutoConfiguration::class])
@EnableCaching
class KmaLarderApplication

fun main(args: Array<String>) {
    runApplication<KmaLarderApplication>(*args)
}
