package cl.frutapp.app.platform

/**
 * Recorta una region cuadrada del bitmap original y la escala al tamano de salida.
 * Implementacion por plataforma: Android usa android.graphics.Bitmap.createBitmap +
 * createScaledBitmap + compress a JPEG.
 *
 * Params son coordenadas y tamano en pixeles del bitmap **original** decodificado
 * (NO del viewport ni del display). El caller (UI del crop) los calcula a partir
 * de la transformacion scale+offset que arme con los gestures.
 *
 * Devuelve bytes JPEG comprimidos listos para subir al backend.
 */
expect suspend fun recortarAvatar(
    srcBytes: ByteArray,
    srcX: Int,
    srcY: Int,
    srcSize: Int,
    outputPx: Int = 1024,
    calidad: Int = 85
): ByteArray

/** Lee solo el ancho/alto del bitmap sin decodificar todos los pixeles.
 *  Util para calcular escalas iniciales en el CropScreen sin cargarlo dos veces. */
expect fun dimensionesImagen(bytes: ByteArray): Pair<Int, Int>
