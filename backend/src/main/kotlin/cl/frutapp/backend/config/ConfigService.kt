package cl.frutapp.backend.config

import cl.frutapp.backend.error.NotFoundException
import cl.frutapp.backend.error.ValidationException
import cl.frutapp.shared.dto.AdminConfigEntryDto
import kotlinx.datetime.Clock

/**
 * Servicio del back office para `app_config`. Solo edita entradas existentes (no crea
 * keys nuevas — esas vienen por migración para mantener el contrato del cliente estable).
 * Valida que el nuevo `value` parsee al `type` declarado en BD antes de escribir.
 */
class ConfigService(private val repo: ConfigRepository) {

    fun listAll(): List<AdminConfigEntryDto> = ConfigCache.all().map { it.toDto() }

    /** Edita una key. Devuelve la entrada actualizada según el caché POST-refresh.
     *  El caller (route) debe llamar a [ConfigCache.refresh] después para que el cambio
     *  sea visible inmediatamente; sin eso, esperaría hasta 60s al próximo tick. */
    suspend fun update(key: String, rawValue: String): AdminConfigEntryDto {
        val entry = ConfigCache.entry(key)
            ?: throw NotFoundException("La configuración '$key' no existe.")
        val value = rawValue.trim()
        if (value.isEmpty()) throw ValidationException("El valor no puede estar vacío.")
        validateAgainstType(value, entry.type, key)
        val affected = repo.update(key, value)
        if (affected == 0) throw NotFoundException("La configuración '$key' no existe.")
        // Devolvemos el dato recién escrito sin depender del refresh del caché (seria carrera).
        // updatedAt fresco: si reusaramos el del entry viejo, la UI del back office mostraria
        // la fecha de la edicion anterior justo despues de haber editado (confuso).
        return entry.copy(value = value, updatedAt = Clock.System.now()).toDto()
    }

    private fun validateAgainstType(value: String, type: String, key: String) {
        val normalized = type.uppercase()
        val ok = when (normalized) {
            "INT" -> value.toIntOrNull() != null
            "DECIMAL" -> value.toDoubleOrNull() != null
            "BOOL" -> value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true)
            "STRING" -> true
            else -> throw ValidationException("Tipo desconocido '$type' para '$key'.")
        }
        if (!ok) throw ValidationException("El valor '$value' no es un $normalized válido para '$key'.")
    }
}

private fun ConfigEntry.toDto() = AdminConfigEntryDto(
    key = key,
    value = value,
    type = type,
    description = description,
    clientVisible = clientVisible,
    updatedAt = updatedAt.toString()
)
