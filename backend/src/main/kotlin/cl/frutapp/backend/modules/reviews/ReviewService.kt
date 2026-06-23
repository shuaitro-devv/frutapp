package cl.frutapp.backend.modules.reviews

import cl.frutapp.backend.error.ValidationException
import cl.frutapp.backend.modules.media.StorageService
import cl.frutapp.shared.dto.ResenaDto
import java.util.UUID

/**
 * Logica de resenas de producto.
 *
 *  - **Crear / editar**: 1-5 estrellas obligatorias, texto opcional (<=500
 *    chars), imagen opcional (JPEG/PNG, <=5 MB). Upsert: una resena por
 *    (productId, userId); si el cliente vuelve a enviar, se actualiza.
 *
 *  - **Imagen**:
 *      - bytes != null → subimos a MinIO key `reviews/{productId}/{resenaId}.{ext}`
 *        y guardamos image_key.
 *      - bytes == null y removerImagen=false → no tocamos la imagen anterior.
 *      - bytes == null y removerImagen=true → borramos image_key y el objeto.
 *      Si reemplazamos una imagen anterior, borramos el objeto viejo
 *      best-effort (si falla, queda huerfano benigno).
 *
 *  - **Listar**: ultimo arriba, hasta 50 por request. autorNombre desde el
 *    JOIN con app_user. imagenUrl presignada en cada GET (TTL 1h).
 */
class ReviewService(
    private val repo: ReviewRepository,
    private val storage: StorageService?,
) {
    companion object {
        const val MAX_TEXTO_CHARS = 500
        private const val MAX_IMAGE_BYTES = 5L * 1024 * 1024  // 5 MB
        private val MIME_WHITELIST = setOf("image/jpeg", "image/png")
    }

    suspend fun crearOActualizar(
        userId: UUID,
        productId: UUID,
        estrellas: Int,
        texto: String,
        imagenBytes: ByteArray? = null,
        imagenContentType: String? = null,
        removerImagen: Boolean = false,
    ): ResenaDto {
        if (estrellas !in 1..5) {
            throw ValidationException("Las estrellas deben estar entre 1 y 5.")
        }
        val limpio = texto.trim()
        if (limpio.length > MAX_TEXTO_CHARS) {
            throw ValidationException("El comentario no puede tener más de $MAX_TEXTO_CHARS caracteres.")
        }

        // Reservamos un UUID antes de subir bytes a MinIO; asi la key del
        // bucket usa el mismo id que la fila DB y mensaje<->objeto matchean.
        val resenaId = UUID.randomUUID()
        val hayImagenNueva = imagenBytes != null && imagenBytes.isNotEmpty()
        val keyAnterior = repo.imageKeyActual(productId, userId)

        val imageKeyParaUpsert: String? = when {
            hayImagenNueva -> {
                val store = storage
                    ?: throw ValidationException("Imágenes deshabilitadas en este ambiente.")
                val ct = imagenContentType ?: "application/octet-stream"
                validarImagen(imagenBytes!!, ct)
                val ext = if (ct == "image/png") "png" else "jpg"
                val key = "reviews/$productId/$resenaId.$ext"
                store.subir(key, imagenBytes, ct)
                key
            }
            removerImagen -> ""     // marker para el repo: borrar
            else -> null            // no tocar la imagen
        }

        val row = repo.upsert(productId, userId, estrellas, limpio, imageKeyParaUpsert, forcedId = resenaId)

        // Cleanup best-effort del objeto anterior si reemplazamos o quitamos.
        val store = storage
        if (keyAnterior != null && keyAnterior != row.imageKey && store != null) {
            runCatching { store.borrar(keyAnterior) }
        }

        return row.toDto()
    }

    suspend fun listar(productId: UUID): List<ResenaDto> =
        repo.listarPorProducto(productId).map { it.toDto() }

    suspend fun miResena(userId: UUID, productId: UUID): ResenaDto? =
        repo.miResena(productId, userId)?.toDto()

    private fun validarImagen(bytes: ByteArray, contentType: String) {
        if (contentType !in MIME_WHITELIST) {
            throw ValidationException("Tipo de imagen no permitido. Solo JPEG o PNG.")
        }
        if (bytes.size.toLong() > MAX_IMAGE_BYTES) {
            throw ValidationException("La imagen pesa más de 5 MB. Sácala con menos resolución o comprímela.")
        }
        if (!verificarMagicNumber(bytes, contentType)) {
            throw ValidationException("La imagen está corrupta o no coincide con el tipo declarado.")
        }
    }

    private fun verificarMagicNumber(bytes: ByteArray, contentType: String): Boolean {
        if (bytes.size < 8) return false
        return when (contentType) {
            "image/jpeg" -> bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
            "image/png" -> bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()
            else -> false
        }
    }

    private fun ReviewRepository.ResenaRow.toDto() = ResenaDto(
        id = id.toString(),
        productoId = productId.toString(),
        autorNombre = autorNombre,
        autorUserId = userId.toString(),
        estrellas = estrellas,
        texto = texto,
        imagenUrl = imageKey?.let { storage?.urlFirmada(it) },
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString(),
    )
}
