package cl.frutapp.backend

import cl.frutapp.backend.config.JwtConfig
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.RefreshTokenRepository
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.backend.modules.catalog.CatalogService
import cl.frutapp.backend.plugins.configureCors
import cl.frutapp.backend.plugins.configureDatabases
import cl.frutapp.backend.plugins.configureMonitoring
import cl.frutapp.backend.plugins.configureRouting
import cl.frutapp.backend.plugins.configureSecurity
import cl.frutapp.backend.plugins.configureSerialization
import cl.frutapp.backend.plugins.configureStatusPages
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain

/**
 * Entrada principal del backend FrutApp.
 *
 * `EngineMain` lee `application.yaml` y arranca Netty. El cableado (config → DB →
 * servicios → seguridad → rutas) es explícito y se mantiene legible/testeable.
 */
fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    val jwtConfig = JwtConfig.from(environment.config)

    configureSerialization()
    configureCors()
    configureStatusPages()
    configureMonitoring()
    configureDatabases()

    val tokenService = TokenService(jwtConfig)
    val authService = AuthService(UserRepository(), RefreshTokenRepository(), tokenService)
    val catalogService = CatalogService(CatalogRepository())

    configureSecurity(jwtConfig, tokenService)
    configureRouting(authService, catalogService)
}
