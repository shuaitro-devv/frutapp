package cl.frutapp.backend.modules.referrals

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.audit.EventContext
import cl.frutapp.backend.modules.audit.UserEventService
import cl.frutapp.backend.modules.auth.UserRepository
import cl.frutapp.backend.modules.auth.UsersTable
import cl.frutapp.backend.modules.orders.FrutCoinsLedgerTable
import cl.frutapp.backend.modules.orders.OrdersTable
import cl.frutapp.shared.domain.ReferralConfig
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import org.slf4j.LoggerFactory
import java.util.UUID
import kotlin.time.Duration.Companion.microseconds

/**
 * Programa "Invita a un amigo": al completar la primera entrega de un usuario
 * referido, otorga FrutCoins al referidor Y al referido, y marca la fila
 * app_user.referral_reward_granted = true para evitar pagos duplicados.
 *
 * Diseño (fix v0.1.18: TODO en una sola transaccion):
 *  - Hook llamado desde `StaffOrderService.deliveredDispatch`.
 *  - Claim + inserts del ledger en la MISMA dbQuery. Sin esto, si el
 *    proceso muere entre el claim y los inserts, la fila queda con flag
 *    true pero los bonos jamas se pagan (perdida silente).
 *  - Filtros deletedAt en owner + referidor: no pagamos bonos a pedidos
 *    ni usuarios eliminados.
 *  - Idempotencia real: el UPDATE del flag es WHERE-guarded, asi que
 *    dos calls concurrentes solo hacen commit uno.
 */
class ReferralBonusService(
    private val users: UserRepository,
    private val events: UserEventService,
) {
    private val log = LoggerFactory.getLogger("ReferralBonusService")

    private val bonoReferidor = ReferralConfig.BONO_REFERIDOR
    private val bonoReferido = ReferralConfig.BONO_REFERIDO

    /** Chequea si el pedido [orderId] es la primera entrega de un usuario
     *  con referrer, y si es asi, paga los bonos. Idempotente.
     *
     *  El caller debe pasar un [context] con la trazabilidad del request
     *  original (default EMPTY es aceptable para hooks fire-and-forget). */
    suspend fun tryAwardOnFirstDelivery(orderId: UUID, context: EventContext = EventContext.EMPTY) {
        val resultado = dbQuery {
            // Cliente dueño del pedido, filtrando pedidos soft-deleted.
            val cliente = OrdersTable
                .select(OrdersTable.userId)
                .where { (OrdersTable.id eq orderId) and OrdersTable.deletedAt.isNull() }
                .singleOrNull()?.get(OrdersTable.userId)
                ?: return@dbQuery null

            // Info del referral, filtrando users soft-deleted.
            val fila = UsersTable
                .select(UsersTable.referredByUserId, UsersTable.referralRewardGranted)
                .where { (UsersTable.id eq cliente) and UsersTable.deletedAt.isNull() }
                .singleOrNull()
                ?: return@dbQuery null
            val referredBy = fila[UsersTable.referredByUserId] ?: return@dbQuery null
            if (fila[UsersTable.referralRewardGranted]) return@dbQuery null

            // El referidor tiene que seguir vivo. Sin esto pagariamos a un
            // usuario borrado (row-fantasma).
            val referrerVivo = UsersTable
                .select(UsersTable.id)
                .where { (UsersTable.id eq referredBy) and UsersTable.deletedAt.isNull() }
                .empty().not()
            if (!referrerVivo) return@dbQuery null

            // Claim WHERE-guarded: dos calls concurrentes solo hacen commit
            // una. La que pierde el race sale sin pagar.
            val ganamos = UsersTable.update({
                (UsersTable.id eq cliente) and (UsersTable.referralRewardGranted eq false)
            }) {
                it[UsersTable.referralRewardGranted] = true
                it[UsersTable.updatedAt] = Clock.System.now()
            } > 0
            if (!ganamos) return@dbQuery null

            // Bonos en el mismo dbQuery para no dejar el flag apuntando a
            // un pago que jamas ocurrio si el proceso muere aca.
            insertLedger(
                userId = referredBy,
                orderId = orderId,
                delta = bonoReferidor,
                motivo = "REFERIDO_COMPLETO",
            )
            insertLedger(
                userId = cliente,
                orderId = orderId,
                delta = bonoReferido,
                motivo = "BONO_BIENVENIDA_REFERIDO",
                nanosDelay = 1,
            )

            Pair(cliente, referredBy)
        } ?: return

        val (cliente, referredBy) = resultado
        log.info(
            "Referral bonus: referidor={} recibio {} coins, referido={} recibio {} coins por entrega {}",
            referredBy, bonoReferidor, cliente, bonoReferido, orderId
        )

        events.logSafely(
            eventType = "coins.referral_awarded",
            userId = cliente,
            entityType = "app_user",
            entityId = referredBy,
            payload = buildJsonObject {
                put("orderId", JsonPrimitive(orderId.toString()))
                put("referidor", JsonPrimitive(referredBy.toString()))
                put("bonoReferidor", JsonPrimitive(bonoReferidor))
                put("bonoReferido", JsonPrimitive(bonoReferido))
            },
            context = context,
        )
    }

    /** Insert en el ledger con saldo calculado desde el balance_after
     *  reciente del user. Requiere ejecucion dentro de un dbQuery. */
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
}
