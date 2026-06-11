package cl.frutapp.app.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

actual suspend fun recortarAvatar(
    srcBytes: ByteArray,
    srcX: Int,
    srcY: Int,
    srcSize: Int,
    outputPx: Int,
    calidad: Int
): ByteArray = withContext(Dispatchers.Default) {
    val src = BitmapFactory.decodeByteArray(srcBytes, 0, srcBytes.size)
        ?: throw IllegalArgumentException("No pude decodificar la imagen")
    try {
        // Clamp para no salirnos del bitmap: si las coords vinieron con error de
        // redondeo, recortar lo que se pueda y no crashear.
        val safeX = srcX.coerceIn(0, src.width - 1)
        val safeY = srcY.coerceIn(0, src.height - 1)
        val maxSize = minOf(src.width - safeX, src.height - safeY)
        val safeSize = srcSize.coerceIn(1, maxSize)
        val crop = Bitmap.createBitmap(src, safeX, safeY, safeSize, safeSize)
        val scaled = if (crop.width != outputPx)
            Bitmap.createScaledBitmap(crop, outputPx, outputPx, true)
        else crop
        ByteArrayOutputStream().use { baos ->
            scaled.compress(Bitmap.CompressFormat.JPEG, calidad, baos)
            if (scaled !== crop) scaled.recycle()
            crop.recycle()
            baos.toByteArray()
        }
    } finally {
        src.recycle()
    }
}

actual fun dimensionesImagen(bytes: ByteArray): Pair<Int, Int> {
    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    return opts.outWidth to opts.outHeight
}
