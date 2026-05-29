package cl.frutapp.backend.modules.orders

import cl.frutapp.backend.db.dbQuery
import cl.frutapp.shared.dto.FrutCoinsBalanceDto
import cl.frutapp.shared.dto.FrutCoinsEntryDto
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.select
import java.util.UUID

class FrutCoinsRepository {

    /** Saldo (suma del ledger) + historial de movimientos, más reciente primero. */
    suspend fun balanceAndHistory(userId: UUID): FrutCoinsBalanceDto = dbQuery {
        val movimientos = FrutCoinsLedgerTable
            .select { FrutCoinsLedgerTable.userId eq userId }
            .orderBy(FrutCoinsLedgerTable.createdAt to SortOrder.DESC)
            .map {
                FrutCoinsEntryDto(
                    delta = it[FrutCoinsLedgerTable.delta],
                    motivo = it[FrutCoinsLedgerTable.motivo],
                    balanceAfter = it[FrutCoinsLedgerTable.balanceAfter],
                    fecha = it[FrutCoinsLedgerTable.createdAt].toString()
                )
            }
        FrutCoinsBalanceDto(balance = movimientos.sumOf { it.delta }, movimientos = movimientos)
    }
}
