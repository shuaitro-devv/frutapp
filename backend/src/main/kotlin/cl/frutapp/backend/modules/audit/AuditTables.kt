package cl.frutapp.backend.modules.audit

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/**
 * Ledger inmutable de eventos de usuario para auditoria, soporte y analytics.
 * Patron event-sourced (nunca UPDATE, solo INSERT). Mismo enfoque que
 * frutcoins_ledger pero para acciones de UX/auth/staff.
 *
 * El user_id es nullable para dos casos:
 *  - Evento anonimo (browse pre-login).
 *  - Anonimizacion post-retencion (Ley 21.719): UPDATE user_id=NULL conserva
 *    el dato estadistico sin la identidad.
 *
 * payload se persiste como TEXT (JSON serializado a mano). En BD la columna es
 * JSONB para indices futuros; desde Exposed lo tratamos como TEXT porque la
 * version de Exposed del proyecto no incluye el modulo json.
 */
object UserEventTable : Table("user_event") {
    val id = uuid("id")
    val userId = uuid("user_id").nullable()
    val eventType = text("event_type")
    val entityType = text("entity_type").nullable()
    val entityId = uuid("entity_id").nullable()
    val payload = text("payload")
    val ipAddress = text("ip_address").nullable()
    val userAgent = text("user_agent").nullable()
    val appVersion = text("app_version").nullable()
    val deviceModel = text("device_model").nullable()
    val osVersion = text("os_version").nullable()
    val locale = text("locale").nullable()
    val channel = text("channel").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
