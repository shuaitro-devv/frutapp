package cl.frutapp.backend.modules.rbac

/**
 * Caché en memoria de rol → permisos (fuente: role_permission). El JWT lleva los roles del
 * usuario; los permisos se resuelven acá (cacheado), así cambiar permisos de un rol no obliga
 * a re-emitir tokens. Se refresca al arrancar y periódicamente.
 */
object PermissionCache {
    @Volatile
    private var rolePerms: Map<String, Set<String>> = emptyMap()

    suspend fun refresh(repo: RbacRepository) {
        rolePerms = repo.loadRolePermissions()
    }

    /** ¿Alguno de los roles tiene el permiso? */
    fun has(roles: Collection<String>, permission: String): Boolean =
        roles.any { rolePerms[it]?.contains(permission) == true }

    fun permissionsForRoles(roles: Collection<String>): Set<String> =
        roles.flatMap { rolePerms[it] ?: emptySet() }.toSet()
}
