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
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(authService: AuthService) {
    route("/v1/auth") {
        post("/register") {
            authService.register(call.receive<RegisterRequest>())
            call.respond(HttpStatusCode.Created, MessageResponse("Te enviamos un código de verificación a tu correo."))
        }
        post("/verify-email") {
            call.respond(HttpStatusCode.OK, authService.verifyEmail(call.receive<VerifyEmailRequest>()))
        }
        post("/resend-verification") {
            authService.resendVerification(call.receive<ResendVerificationRequest>())
            call.respond(HttpStatusCode.OK, MessageResponse("Si tu cuenta está pendiente, reenviamos el código."))
        }
        post("/login") {
            call.respond(HttpStatusCode.OK, authService.login(call.receive<LoginRequest>()))
        }
        post("/refresh") {
            call.respond(HttpStatusCode.OK, authService.refresh(call.receive<RefreshRequest>()))
        }
        post("/logout") {
            authService.logout(call.receive<LogoutRequest>())
            call.respond(HttpStatusCode.NoContent)
        }
        post("/forgot-password") {
            authService.forgotPassword(call.receive<ForgotPasswordRequest>())
            call.respond(HttpStatusCode.OK, MessageResponse("Si el correo está registrado, te enviamos un código."))
        }
        post("/reset-password") {
            authService.resetPassword(call.receive<ResetPasswordRequest>())
            call.respond(HttpStatusCode.OK, MessageResponse("Contraseña actualizada. Ya puedes iniciar sesión."))
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
