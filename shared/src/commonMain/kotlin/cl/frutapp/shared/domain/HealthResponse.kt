package cl.frutapp.shared.domain

import kotlinx.serialization.Serializable

/**
 * Respuesta del endpoint de health check del backend.
 *
 * Es el primer data class compartido entre `app` y `backend` — demuestra que el
 * módulo `shared` está bien cableado en el monorepo Kotlin.
 *
 * Aplica desde el Sprint 0 (setup) en adelante.
 */
@Serializable
data class HealthResponse(
    val status: String,
    val service: String = "frutapp-backend",
    val version: String,
    val timestampMs: Long
)
