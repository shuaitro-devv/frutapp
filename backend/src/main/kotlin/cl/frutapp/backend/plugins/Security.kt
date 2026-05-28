package cl.frutapp.backend.plugins

import cl.frutapp.backend.config.JwtConfig
import cl.frutapp.backend.modules.auth.TokenService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.response.respond

/** Nombre del provider de autenticación JWT (se referencia en `authenticate(JWT_AUTH)`). */
const val JWT_AUTH = "auth-jwt"

fun Application.configureSecurity(jwtConfig: JwtConfig, tokenService: TokenService) {
    install(Authentication) {
        jwt(JWT_AUTH) {
            realm = jwtConfig.realm
            verifier(tokenService.buildVerifier())
            validate { credential ->
                if (credential.payload.subject != null) JWTPrincipal(credential.payload) else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized)
            }
        }
    }
}
