package cl.frutapp.backend.modules.rbac

import org.jetbrains.exposed.sql.Table

object RoleTable : Table("role") {
    val id = uuid("id")
    val code = text("code")
    val name = text("name")
    override val primaryKey = PrimaryKey(id)
}

object PermissionTable : Table("permission") {
    val id = uuid("id")
    val code = text("code")
    val description = text("description").nullable()
    override val primaryKey = PrimaryKey(id)
}

object RolePermissionTable : Table("role_permission") {
    val roleId = uuid("role_id")
    val permissionId = uuid("permission_id")
    override val primaryKey = PrimaryKey(roleId, permissionId)
}

object UserRoleTable : Table("user_role") {
    val userId = uuid("user_id")
    val roleId = uuid("role_id")
    override val primaryKey = PrimaryKey(userId, roleId)
}
