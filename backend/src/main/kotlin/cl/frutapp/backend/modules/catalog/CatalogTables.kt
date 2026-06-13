package cl.frutapp.backend.modules.catalog

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object CategoryTable : Table("category") {
    val id = uuid("id")
    val name = text("name")
    val slug = text("slug")
    val sortOrder = integer("sort_order")
    override val primaryKey = PrimaryKey(id)
}

object ProductTable : Table("product") {
    val id = uuid("id")
    val categoryId = uuid("category_id")
    val name = text("name")
    val slug = text("slug")
    val description = text("description")
    val priceClp = integer("price_clp")
    val unit = text("unit")
    val imageKey = text("image_key")
    val active = bool("active")
    /** Disponibilidad operacional (stock del dia). Distinto de [active] (soft-delete).
     *  El operador la flipea desde el back office segun lo que llega del proveedor. */
    val disponible = bool("disponible")
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    override val primaryKey = PrimaryKey(id)
}
