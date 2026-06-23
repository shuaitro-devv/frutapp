package cl.frutapp.backend.modules.baskets

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.ActualizarCanastaRequest
import cl.frutapp.shared.dto.CanastaDto
import cl.frutapp.shared.dto.CanastaItemDto
import cl.frutapp.shared.dto.CrearCanastaRequest
import java.util.UUID

/**
 * Logica de canastas guardadas.
 *
 *  - Ownership: el user_id de la canasta debe coincidir con el caller; el
 *    repo ya filtra por userId en todas las queries (defensa en profundidad).
 *  - Crear / actualizar: nombre obligatorio, max 60 chars; items max 50;
 *    cada item con cantidad entre 1 y 100, gramos entre 50 y 10000 o null.
 *  - Actualizar es "reemplazar items" — sin diff: el cliente manda la lista
 *    completa, la BD borra los items previos y mete los nuevos en una
 *    transaccion (mas simple que ir item por item).
 */
class BasketService(
    private val repo: BasketRepository,
) {
    companion object {
        const val MAX_NOMBRE_CHARS = 60
        const val MAX_ITEMS = 50
        const val MAX_CANTIDAD = 100
        const val MIN_GRAMOS = 50
        const val MAX_GRAMOS = 10_000
    }

    suspend fun listar(userId: UUID): List<CanastaDto> =
        repo.listarPorUsuario(userId).map { it.toDto() }

    suspend fun cargar(userId: UUID, idStr: String): CanastaDto {
        val id = parseId(idStr)
        return repo.cargar(id, userId)?.toDto()
            ?: throw NotFoundException("Canasta no encontrada.")
    }

    suspend fun crear(userId: UUID, req: CrearCanastaRequest): CanastaDto {
        validarNombre(req.nombre)
        val items = req.items.map { parseItem(it.productoId, it.cantidad, it.gramos) }
        validarItems(items)
        return repo.crear(
            userId = userId,
            nombre = req.nombre.trim(),
            emoji = req.emoji.ifBlank { "🧺" },
            recordatorioMensual = req.recordatorioMensual,
            items = items,
        ).toDto()
    }

    suspend fun actualizar(userId: UUID, idStr: String, req: ActualizarCanastaRequest): CanastaDto {
        val id = parseId(idStr)
        val nombreNuevo = req.nombre
        if (nombreNuevo != null) validarNombre(nombreNuevo)
        val items = req.items?.map { parseItem(it.productoId, it.cantidad, it.gramos) }
        if (items != null) validarItems(items)
        return repo.actualizar(
            canastaId = id,
            userId = userId,
            nombre = nombreNuevo?.trim(),
            emoji = req.emoji,
            recordatorioMensual = req.recordatorioMensual,
            items = items,
        )?.toDto() ?: throw NotFoundException("Canasta no encontrada.")
    }

    suspend fun eliminar(userId: UUID, idStr: String) {
        val id = parseId(idStr)
        val ok = repo.eliminar(id, userId)
        if (!ok) throw NotFoundException("Canasta no encontrada.")
    }

    private fun parseId(idStr: String): UUID =
        runCatching { UUID.fromString(idStr) }.getOrNull()
            ?: throw ValidationException("Id de canasta inválido.")

    private fun validarNombre(nombre: String) {
        val limpio = nombre.trim()
        if (limpio.isEmpty()) throw ValidationException("El nombre de la canasta es obligatorio.")
        if (limpio.length > MAX_NOMBRE_CHARS) {
            throw ValidationException("El nombre de la canasta no puede tener más de $MAX_NOMBRE_CHARS caracteres.")
        }
    }

    private fun validarItems(items: List<BasketRepository.NuevoItem>) {
        if (items.size > MAX_ITEMS) {
            throw ValidationException("La canasta no puede tener más de $MAX_ITEMS productos.")
        }
        items.forEach { item ->
            if (item.cantidad !in 1..MAX_CANTIDAD) {
                throw ValidationException("La cantidad debe estar entre 1 y $MAX_CANTIDAD.")
            }
            if (item.gramos != null && item.gramos !in MIN_GRAMOS..MAX_GRAMOS) {
                throw ValidationException("Los gramos deben estar entre $MIN_GRAMOS y $MAX_GRAMOS.")
            }
        }
    }

    private fun parseItem(productoIdStr: String, cantidad: Int, gramos: Int?): BasketRepository.NuevoItem {
        val productId = runCatching { UUID.fromString(productoIdStr) }.getOrNull()
            ?: throw ValidationException("productoId inválido: '$productoIdStr'.")
        return BasketRepository.NuevoItem(productId, cantidad, gramos)
    }

    private fun BasketRepository.CanastaConItems.toDto() = CanastaDto(
        id = cabecera.id.toString(),
        nombre = cabecera.nombre,
        emoji = cabecera.emoji,
        recordatorioMensual = cabecera.recordatorioMensual,
        items = items.map { CanastaItemDto(
            id = it.id.toString(),
            productoId = it.productId.toString(),
            cantidad = it.cantidad,
            gramos = it.gramos,
            posicion = it.posicion,
        ) },
        createdAt = cabecera.createdAt.toString(),
        updatedAt = cabecera.updatedAt.toString(),
    )
}
