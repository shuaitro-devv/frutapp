package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Formato uniforme de error de la API. */
@Serializable
data class ApiError(
    val error: String,
    val message: String
)
