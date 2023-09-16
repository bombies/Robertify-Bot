package api.plugins

import api.utils.prodModule
import api.utils.testModule
import io.ktor.server.application.*
import org.koin.core.error.KoinAppAlreadyStartedException
import org.koin.ktor.plugin.Koin

fun Application.configureKoin(test: Boolean) {
    try {
        install(Koin) {
            if (!test) modules(prodModule)
            else modules(testModule)
        }
    } catch (e: KoinAppAlreadyStartedException) {
        log.warn("There is already a Koin application running! Skipping initialization...")
    }
}