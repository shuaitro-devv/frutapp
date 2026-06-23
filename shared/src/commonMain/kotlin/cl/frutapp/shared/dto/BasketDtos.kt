package cl.frutapp.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class CanastaDto(
    val id: String,
    val nombre: String,
    val emoji: String,
    val recordatorioMensual: Boolean = false,
    val items: List<CanastaItemDto>,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class CanastaItemDto(
    val id: String,
    val productoId: String,    // UUID del product
    val cantidad: Int,
    val gramos: Int? = null,
    val posicion: Int = 0,
)

/** Body POST /v1/baskets. */
@Serializable
data class CrearCanastaRequest(
    val nombre: String,
    val emoji: String = "🧺",
    val recordatorioMensual: Boolean = false,
    val items: List<NuevoCanastaItem>,
)

/** Body PUT /v1/baskets/{id}. Todo nullable: solo se aplica lo que viene.
 *  Si [items] != null, reemplaza la lista completa. */
@Serializable
data class ActualizarCanastaRequest(
    val nombre: String? = null,
    val emoji: String? = null,
    val recordatorioMensual: Boolean? = null,
    val items: List<NuevoCanastaItem>? = null,
)

@Serializable
data class NuevoCanastaItem(
    val productoId: String,
    val cantidad: Int,
    val gramos: Int? = null,
)
