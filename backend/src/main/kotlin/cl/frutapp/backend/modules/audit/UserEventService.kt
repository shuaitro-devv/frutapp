package cl.frutapp.backend.modules.audit

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.insert
import org.slf4j.LoggerFactory
import java.util.UUID

private val payloadJson = Json {}
private val logger = LoggerFactory.getLogger(UserEventService::class.java)

/**
 * Contexto tecnico de la request (de donde viene). Se arma en el endpoint
 * leyendo headers + (opcionalmente) ClientContextDto que ya manda la app.
 */
data class EventContext(
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val appVersion: String? = null,
    val deviceModel: String? = null,
    val osVersion: String? = null,
    val locale: String? = null,
    val channel: String? = null
) {
    companion object {
        val EMPTY = EventContext()
    }
}

/**
 * Servicio central para registrar eventos de usuario. Lo usan todos los
 * endpoints relevantes (auth, orders, staff, frutcoins). Los eventos quedan
 * como ledger inmutable consultable para soporte, analytics y auditoria
 * regulatoria (Ley 21.719).
 *
 * Convencion de event_type: 'dominio.accion' en minusculas.
 *   auth.login_ok, auth.login_fail, auth.register, auth.logout
 *   order.created, order.cancelled
 *   staff.order_taken, staff.order_released, staff.order_completed
 *   staff.dispatch_taken, staff.dispatch_delivered
 *   frutcoins.earned, frutcoins.redeemed
 *
 * payload: JSON con detalles especificos del evento (numero pedido, motivo,
 * monto, etc). NO duplicar lo que ya esta en contexto (IP, app version).
 */
class UserEventService {

    suspend fun log(
        eventType: String,
        userId: UUID? = null,
        entityType: String? = null,
        entityId: UUID? = null,
        payload: JsonElement = JsonObject(emptyMap()),
        context: EventContext = EventContext.EMPTY
    ) {
        dbQuery {
            UserEventTable.insert {
                it[id] = UUID.randomUUID()
                it[UserEventTable.userId] = userId
                it[UserEventTable.eventType] = eventType
                it[UserEventTable.entityType] = entityType
                it[UserEventTable.entityId] = entityId
                it[UserEventTable.payload] = payloadJson.encodeToString(JsonElement.serializer(), payload)
                it[ipAddress] = context.ipAddress
                it[userAgent] = context.userAgent
                it[appVersion] = context.appVersion
                it[deviceModel] = context.deviceModel
                it[osVersion] = context.osVersion
                it[locale] = context.locale
                it[channel] = context.channel
                it[createdAt] = Clock.System.now()
            }
        }
    }

    /**
     * Como [log] pero atrapa cualquier excepcion (re-lanza CancellationException por
     * structured concurrency). Para usar despues de operaciones de negocio que YA
     * commitearon: si el insert del evento falla (BD caida, cast invalido) NO queremos
     * que el endpoint devuelva 500 sobre una operacion que ya tuvo exito.
     *
     * Tradeoff conocido: podemos perder eventos en caso de outage. El balance favorece
     * la experiencia del usuario (la accion principal funciono) sobre la completitud
     * del ledger; los eventos perdidos quedan en logs/Sentry para reconciliar.
     */
    suspend fun logSafely(
        eventType: String,
        userId: UUID? = null,
        entityType: String? = null,
        entityId: UUID? = null,
        payload: JsonElement = JsonObject(emptyMap()),
        context: EventContext = EventContext.EMPTY
    ) {
        try {
            log(eventType, userId, entityType, entityId, payload, context)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            logger.warn("No se pudo registrar evento $eventType para user=$userId entity=$entityType:$entityId", e)
        }
    }
}
