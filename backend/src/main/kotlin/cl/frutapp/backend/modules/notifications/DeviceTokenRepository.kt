package cl.frutapp.backend.modules.notifications

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.backend.modules.rbac.RoleTable
import cl.frutapp.backend.modules.rbac.UserRoleTable
import kotlinx.datetime.Clock
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

data class DeviceTokenRow(
    val id: UUID,
    val userId: UUID,
    val fcmToken: String,
    val platform: String,
    val appId: String?
)

class DeviceTokenRepository {

    /**
     * Upsert por [fcmToken]: si ya existe (lo mando otro device/user), reasigna a [userId]
     * y actualiza platform/appId. Sino, inserta. Resuelve el caso "Juan logout, Maria login
     * en el mismo celu" — FCM emite el mismo token y reasignamos el dueno para que Maria
     * NO siga recibiendo push de pedidos de Juan.
     */
    suspend fun upsert(userId: UUID, fcmToken: String, platform: String, appId: String?): DeviceTokenRow = dbQuery {
        val now = Clock.System.now()
        val existing = DeviceTokensTable
            .selectAll().where { DeviceTokensTable.fcmToken eq fcmToken }
            .singleOrNull()
        if (existing != null) {
            DeviceTokensTable.update({ DeviceTokensTable.fcmToken eq fcmToken }) {
                it[DeviceTokensTable.userId] = userId
                it[DeviceTokensTable.platform] = platform
                it[DeviceTokensTable.appId] = appId
                it[DeviceTokensTable.updatedAt] = now
            }
            DeviceTokenRow(
                id = existing[DeviceTokensTable.id],
                userId = userId,
                fcmToken = fcmToken,
                platform = platform,
                appId = appId
            )
        } else {
            val newId = UUID.randomUUID()
            DeviceTokensTable.insert {
                it[DeviceTokensTable.id] = newId
                it[DeviceTokensTable.userId] = userId
                it[DeviceTokensTable.fcmToken] = fcmToken
                it[DeviceTokensTable.platform] = platform
                it[DeviceTokensTable.appId] = appId
                it[DeviceTokensTable.createdAt] = now
                it[DeviceTokensTable.updatedAt] = now
            }
            DeviceTokenRow(newId, userId, fcmToken, platform, appId)
        }
    }

    suspend fun listByUser(userId: UUID): List<DeviceTokenRow> = dbQuery {
        DeviceTokensTable
            .selectAll().where { DeviceTokensTable.userId eq userId }
            .map(::toRow)
    }

    /**
     * Tokens de TODOS los users con [roleCode] cuyo `home_location_id` matchea
     * [locationId]. Usado para notificar a pickers/repartidores cuando hay
     * pedido o despacho nuevo en su location. Excluye usuarios soft-deleted.
     *
     * Implementacion en 3 lookups por separado en vez de un join multi-tabla:
     * la cantidad de pickers/repartidores por location es chica (<10) y este
     * camino es mucho mas legible que el SQL crudo.
     */
    suspend fun listTokensByRoleInLocation(roleCode: String, locationId: UUID): List<DeviceTokenRow> = dbQuery {
        val roleId = RoleTable.selectAll().where { RoleTable.code eq roleCode }
            .singleOrNull()?.get(RoleTable.id)
            ?: return@dbQuery emptyList()
        val userIdsWithRole = UserRoleTable.selectAll().where { UserRoleTable.roleId eq roleId }
            .map { it[UserRoleTable.userId] }.toSet()
        if (userIdsWithRole.isEmpty()) return@dbQuery emptyList()
        val targetUserIds = UsersTable.selectAll()
            .where {
                (UsersTable.id inList userIdsWithRole.toList()) and
                (UsersTable.homeLocationId eq locationId) and
                UsersTable.deletedAt.isNull()
            }
            .map { it[UsersTable.id] }
        if (targetUserIds.isEmpty()) return@dbQuery emptyList()
        DeviceTokensTable.selectAll()
            .where { DeviceTokensTable.userId inList targetUserIds }
            .map(::toRow)
    }

    suspend fun deleteByToken(fcmToken: String): Int = dbQuery {
        DeviceTokensTable.deleteWhere { DeviceTokensTable.fcmToken eq fcmToken }
    }

    /** Se usa cuando FCM responde 404 UNREGISTERED al enviar push → el token murio. */
    suspend fun deleteAll(fcmTokens: Collection<String>): Int = dbQuery {
        if (fcmTokens.isEmpty()) return@dbQuery 0
        DeviceTokensTable.deleteWhere { DeviceTokensTable.fcmToken inList fcmTokens.toList() }
    }

    private fun toRow(r: org.jetbrains.exposed.sql.ResultRow) = DeviceTokenRow(
        id = r[DeviceTokensTable.id],
        userId = r[DeviceTokensTable.userId],
        fcmToken = r[DeviceTokensTable.fcmToken],
        platform = r[DeviceTokensTable.platform],
        appId = r[DeviceTokensTable.appId]
    )
}
