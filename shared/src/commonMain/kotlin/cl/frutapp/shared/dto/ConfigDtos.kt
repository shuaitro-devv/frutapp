package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Configuración pública (subconjunto client-visible de app_config). */
@Serializable
data class AppConfigDto(
    val values: Map<String, String>
)

/** Entrada completa de configuración para el back office: incluye metadata
 *  (type, description, client_visible) que el endpoint público no expone. */
@Serializable
data class AdminConfigEntryDto(
    val key: String,
    val value: String,
    val type: String,           // INT | DECIMAL | STRING | BOOL
    val description: String?,
    val clientVisible: Boolean,
    val updatedAt: String       // ISO-8601 para mostrar "última edición" sin parsear
)

/** Lista completa de configuración para el back office. */
@Serializable
data class AdminConfigListResponseDto(
    val entries: List<AdminConfigEntryDto>
)

/** Request para PUT /v1/admin/config/{key}. El type lo determina la fila existente:
 *  no se crean keys nuevas desde acá (esas vienen por migración), solo se editan
 *  las existentes. Mantiene el contrato simple y previene basura en la tabla. */
@Serializable
data class UpdateConfigRequest(
    val value: String
)
