package cl.frutapp.backend.modules.auth

import cl.frutapp.backend.plugins.JWT_AUTH
import cl.frutapp.shared.dto.ForgotPasswordRequest
import cl.frutapp.shared.dto.LoginRequest
import cl.frutapp.shared.dto.LogoutRequest
import cl.frutapp.shared.dto.MessageResponse
import cl.frutapp.shared.dto.RefreshRequest
import cl.frutapp.shared.dto.RegisterRequest
import cl.frutapp.shared.dto.ResendVerificationRequest
import cl.frutapp.shared.dto.ResetPasswordRequest
import cl.frutapp.shared.dto.VerifyEmailRequest
import io.ktor.http.HttpStatusCode
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.audit.eventContext
import cl.frutapp.backend.modules.audit.jsonOf
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService, events: UserEventService) {
    route("/v1/auth") {
        // Pool "auth": 10 req/60s por IP. Cubre login/register/refresh/reset que el
        // cliente legitimo no toca seguido. Sin esto un atacante podia barrer
        // passwords por fuerza bruta sin freno.
        rateLimit(RateLimitName("auth")) {
            post("/register") {
                val req = call.receive<RegisterRequest>()
                authService.register(req)
                events.logSafely(
                    eventType = "auth.register",
                    payload = jsonOf("email" to req.email),
                    context = call.eventContext()
                )
                call.respond(HttpStatusCode.Created, MessageResponse("Te enviamos un código de verificación a tu correo."))
            }
            post("/login") {
                val req = call.receive<LoginRequest>()
                val ctx = call.eventContext()
                val resp = try {
                    authService.login(req)
                } catch (t: Throwable) {
                    events.logSafely(
                        eventType = "auth.login_fail",
                        payload = jsonOf("email" to req.email, "motivo" to (t.message ?: t::class.simpleName.orEmpty())),
                        context = ctx
                    )
                    throw t
                }
                events.logSafely(
                    eventType = "auth.login_ok",
                    userId = runCatching { java.util.UUID.fromString(resp.user.id) }.getOrNull(),
                    payload = jsonOf("email" to req.email),
                    context = ctx
                )
                call.respond(HttpStatusCode.OK, resp)
            }
            post("/refresh") {
                call.respond(HttpStatusCode.OK, authService.refresh(call.receive<RefreshRequest>()))
            }
            post("/reset-password") {
                authService.resetPassword(call.receive<ResetPasswordRequest>())
                call.respond(HttpStatusCode.OK, MessageResponse("Contraseña actualizada. Ya puedes iniciar sesión."))
            }
        }
        // Pool "auth-slow": 3 req/60s por IP. Para endpoints que disparan envio de
        // mail (Resend cuesta cuota); evita spam al inbox del usuario.
        rateLimit(RateLimitName("auth-slow")) {
            post("/verify-email") {
                call.respond(HttpStatusCode.OK, authService.verifyEmail(call.receive<VerifyEmailRequest>()))
            }
            post("/resend-verification") {
                authService.resendVerification(call.receive<ResendVerificationRequest>())
                call.respond(HttpStatusCode.OK, MessageResponse("Si tu cuenta está pendiente, reenviamos el código."))
            }
            post("/forgot-password") {
                authService.forgotPassword(call.receive<ForgotPasswordRequest>())
                call.respond(HttpStatusCode.OK, MessageResponse("Si el correo está registrado, te enviamos un código."))
            }
        }
        // Logout no necesita rate limit: requiere JWT valido y solo invalida tokens.
        post("/logout") {
            authService.logout(call.receive<LogoutRequest>())
            events.logSafely(eventType = "auth.logout", context = call.eventContext())
            call.respond(HttpStatusCode.NoContent)
        }
        authenticate(JWT_AUTH) {
            get("/me") {
                val subject = call.principal<JWTPrincipal>()?.subject
                    ?: return@get call.respond(HttpStatusCode.Unauthorized)
                call.respond(HttpStatusCode.OK, authService.me(subject))
            }
        }
    }
}
