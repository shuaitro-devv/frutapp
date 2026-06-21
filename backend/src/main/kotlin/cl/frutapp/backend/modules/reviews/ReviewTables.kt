package cl.frutapp.backend.modules.reviews

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** V32 `producto_resena`: una resena por (user, producto), con upsert.
 *  Estrellas en 1..5 (validado por CHECK en DB y por el service). */
internal object ProductoResenaTable : Table("producto_resena") {
    val id = uuid("id")
    val productId = uuid("product_id")
    val userId = uuid("user_id")
    val estrellas = integer("estrellas")
    val texto = text("texto")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
