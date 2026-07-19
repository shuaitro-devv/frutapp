package cl.frutapp.app.platform

import androidx.compose.ui.geometry.Offset

/**
 * Encoder de firma → PNG. Recibe la lista de trazos capturados en el canvas
 * (cada trazo es una lista de puntos consecutivos) + el tamaño del canvas en
 * px lógicos y devuelve un PNG con fondo blanco y trazos negros.
 *
 * Vive en platform porque necesita el runtime de bitmap del OS (Android
 * Bitmap + Canvas + PNG encoder).
 */
expect fun renderizarFirmaPng(
    trazos: List<List<Offset>>,
    ancho: Int,
    alto: Int,
): ByteArray
