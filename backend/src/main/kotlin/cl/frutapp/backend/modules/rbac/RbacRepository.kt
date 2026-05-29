package cl.frutapp.backend.modules.rbac

import cl.frutapp.backend.db.dbQuery
import org.jetbrains.exposed.sql.JoinType
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID

class RbacRepository {

    /** Códigos de rol de un usuario (se ponen en el JWT al emitir tokens). */
    suspend fun rolesOf(userId: UUID): List<String> = dbQuery {
        UserRoleTable
            .join(RoleTable, JoinType.INNER, onColumn = UserRoleTable.roleId, otherColumn = RoleTable.id)
            .select { UserRoleTable.userId eq userId }
            .map { it[RoleTable.code] }
    }

    /** Asigna un rol a un usuario (idempotente). No-op si el rol no existe. */
    suspend fun assignRole(userId: UUID, roleCode: String): Unit = dbQuery {
        val roleId = RoleTable.select { RoleTable.code eq roleCode }.singleOrNull()?.get(RoleTable.id)
            ?: return@dbQuery
        UserRoleTable.insertIgnore {
            it[UserRoleTable.userId] = userId
            it[UserRoleTable.roleId] = roleId
        }
        Unit
    }

    /** Quita un rol a un usuario (no-op si no lo tenía o el rol no existe). */
    suspend fun revokeRole(userId: UUID, roleCode: String): Unit = dbQuery {
        val roleId = RoleTable.select { RoleTable.code eq roleCode }.singleOrNull()?.get(RoleTable.id)
            ?: return@dbQuery
        UserRoleTable.deleteWhere { Op.build { (UserRoleTable.userId eq userId) and (UserRoleTable.roleId eq roleId) } }
        Unit
    }

    /** Todos los códigos de rol existentes (para validar requests). */
    suspend fun allRoleCodes(): Set<String> = dbQuery {
        RoleTable.selectAll().map { it[RoleTable.code] }.toSet()
    }

    /** roleCode -> set de permisos (para [PermissionCache]). */
    suspend fun loadRolePermissions(): Map<String, Set<String>> = dbQuery {
        RolePermissionTable
            .join(RoleTable, JoinType.INNER, onColumn = RolePermissionTable.roleId, otherColumn = RoleTable.id)
            .join(PermissionTable, JoinType.INNER, onColumn = RolePermissionTable.permissionId, otherColumn = PermissionTable.id)
            .selectAll()
            .map { it[RoleTable.code] to it[PermissionTable.code] }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.toSet() }
    }
}
