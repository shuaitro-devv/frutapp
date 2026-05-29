package cl.frutapp.backend.modules.rbac

import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal

/** Roles (códigos) del JWT del llamante. */
fun ApplicationCall.roles(): List<String> =
    principal<JWTPrincipal>()?.payload?.getClaim("roles")?.asList(String::class.java) ?: emptyList()

/** ¿El llamante tiene el permiso? (resuelto desde sus roles vía [PermissionCache]). */
fun ApplicationCall.hasPermission(permission: String): Boolean =
    PermissionCache.has(roles(), permission)
