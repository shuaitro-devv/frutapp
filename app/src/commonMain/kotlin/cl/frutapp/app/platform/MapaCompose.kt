package cl.frutapp.app.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Mapa Compose multiplataforma. Android: Google Maps SDK + maps-compose.
 * Una unica API simple: muestra un mapa centrado y, opcionalmente, un
 * marker del repartidor moviendose.
 *
 * Para mas APIs (zoom controls, polilines, multiples markers, MyLocation
 * button) extender este expect.
 *
 *  - [centerLat]/[centerLng]: punto al que el mapa esta enfocado al cargar.
 *  - [markerLat]/[markerLng]: si NO son null, dibuja un marker (rotacion
 *    sin acimuth — punto fijo). Si cambian, el marker se anima al nuevo
 *    punto en el next recompose.
 *  - [zoom]: nivel de zoom inicial (10 = ciudad, 14 = barrio, 17 = calle).
 */
@Composable
expect fun MapaCompose(
    centerLat: Double,
    centerLng: Double,
    markerLat: Double? = null,
    markerLng: Double? = null,
    zoom: Float = 14f,
    modifier: Modifier = Modifier,
)
