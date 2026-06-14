package cl.frutapp.backend.modules.pagos

import cl.frutapp.backend.db.dbQuery
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

/** Acceso a `webpay_transaccion`. SQL puro — sin reglas de negocio. */
class WebpayRepository {

    suspend fun insert(
        token: String,
        orderId: UUID,
        userId: UUID,
        buyOrder: String,
        montoClp: Int,
    ) = dbQuery {
        WebpayTransaccionTable.insert {
            it[WebpayTransaccionTable.token] = token
            it[WebpayTransaccionTable.orderId] = orderId
            it[WebpayTransaccionTable.userId] = userId
            it[WebpayTransaccionTable.buyOrder] = buyOrder
            it[WebpayTransaccionTable.monto] = montoClp
            it[WebpayTransaccionTable.estado] = WEBPAY_ESTADO_INICIADA
            it[WebpayTransaccionTable.creadoEn] = Clock.System.now()
        }
    }

    suspend fun findByToken(token: String): TxRow? = dbQuery {
        WebpayTransaccionTable
            .selectAll().where { WebpayTransaccionTable.token eq token }
            .singleOrNull()?.let {
                TxRow(
                    token = it[WebpayTransaccionTable.token],
                    orderId = it[WebpayTransaccionTable.orderId],
                    userId = it[WebpayTransaccionTable.userId],
                    buyOrder = it[WebpayTransaccionTable.buyOrder],
                    monto = it[WebpayTransaccionTable.monto],
                    estado = it[WebpayTransaccionTable.estado],
                    creadoEn = it[WebpayTransaccionTable.creadoEn],
                )
            }
    }

    /** True si hay una tx INICIADA para el pedido creada despues de [umbral]
     *  (guard anti doble-cobro: el usuario podria abrir 2 ventanas y cobrarse
     *  dos veces si dejamos arrancar otra tx con la primera todavia abierta). */
    suspend fun hayIniciadaReciente(orderId: UUID, umbral: Instant): Boolean = dbQuery {
        WebpayTransaccionTable.selectAll().where {
            (WebpayTransaccionTable.orderId eq orderId) and
            (WebpayTransaccionTable.estado eq WEBPAY_ESTADO_INICIADA) and
            (WebpayTransaccionTable.creadoEn.greater(umbral))
        }.any()
    }

    /** Cambia el estado de una tx. Idempotente — si el estado ya es [nuevo]
     *  el UPDATE deja 0 filas pero no falla (el caller ya valido el flujo). */
    suspend fun cambiarEstado(token: String, nuevo: String) = dbQuery {
        WebpayTransaccionTable.update({ WebpayTransaccionTable.token eq token }) {
            it[estado] = nuevo
        }
    }

    data class TxRow(
        val token: String,
        val orderId: UUID,
        val userId: UUID,
        val buyOrder: String,
        val monto: Int,
        val estado: String,
        val creadoEn: Instant,
    )
}
