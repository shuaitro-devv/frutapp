package cl.frutapp.app.platform

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

/**
 * Mapa Android via maps-compose. UI minima: sin botones, sin zoom controls
 * (gestures siguen funcionando). El marker se anima al cambiar de posicion
 * por el `MarkerState`.
 *
 * Si MAPS_API_KEY no esta seteado en el manifest, GoogleMap se renderiza
 * gris pero la app no crashea. La pantalla cliente ya tiene fallback
 * textual al lado del mapa para que el flow siga siendo usable sin clave.
 */
@Composable
actual fun MapaCompose(
    centerLat: Double,
    centerLng: Double,
    markerLat: Double?,
    markerLng: Double?,
    zoom: Float,
    modifier: Modifier,
) {
    val center = LatLng(centerLat, centerLng)
    val cameraState: CameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, zoom)
    }
    // Si el centro cambia (ej: recibimos primera lectura GPS), recentrar
    // el mapa suavemente para que el cliente lo vea moverse.
    LaunchedEffect(center) {
        cameraState.animate(CameraUpdateFactory.newLatLngZoom(center, zoom))
    }
    GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraState,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = false,
            mapToolbarEnabled = false,
        ),
        properties = MapProperties(
            isMyLocationEnabled = false,  // requiere permisos; lo manejamos manual
        ),
    ) {
        if (markerLat != null && markerLng != null) {
            val markerState = remember { MarkerState(LatLng(markerLat, markerLng)) }
            // Actualizar la posicion al recibir nuevos lat/lng del backend.
            LaunchedEffect(markerLat, markerLng) {
                markerState.position = LatLng(markerLat, markerLng)
            }
            Marker(
                state = markerState,
                title = "Tu repartidor",
            )
        }
    }
}

