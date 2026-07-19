package cl.frutapp.app.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.geometry.Offset
import java.io.ByteArrayOutputStream
import kotlin.math.max

actual fun renderizarFirmaPng(
    trazos: List<List<Offset>>,
    ancho: Int,
    alto: Int,
): ByteArray {
    // El canvas del usuario puede ser 0x0 si la pantalla nunca midió (edge
    // case defensivo). Forzamos un tamaño minimo para que Bitmap.createBitmap
    // no crashee y el PNG sea al menos abrible.
    val w = max(1, ancho)
    val h = max(1, alto)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.WHITE)
    val paint = Paint().apply {
        color = Color.BLACK
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    trazos.forEach { puntos ->
        if (puntos.size < 2) {
            // Trazo de 1 punto: dot pequeño (util para firmas con "puntitos").
            puntos.firstOrNull()?.let { p ->
                canvas.drawCircle(p.x, p.y, 3f, Paint(paint).apply { style = Paint.Style.FILL })
            }
            return@forEach
        }
        val path = Path()
        path.moveTo(puntos[0].x, puntos[0].y)
        for (i in 1 until puntos.size) {
            path.lineTo(puntos[i].x, puntos[i].y)
        }
        canvas.drawPath(path, paint)
    }
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
    bmp.recycle()
    return out.toByteArray()
}
