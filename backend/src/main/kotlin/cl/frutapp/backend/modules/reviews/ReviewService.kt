package cl.frutapp.backend.modules.reviews

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.ResenaDto
import java.util.UUID

/**
 * Logica de resenas de producto:
 *
 *  - **Crear / editar**: 1-5 estrellas obligatorias, texto opcional (<=500
 *    chars). Upsert: una resena por (productId, userId); si el cliente vuelve
 *    a enviar, se actualiza la anterior.
 *
 *  - **Listar**: ultimo arriba, hasta 50 por request. Para el detalle del
 *    producto la app puede usar las primeras 3 y un "ver todas". El nombre
 *    del autor viene resuelto desde `app_user.name` (JOIN en el repo).
 *
 *  - **Validacion de pertenencia al pedido**: por ahora cualquier user
 *    autenticado puede resenar cualquier producto. Si en el futuro vienen
 *    spam o falsificaciones, se valida que el user haya pedido ese producto
 *    en algun pedido ENTREGADO (JOIN order_item → orders).
 */
class ReviewService(
    private val repo: ReviewRepository,
) {
    companion object {
        const val MAX_TEXTO_CHARS = 500
    }

    suspend fun crearOActualizar(
        userId: UUID,
        productId: UUID,
        estrellas: Int,
        texto: String,
    ): ResenaDto {
        if (estrellas !in 1..5) {
            throw ValidationException("Las estrellas deben estar entre 1 y 5.")
        }
        val limpio = texto.trim()
        if (limpio.length > MAX_TEXTO_CHARS) {
            throw ValidationException("El comentario no puede tener más de $MAX_TEXTO_CHARS caracteres.")
        }
        return repo.upsert(productId, userId, estrellas, limpio).toDto()
    }

    suspend fun listar(productId: UUID): List<ResenaDto> =
        repo.listarPorProducto(productId).map { it.toDto() }

    suspend fun miResena(userId: UUID, productId: UUID): ResenaDto? =
        repo.miResena(productId, userId)?.toDto()

    private fun ReviewRepository.ResenaRow.toDto() = ResenaDto(
        id = id.toString(),
        productoId = productId.toString(),
        autorNombre = autorNombre,
        autorUserId = userId.toString(),
        estrellas = estrellas,
        texto = texto,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
