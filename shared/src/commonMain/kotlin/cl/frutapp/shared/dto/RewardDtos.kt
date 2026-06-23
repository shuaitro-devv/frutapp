package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

/** Cupon generado al canjear FrutCoins. El [codigo] es lo que el cliente ve. */
@Serializable
data class CuponDto(
    val id: String,
    val codigo: String,        // ej "FRUT-ABCD-EFGH"
    val monto: Int,            // FrutCoins debitados
    val recompensa: String,    // ej "Descuento $1000 en tu próximo pedido"
    val estado: String,        // ACTIVO / USADO / EXPIRADO
    val expiraEn: String?,     // ISO o null
    val usadoEn: String?,      // ISO si ya se uso
    val createdAt: String,     // ISO
)

/** Body para POST /v1/frutcoins/redeem.
 *  [idempotencyKey] lo genera el cliente (UUID v4) al abrir el dialogo de canje
 *  y lo reenvia en cada reintento — el backend lo usa para no duplicar. */
@Serializable
data class CanjearFrutCoinsRequest(
    val monto: Int,
    val recompensa: String,
    val idempotencyKey: String,
)
