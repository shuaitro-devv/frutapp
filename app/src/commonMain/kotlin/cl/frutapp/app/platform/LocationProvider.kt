package cl.frutapp.app.platform

/**
 * Acceso a la ubicacion del dispositivo. La implementacion Android usa
 * Fused Location Provider (Google Play Services). Incluye reverse geocoding
 * para autocompletar direcciones a partir de la posicion.
 *
 *  - [permisoConcedido]: verifica los permisos runtime (FINE / COARSE).
 *  - [solicitarPermiso]: dispara la UI del sistema para pedirlos. La
 *    coroutine vuelve con true/false segun la respuesta del usuario.
 *  - [obtenerActual]: one-shot, devuelve la ultima ubicacion conocida o
 *    null si no hay GPS / permisos / ubicacion disponible.
 *  - [reverseGeocode]: dado un lat/lng, devuelve la direccion legible o
 *    null si no se puede resolver.
 *
 * Las pantallas usan [rememberLocationProvider] que devuelve la instancia
 * bindeada al ciclo de vida del Composable. iOS pendiente.
 */
expect class LocationProvider {
    fun permisoConcedido(): Boolean
    suspend fun solicitarPermiso(): Boolean
    suspend fun obtenerActual(): Coordenadas?
    suspend fun reverseGeocode(lat: Double, lng: Double): String?
}

data class Coordenadas(val lat: Double, val lng: Double)

@androidx.compose.runtime.Composable
expect fun rememberLocationProvider(): LocationProvider
