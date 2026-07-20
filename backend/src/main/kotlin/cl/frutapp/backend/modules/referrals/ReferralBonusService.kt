package cl.frutapp.backend.modules.referrals

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.orders.FrutCoinsLedgerTable
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.microseconds

/**
 * Programa "Referí un amigo": al completar la primera entrega de un usuario
 * referido, otorga FrutCoins al referidor Y al referido, y marca la fila
 * app_user.referral_reward_granted = true para evitar pagos duplicados.
 *
 * Diseño:
 *  - Hook llamado desde `StaffOrderService.deliveredDispatch` (fire-and-forget).
 *  - Idempotencia: `UserRepository.claimReferralReward` usa un UPDATE
 *    WHERE-guarded (`referral_reward_granted = false`). Si ya se pagó,
 *    devuelve false y no volvemos a insertar en el ledger.
 *  - Los inserts al ledger usan timestamps distintos (ver caso [2] del
 *    audit del reverso de FrutCoins) para que orderBy(createdAt DESC) sea
 *    determinístico.
 */
class ReferralBonusService(
    private val users: UserRepository,
    private val events: UserEventService,
) {
    private val log = LoggerFactory.getLogger("ReferralBonusService")

    companion object {
        // Bonos configurables. En una V+ podemos moverlos a app_config si
        // queremos ajustarlos sin redeploy — por ahora hardcoded para
        // simplicidad y evitar un query extra por cada bono.
        const val BONO_REFERIDOR = 200
        const val BONO_REFERIDO = 100
    }

    /** Chequea si el pedido [orderId] es la primera entrega de un usuario
     *  con referrer, y si es asi, paga los bonos. Idempotente.
     *
     *  El caller debe pasar un [context] con la trazabilidad del request
     *  original (default EMPTY es aceptable para hooks fire-and-forget). */
    suspend fun tryAwardOnFirstDelivery(orderId: UUID, context: EventContext = EventContext.EMPTY) {
        val cliente = ownerOf(orderId) ?: return
        val (referredBy, alreadyGranted) = users.referralInfoOf(cliente) ?: return
        if (referredBy == null) return
        if (alreadyGranted) return

        // Race guard: dos eventos deliveredDispatch concurrentes para el
        // mismo cliente (imposible en la practica pero por si acaso). El
        // UPDATE WHERE-guarded en claimReferralReward garantiza que solo
        // uno gana.
        val ganamos = users.claimReferralReward(cliente)
        if (!ganamos) return

        // Pagamos ambos bonos en la MISMA dbQuery para que el saldo del
        // referidor y del referido queden coherentes contra la lectura
        // (aunque son users distintos, mantener la operacion atomica evita
        // parcialmente aplicado si el proceso muere entre inserts).
        dbQuery {
            insertLedger(
                userId = referredBy,
                orderId = orderId,
                delta = BONO_REFERIDOR,
                motivo = "REFERIDO_COMPLETO",
            )
            insertLedger(
                userId = cliente,
                orderId = orderId,
                delta = BONO_REFERIDO,
                motivo = "BONO_BIENVENIDA_REFERIDO",
                nanosDelay = 1,
            )
        }

        log.info("Referral bonus: referidor={} recibio {} coins, referido={} recibio {} coins por entrega {}",
            referredBy, BONO_REFERIDOR, cliente, BONO_REFERIDO, orderId)

        events.logSafely(
            eventType = "coins.referral_awarded",
            userId = cliente,
            entityType = "app_user",
            entityId = referredBy,
            payload = buildJsonObject {
                put("orderId", JsonPrimitive(orderId.toString()))
                put("referidor", JsonPrimitive(referredBy.toString()))
                put("bonoReferidor", JsonPrimitive(BONO_REFERIDOR))
                put("bonoReferido", JsonPrimitive(BONO_REFERIDO))
            },
            context = context,
        )
    }

    /** Insert en el ledger con saldo calculado desde el balance_after
     *  reciente del user. Requiere ejecucion dentro de un dbQuery (no lo
     *  abre el mismo para poder agrupar los dos inserts del bono). */
    private fun insertLedger(
        userId: UUID,
        orderId: UUID,
        delta: Int,
        motivo: String,
        nanosDelay: Int = 0,
    ) {
        val saldoActual = FrutCoinsLedgerTable
            .selectAll().where { FrutCoinsLedgerTable.userId eq userId }
            .orderBy(FrutCoinsLedgerTable.createdAt to SortOrder.DESC)
            .firstOrNull()?.get(FrutCoinsLedgerTable.balanceAfter) ?: 0
        val nuevoSaldo = saldoActual + delta
        val now = Clock.System.now() + nanosDelay.microseconds
        FrutCoinsLedgerTable.insert {
            it[id] = UUID.randomUUID()
            it[FrutCoinsLedgerTable.userId] = userId
            it[FrutCoinsLedgerTable.orderId] = orderId
            it[FrutCoinsLedgerTable.delta] = delta
            it[FrutCoinsLedgerTable.motivo] = motivo
            it[FrutCoinsLedgerTable.balanceAfter] = nuevoSaldo
            it[FrutCoinsLedgerTable.createdAt] = now
        }
    }

    private suspend fun ownerOf(orderId: UUID): UUID? = dbQuery {
        cl.frutapp.backend.modules.orders.OrdersTable
            .select(cl.frutapp.backend.modules.orders.OrdersTable.userId)
            .where { cl.frutapp.backend.modules.orders.OrdersTable.id eq orderId }
            .singleOrNull()
            ?.get(cl.frutapp.backend.modules.orders.OrdersTable.userId)
    }
}
