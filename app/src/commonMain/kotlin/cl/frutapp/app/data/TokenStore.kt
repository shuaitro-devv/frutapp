package cl.frutapp.app.data

import cl.frutapp.shared.dto.UserDto
import kotlinx.serialization.json.Json

/**
 * Sesión del usuario (tokens + datos). En memoria para acceso rápido, respaldada en
 * [SessionStorage] para que sobreviva al cerrar la app. Llamar [restore] al arrancar.
 */
object TokenStore {
    private const val K_ACCESS = "access_token"
    private const val K_REFRESH = "refresh_token"
    private const val K_USER = "user"
    private val json = Json { ignoreUnknownKeys = true }

    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set
    var user: UserDto? = null
        private set

    val isLoggedIn: Boolean get() = accessToken != null

    /** Carga la sesión persistida a memoria (idempotente). */
    fun restore() {
        accessToken = SessionStorage.getString(K_ACCESS)
        refreshToken = SessionStorage.getString(K_REFRESH)
        user = SessionStorage.getString(K_USER)?.let {
            runCatching { json.decodeFromString(UserDto.serializer(), it) }.getOrNull()
        }
    }

    fun save(access: String, refresh: String, user: UserDto) {
        accessToken = access
        refreshToken = refresh
        this.user = user
        SessionStorage.putString(K_ACCESS, access)
        SessionStorage.putString(K_REFRESH, refresh)
        SessionStorage.putString(K_USER, json.encodeToString(UserDto.serializer(), user))
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        user = null
        SessionStorage.remove(K_ACCESS)
        SessionStorage.remove(K_REFRESH)
        SessionStorage.remove(K_USER)
    }
}
