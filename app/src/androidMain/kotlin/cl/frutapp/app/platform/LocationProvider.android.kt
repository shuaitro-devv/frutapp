package cl.frutapp.app.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

actual class LocationProvider internal constructor(
    private val context: Context,
    private val pedirPermisos: suspend () -> Boolean,
) {
    private val fused by lazy { LocationServices.getFusedLocationProviderClient(context) }
    private val geocoder by lazy { Geocoder(context, Locale("es", "CL")) }

    actual fun permisoConcedido(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    actual suspend fun solicitarPermiso(): Boolean {
        if (permisoConcedido()) return true
        return pedirPermisos()
    }

    actual suspend fun obtenerActual(): Coordenadas? {
        if (!permisoConcedido()) return null
        return try {
            // getCurrentLocation prioriza una lectura fresca (vs lastLocation
            // que puede devolver basura cacheada). HIGH_ACCURACY usa GPS si
            // esta disponible, y cae a network si no.
            @Suppress("MissingPermission")
            val loc = fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            loc?.let { Coordenadas(it.latitude, it.longitude) }
        } catch (e: Throwable) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            null
        }
    }

    actual suspend fun reverseGeocode(lat: Double, lng: Double): String? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: version async con callback (la sincrona quedo deprecated).
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    cont.resume(addresses.firstOrNull()?.getAddressLine(0))
                }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(lat, lng, 1)?.firstOrNull()?.getAddressLine(0)
            }
        }
    }.getOrNull()?.takeIf { it.isNotBlank() }
}

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    val context = LocalContext.current
    // Estado compartido para el resultado del launcher: cuando el sistema
    // responde, completamos la continuacion suspendida que pedia el permiso.
    var pendiente: ((Boolean) -> Unit)? = null
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val ok = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        pendiente?.invoke(ok)
        pendiente = null
    }
    return remember {
        LocationProvider(
            context = context,
            pedirPermisos = {
                suspendCancellableCoroutine { cont ->
                    pendiente = { ok -> cont.resume(ok) }
                    launcher.launch(arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ))
                }
            }
        )
    }
}
