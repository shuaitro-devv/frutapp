package cl.frutapp.app.data.remote

import cl.frutapp.shared.dto.AuthResponse
import cl.frutapp.shared.dto.ForgotPasswordRequest
import cl.frutapp.shared.dto.LoginRequest
import cl.frutapp.shared.dto.LogoutRequest
import cl.frutapp.shared.dto.MessageResponse
import cl.frutapp.shared.dto.RefreshRequest
import cl.frutapp.shared.dto.RegisterRequest
import cl.frutapp.shared.dto.ResendVerificationRequest
import cl.frutapp.shared.dto.ResetPasswordRequest
import cl.frutapp.shared.dto.VerifyEmailRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class AuthApi(
    private val client: HttpClient = ApiClient.client,
    private val baseUrl: String = ApiClient.baseUrl
) {
    suspend fun register(req: RegisterRequest): MessageResponse =
        client.post("$baseUrl/v1/auth/register") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun verifyEmail(req: VerifyEmailRequest): AuthResponse =
        client.post("$baseUrl/v1/auth/verify-email") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun resendVerification(req: ResendVerificationRequest): MessageResponse =
        client.post("$baseUrl/v1/auth/resend-verification") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun login(req: LoginRequest): AuthResponse =
        client.post("$baseUrl/v1/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun refresh(req: RefreshRequest): AuthResponse =
        client.post("$baseUrl/v1/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun logout(req: LogoutRequest) {
        client.post("$baseUrl/v1/auth/logout") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }
    }

    suspend fun forgotPassword(req: ForgotPasswordRequest): MessageResponse =
        client.post("$baseUrl/v1/auth/forgot-password") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()

    suspend fun resetPassword(req: ResetPasswordRequest): MessageResponse =
        client.post("$baseUrl/v1/auth/reset-password") {
            contentType(ContentType.Application.Json)
            setBody(req)
        }.body()
}
