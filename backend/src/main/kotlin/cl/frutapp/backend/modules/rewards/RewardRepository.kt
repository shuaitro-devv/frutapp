package cl.frutapp.backend.modules.rewards

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.backend.modules.orders.FrutCoinsLedgerTable
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.plus
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import java.util.UUID

class RewardRepository {

    /** Saldo actual = suma de delta del ledger del usuario. */
    suspend fun saldoActual(userId: UUID): Int = dbQuery {
        FrutCoinsLedgerTable
            .selectAll().where { FrutCoinsLedgerTable.userId eq userId }
            .sumOf { it[FrutCoinsLedgerTable.delta] }
    }

    /** Busca un cupon por (user, idempotencyKey) si existe — usado para
     *  blindar reintentos del POST. */
    suspend fun cuponPorIdempotency(userId: UUID, idempotencyKey: String): CuponRow? = dbQuery {
        CuponTable.selectAll().where {
            (CuponTable.userId eq userId) and (CuponTable.idempotencyKey eq idempotencyKey)
        }.singleOrNull()?.toRow()
    }

    /** Crea cupon + entrada en frutcoins_ledger en una sola transaccion.
     *  Devuelve el cupon resultante. NO valida saldo — eso lo hace el service. */
    suspend fun canjear(
        userId: UUID,
        monto: Int,
        recompensa: String,
        idempotencyKey: String,
        codigo: String,
        ttlDias: Int,
    ): CuponRow = dbQuery {
        val now = Clock.System.now()
        val cuponId = UUID.randomUUID()
        val saldoActual = FrutCoinsLedgerTable
            .selectAll().where { FrutCoinsLedgerTable.userId eq userId }
            .sumOf { it[FrutCoinsLedgerTable.delta] }
        val expira = now.plus(ttlDias.toLong(), DateTimeUnit.DAY, kotlinx.datetime.TimeZone.UTC)

        CuponTable.insert {
            it[CuponTable.id] = cuponId
            it[CuponTable.userId] = userId
            it[CuponTable.codigo] = codigo
            it[CuponTable.monto] = monto
            it[CuponTable.recompensa] = recompensa
            it[CuponTable.estado] = EstadoCupon.ACTIVO
            it[CuponTable.idempotencyKey] = idempotencyKey
            it[CuponTable.expiraEn] = expira
            it[CuponTable.createdAt] = now
        }
        FrutCoinsLedgerTable.insert {
            it[FrutCoinsLedgerTable.id] = UUID.randomUUID()
            it[FrutCoinsLedgerTable.userId] = userId
            it[FrutCoinsLedgerTable.delta] = -monto
            it[FrutCoinsLedgerTable.motivo] = "CANJE"
            it[FrutCoinsLedgerTable.balanceAfter] = saldoActual - monto
            it[FrutCoinsLedgerTable.createdAt] = now
        }
        // Releer la fila del cupon recien creada (mantenemos la fuente unica).
        CuponTable.selectAll().where { CuponTable.id eq cuponId }.single().toRow()
    }

    /** Lista cupones del usuario, mas reciente primero. */
    suspend fun listarPorUsuario(userId: UUID): List<CuponRow> = dbQuery {
        CuponTable.selectAll()
            .where { CuponTable.userId eq userId }
            .orderBy(CuponTable.createdAt, SortOrder.DESC)
            .map { it.toRow() }
    }

    /** Marca un cupon como usado (estado USADO + usado_en=now). Solo si es del
     *  usuario y esta ACTIVO. Devuelve true si actualizo. */
    suspend fun marcarUsado(userId: UUID, cuponId: UUID): Boolean = dbQuery {
        val now = Clock.System.now()
        val n = CuponTable.update({
            (CuponTable.id eq cuponId) and
                (CuponTable.userId eq userId) and
                (CuponTable.estado eq EstadoCupon.ACTIVO)
        }) {
            it[estado] = EstadoCupon.USADO
            it[usadoEn] = now
        }
        n > 0
    }

    private fun org.jetbrains.exposed.sql.ResultRow.toRow(): CuponRow = CuponRow(
        id = this[CuponTable.id],
        userId = this[CuponTable.userId],
        codigo = this[CuponTable.codigo],
        monto = this[CuponTable.monto],
        recompensa = this[CuponTable.recompensa],
        estado = this[CuponTable.estado],
        expiraEn = this[CuponTable.expiraEn],
        usadoEn = this[CuponTable.usadoEn],
        createdAt = this[CuponTable.createdAt],
    )

    data class CuponRow(
        val id: UUID,
        val userId: UUID,
        val codigo: String,
        val monto: Int,
        val recompensa: String,
        val estado: String,
        val expiraEn: Instant?,
        val usadoEn: Instant?,
        val createdAt: Instant,
    )
}
