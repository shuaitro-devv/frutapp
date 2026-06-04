package cl.frutapp.app.navigation

import cafe.adriel.voyager.core.screen.Screen
import cl.frutapp.app.navigation.home.HomeScreen
import cl.frutapp.app.navigation.picker.PickerHomeScreen
import cl.frutapp.app.navigation.repartidor.RepartidorHomeScreen
import cl.frutapp.shared.dto.UserDto

/**
 * Decide a que Home enviar al usuario tras login/verify/restore-session.
 *
 * Fuente de verdad: [UserDto.roles] (lista RBAC, ej. ["picker", "cliente"]). Caemos a la
 * columna legacy [UserDto.role] si la lista viene vacia — eso pasa solo con backends viejos
 * (la respuesta nueva siempre la incluye) o si un usuario quedo sin user_role asignado.
 *
 * Precedencia: el rol staff "mas operativo" gana sobre el cliente. Si un usuario tiene
 * picker + cliente (caso de testeo), va al picker. Si tiene admin + cliente, va al cliente
 * (admin no tiene Home propia hoy; se opera via backoffice externo segun acuerdo).
 *
 * Single-source: usado por LoginScreen, VerifyCodeScreen, SplashScreen. Si manana agregamos
 * proveedor/soporte/etc., aca se decide — las pantallas de auth no tocan.
 */
fun homeForUser(user: UserDto?): Screen {
    if (user == null) return HomeScreen()
    val roles = user.roles.ifEmpty { listOfNotNull(user.role.lowercase().takeIf { it.isNotBlank() }) }
    return when {
        "picker" in roles -> PickerHomeScreen()
        "repartidor" in roles -> RepartidorHomeScreen()
        else -> HomeScreen()
    }
}
