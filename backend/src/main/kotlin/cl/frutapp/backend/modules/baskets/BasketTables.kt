package cl.frutapp.backend.modules.baskets

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

/** V35 `canasta`: canastas guardadas del cliente. */
internal object CanastaTable : Table("canasta") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val nombre = text("nombre")
    val emoji = text("emoji")
    val recordatorioMensual = bool("recordatorio_mensual")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}

/** V35 `canasta_item`: items de una canasta. */
internal object CanastaItemTable : Table("canasta_item") {
    val id = uuid("id")
    val canastaId = uuid("canasta_id")
    val productId = uuid("product_id")
    val cantidad = integer("cantidad")
    val gramos = integer("gramos").nullable()
    val posicion = integer("posicion")
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
