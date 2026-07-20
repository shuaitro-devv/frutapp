package cl.frutapp.backend.modules.orders

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

object OrdersTable : Table("customer_order") {
    val id = uuid("id")
    val numero = text("numero")
    val userId = uuid("user_id")
    val status = text("status")
    val paymentStatus = text("payment_status")
    val direccion = text("direccion")
    val entrega = text("entrega")
    val subtotalEstimado = integer("subtotal_estimado")
    val envio = integer("envio")
    val totalEstimado = integer("total_estimado")
    val totalFinal = integer("total_final").nullable()
    val frutcoinsGanadas = integer("frutcoins_ganadas")
    val frutcoinsCanjeadas = integer("frutcoins_canjeadas")
    val fulfillmentType = text("fulfillment_type")
    val sucursal = text("sucursal").nullable()
    val channel = text("channel").nullable()
    val appVersion = text("app_version").nullable()
    val deviceModel = text("device_model").nullable()
    val osVersion = text("os_version").nullable()
    val locale = text("locale").nullable()
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")
    val deletedAt = timestamp("deleted_at").nullable()
    // V12: cola del staff
    val pickupLocationId = uuid("pickup_location_id").nullable()
    val assignedPickerId = uuid("assigned_picker_id").nullable()
    val assignedRepartidorId = uuid("assigned_repartidor_id").nullable()
    val assignedAt = timestamp("assigned_at").nullable()
    // V36: codigo de entrega de 4 digitos. Se genera al EN_DESPACHO; el cliente
    // lo ve en su app y se lo dice al repartidor cara a cara. El repartidor lo
    // envia en /delivered y el backend valida match antes de transicionar a
    // ENTREGADO.
    val deliveryCode = text("delivery_code").nullable()
    // V41: pausa del despacho — timestamp cuando el repartidor pauso, se
    // limpia al reanudar o al ENTREGAR/CANCELAR. NULL = no pausado.
    val dispatchPausedAt = timestamp("dispatch_paused_at").nullable()
    val dispatchPauseReason = text("dispatch_pause_reason").nullable()
    override val primaryKey = PrimaryKey(id)
}

object OrderPaymentsTable : Table("order_payment") {
    val id = uuid("id")
    val orderId = uuid("order_id")
    val method = text("method")
    val monto = integer("monto")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object OrderItemsTable : Table("order_item") {
    val id = uuid("id")
    val orderId = uuid("order_id")
    val productId = uuid("product_id")
    val nombre = text("nombre")
    val unidad = text("unidad")
    val imageKey = text("image_key")
    val precioUnitario = integer("precio_unitario")
    val gramos = integer("gramos").nullable()
    val cantidad = integer("cantidad")
    val montoEstimado = integer("monto_estimado")
    val pesoReal = integer("peso_real").nullable()
    val montoFinal = integer("monto_final").nullable()
    val itemStatus = text("item_status")
    /** Nombre del producto sustituto cuando el picker sustituye (V22). Null si no
     *  hubo sustitucion — el item se entrego tal cual lo pidio el cliente. */
    val sustitutoNombre = text("sustituto_nombre").nullable()
    val sustitutoImageKey = text("sustituto_image_key").nullable()
    val sustitutoProductId = uuid("sustituto_product_id").nullable()
    override val primaryKey = PrimaryKey(id)
}

object OrderStatusHistoryTable : Table("order_status_history") {
    val id = uuid("id")
    val orderId = uuid("order_id")
    val fromStatus = text("from_status").nullable()
    val toStatus = text("to_status")
    val actor = text("actor")
    // V11: quien EXACTAMENTE hizo la transicion (nullable cuando actor=SISTEMA).
    val actorUserId = uuid("actor_user_id").nullable()
    val nota = text("nota").nullable()
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

// V12: ubicaciones donde se arman pedidos (bodega central o puesto feriante).
object PickupLocationTable : Table("pickup_location") {
    val id = uuid("id")
    val code = text("code")
    val name = text("name")
    val address = text("address").nullable()
    val isActive = bool("is_active")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}

object FrutCoinsLedgerTable : Table("frutcoins_ledger") {
    val id = uuid("id")
    val userId = uuid("user_id")
    val orderId = uuid("order_id").nullable()
    val delta = integer("delta")
    val motivo = text("motivo")
    val balanceAfter = integer("balance_after")
    val createdAt = timestamp("created_at")
    override val primaryKey = PrimaryKey(id)
}
