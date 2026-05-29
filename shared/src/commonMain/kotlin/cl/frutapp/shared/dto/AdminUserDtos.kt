package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Crear/invitar un usuario de equipo (staff/proveedor) con rol(es). Back office. */
@Serializable
data class AdminCreateUserRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val roles: List<String>
)

/** Asignar/quitar roles a un usuario. */
@Serializable
data class SetRolesRequest(
    val add: List<String> = emptyList(),
    val remove: List<String> = emptyList()
)

/** Vista de un usuario para el back office (incluye sus roles). */
@Serializable
data class AdminUserDto(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val roles: List<String>
)
