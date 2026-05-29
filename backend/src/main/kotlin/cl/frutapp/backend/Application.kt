package cl.frutapp.backend

import cl.frutapp.backend.config.JwtConfig
import cl.frutapp.backend.config.MailConfig
import cl.frutapp.backend.modules.auth.AuthService
import cl.frutapp.backend.modules.auth.EmailSender
import cl.frutapp.backend.modules.auth.EmailVerificationTokenRepository
import cl.frutapp.backend.modules.auth.LogEmailSender
import cl.frutapp.backend.modules.auth.PasswordResetTokenRepository
import cl.frutapp.backend.modules.auth.RefreshTokenRepository
import cl.frutapp.backend.modules.auth.ResendEmailSender
import cl.frutapp.backend.modules.auth.TokenService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.catalog.CatalogRepository
import cl.frutapp.backend.modules.catalog.CatalogService
import cl.frutapp.backend.modules.orders.FrutCoinsRepository
import cl.frutapp.backend.modules.orders.OrderRepository
import cl.frutapp.backend.modules.orders.OrderService
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

    val mailConfig = MailConfig.from(environment.config)
    val emailSender: EmailSender = if (mailConfig.enabled) {
        environment.log.info("Correo: Resend habilitado (from={})", mailConfig.from)
        ResendEmailSender(mailConfig.resendApiKey, mailConfig.from)
    } else {
        environment.log.info("Correo: modo demo (LogEmailSender). Define RESEND_API_KEY para enviar de verdad.")
        LogEmailSender()
    }

    val tokenService = TokenService(jwtConfig)
    val authService = AuthService(
        users = UserRepository(),
        refreshTokens = RefreshTokenRepository(),
        passwordResetTokens = PasswordResetTokenRepository(),
        emailVerificationTokens = EmailVerificationTokenRepository(),
        tokens = tokenService,
        emailSender = emailSender
    )
    val catalogRepository = CatalogRepository()
    val catalogService = CatalogService(catalogRepository)
    val orderService = OrderService(OrderRepository(), catalogRepository, FrutCoinsRepository())

    configureSecurity(jwtConfig, tokenService)
    configureRouting(authService, catalogService, orderService)
}
