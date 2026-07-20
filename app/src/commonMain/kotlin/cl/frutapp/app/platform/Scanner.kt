package cl.frutapp.app.platform

import androidx.compose.runtime.Composable

/**
 * Escáner de QR / código de barras. Devuelve el string decodificado via
 * [onScan]; si el usuario cancela con back, se dispara [onCancelar] (que en
 * la práctica solo cierra el visor).
 *
 * Uso: llamar `rememberScanner { texto -> ... }` en el composable, y luego
 * `scanner.escanear()` al tapear un botón. Abre una activity fullscreen con
 * la cámara + overlay + auto-decode.
 *
 * Casos de uso previstos:
 *  - Picker verifica que agarró el producto correcto (barcode del envase).
 *  - Repartidor escanea el QR del voucher al retirar en bodega.
 *
 * Implementación Android: ZXing + CameraX via `zxing-android-embedded`.
 */
expect class Scanner {
    fun escanear()
}

@Composable
expect fun rememberScanner(
    prompt: String = "Apunta la cámara al código",
    onScan: (String) -> Unit,
): Scanner
