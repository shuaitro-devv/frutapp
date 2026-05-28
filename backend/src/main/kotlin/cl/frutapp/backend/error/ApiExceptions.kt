package cl.frutapp.backend.error

import io.ktor.http.HttpStatusCode

/**
 * Excepciones de dominio que StatusPages mapea a una respuesta HTTP uniforme.
 * Cada una lleva su status y un código de error estable para el cliente.
 */
sealed class ApiException(
    val statusCode: HttpStatusCode,
    val errorCode: String,
    override val message: String
) : RuntimeException(message)

class ValidationException(message: String) :
    ApiException(HttpStatusCode.UnprocessableEntity, "validation_error", message)

class ConflictException(message: String) :
    ApiException(HttpStatusCode.Conflict, "conflict", message)

class UnauthorizedException(message: String = "Credenciales inválidas") :
    ApiException(HttpStatusCode.Unauthorized, "unauthorized", message)

class NotFoundException(message: String = "Recurso no encontrado") :
    ApiException(HttpStatusCode.NotFound, "not_found", message)
