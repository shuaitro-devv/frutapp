package cl.frutapp.backend.modules.rewards

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** V34 `cupon`: cupones generados por canje de FrutCoins. */
internal object CuponTable : Table("cupon") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val codigo = text("codigo")
    val monto = integer("monto")
    val recompensa = text("recompensa")
    val estado = text("estado")            // ACTIVO / USADO / EXPIRADO
    val idempotencyKey = text("idempotency_key")
    val expiraEn = timestamp("expira_en").nullable()
    val usadoEn = timestamp("usado_en").nullable()
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}

object EstadoCupon {
    const val ACTIVO = "ACTIVO"
    const val USADO = "USADO"
    const val EXPIRADO = "EXPIRADO"
}
