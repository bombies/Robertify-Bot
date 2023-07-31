package api.plugins

import api.utils.prodModule
import api.utils.testModule
import io.ktor.server.application.*
import org.koin.ktor.plugin.Koin

fun Application.configureKoin(test: Boolean) {
    install(Koin) {
        if (!test) modules(prodModule)
        else modules(testModule)
    }
}