package cl.frutapp.app.data

import cl.frutapp.shared.dto.UserDto

/**
 * Sesión en memoria (tokens + usuario actual). Suficiente para el demo; se pierde
 * al cerrar la app. Cuando haga falta persistir → multiplatform-settings.
 */
object TokenStore {
    var accessToken: String? = null
        private set
    var refreshToken: String? = null
        private set
    var user: UserDto? = null
        private set

    val isLoggedIn: Boolean get() = accessToken != null

    fun save(access: String, refresh: String, user: UserDto) {
        accessToken = access
        refreshToken = refresh
        this.user = user
    }

    fun clear() {
        accessToken = null
        refreshToken = null
        user = null
    }
}
