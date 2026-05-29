package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Configuración pública (subconjunto client-visible de app_config). */
@Serializable
data class AppConfigDto(
    val values: Map<String, String>
)
