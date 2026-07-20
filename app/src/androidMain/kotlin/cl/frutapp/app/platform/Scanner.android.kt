package cl.frutapp.app.platform

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * Implementación Android del [Scanner] con ZXing (`zxing-android-embedded`).
 * Abre `CaptureActivity` fullscreen; al leer un código llama [onScan] con el
 * texto crudo. Cancel back-hardware = no dispara nada (silent close, mismo
 * patrón que SelectorImagenes).
 */
actual class Scanner internal constructor(
    private val onEscanear: () -> Unit,
) {
    actual fun escanear() = onEscanear()
}

@Composable
actual fun rememberScanner(prompt: String, onScan: (String) -> Unit): Scanner {
    val launcher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (!contents.isNullOrBlank()) {
            onScan(contents)
        }
    }
    return remember(prompt) {
        Scanner(
            onEscanear = {
                launcher.launch(
                    ScanOptions()
                        .setPrompt(prompt)
                        .setBeepEnabled(true)
                        .setOrientationLocked(true)
                        // QR + los principales barcodes 1D. Sin PDF417/aztec
                        // que raro los veamos en productos de super/feria.
                        .setDesiredBarcodeFormats(
                            ScanOptions.QR_CODE,
                            ScanOptions.CODE_128,
                            ScanOptions.CODE_39,
                            ScanOptions.EAN_13,
                            ScanOptions.EAN_8,
                            ScanOptions.UPC_A,
                            ScanOptions.UPC_E,
                        )
                        .setBarcodeImageEnabled(false),
                )
            },
        )
    }
}
