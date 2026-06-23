package cl.frutapp.backend.modules.rewards

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.CuponDto
import kotlin.random.Random
import java.util.UUID

/**
 * Logica de canje de FrutCoins → cupon.
 *
 *  - Valida monto > 0 y saldo suficiente del usuario (saldo = SUM(ledger.delta)).
 *  - Idempotencia: si el cliente vuelve a postear con la misma idempotency_key,
 *    devolvemos el cupon ya existente sin debitar de nuevo. UNIQUE(user_id,
 *    idempotency_key) en BD garantiza atomicidad.
 *  - Genera un codigo de cupon legible, formato `FRUT-XXXX-XXXX` (8 chars
 *    alfanumericos del alfabeto sin 0/O/I/1 para evitar ambiguedad al leer).
 *  - TTL del cupon: configurable, default 30 dias desde la creacion.
 */
class RewardService(
    private val repo: RewardRepository,
) {
    companion object {
        const val TTL_DIAS = 30
        private const val CODIGO_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"  // sin 0/O/I/1
        private const val CODIGO_LEN = 8  // FRUT-XXXX-XXXX → 8 chars
        private const val MAX_RECOMPENSA_CHARS = 100
        private const val MAX_REINTENTOS_CODIGO = 5
    }

    suspend fun canjear(
        userId: UUID,
        monto: Int,
        recompensa: String,
        idempotencyKey: String,
    ): CuponDto {
        if (monto <= 0) throw ValidationException("El monto debe ser mayor a 0.")
        if (idempotencyKey.isBlank()) throw ValidationException("Falta el idempotency key.")
        val recompensaLimpia = recompensa.trim()
        if (recompensaLimpia.isEmpty()) throw ValidationException("La descripcion de la recompensa es obligatoria.")
        if (recompensaLimpia.length > MAX_RECOMPENSA_CHARS) {
            throw ValidationException("La descripcion de la recompensa es demasiado larga.")
        }

        // Idempotencia: si ya hubo un POST con esta key, devolvemos lo mismo.
        repo.cuponPorIdempotency(userId, idempotencyKey)?.let { return it.toDto() }

        // Validar saldo antes de descontar.
        val saldo = repo.saldoActual(userId)
        if (saldo < monto) {
            throw ValidationException("Saldo insuficiente. Tienes $saldo FrutCoins.")
        }

        // Generar codigo unico. Si tenemos colision (improbable: 32^8 = 1.1e12),
        // reintentamos hasta MAX_REINTENTOS. UNIQUE en BD blinda race.
        var lastError: Throwable? = null
        repeat(MAX_REINTENTOS_CODIGO) {
            val codigo = generarCodigo()
            val resultado = runCatching {
                repo.canjear(userId, monto, recompensaLimpia, idempotencyKey, codigo, TTL_DIAS)
            }
            resultado.onSuccess { return it.toDto() }
            lastError = resultado.exceptionOrNull()
        }
        throw IllegalStateException("No se pudo generar un codigo de cupon unico tras $MAX_REINTENTOS_CODIGO intentos.", lastError)
    }

    suspend fun listar(userId: UUID): List<CuponDto> =
        repo.listarPorUsuario(userId).map { it.toDto() }

    /** Marca un cupon propio como usado. 404 si no es del usuario o ya no esta
     *  activo. Util para futuro: el back office o el checkout lo invoca cuando
     *  efectivamente se aplica el descuento. */
    suspend fun usar(userId: UUID, cuponIdStr: String) {
        val cuponId = runCatching { UUID.fromString(cuponIdStr) }.getOrNull()
            ?: throw ValidationException("Id de cupon invalido.")
        val ok = repo.marcarUsado(userId, cuponId)
        if (!ok) throw NotFoundException("Cupon no encontrado o ya usado.")
    }

    private fun generarCodigo(): String {
        val chars = CharArray(CODIGO_LEN) { CODIGO_ALPHABET[Random.nextInt(CODIGO_ALPHABET.length)] }
        // Insertar guion en la mitad para legibilidad: FRUT-ABCD-EFGH
        val mitad = CODIGO_LEN / 2
        return "FRUT-" + String(chars, 0, mitad) + "-" + String(chars, mitad, CODIGO_LEN - mitad)
    }

    private fun RewardRepository.CuponRow.toDto() = CuponDto(
        id = id.toString(),
        codigo = codigo,
        monto = monto,
        recompensa = recompensa,
        estado = estado,
        expiraEn = expiraEn?.toString(),
        usadoEn = usadoEn?.toString(),
        createdAt = createdAt.toString(),
    )
}
