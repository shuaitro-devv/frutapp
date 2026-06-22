package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.ImageBitmap
import cl.frutapp.app.data.remote.ReviewApi
import cl.frutapp.shared.dto.ResenaDto
import kotlinx.datetime.toLocalDateTime

data class Resena(
    val id: String,
    val nombre: String,
    val estrellas: Int,
    val fecha: String,
    val texto: String,
    /** True si la escribió el usuario actual (se puede editar). */
    val propia: Boolean = false,
    /** Foto local seleccionada por el usuario (no sube al backend en V1). Solo
     *  vive el proceso; al cargar desde el backend nunca tiene imagen. */
    val imagen: ImageBitmap? = null
)

/**
 * Reseñas por producto. Hidrata desde el backend (V32 `producto_resena`) y
 * cachea en memoria para que las pantallas las consuman sincronicamente como
 * estado Compose.
 *
 * Flujo tipico:
 *  1. La pantalla llama [cargar] en un LaunchedEffect al entrar (idempotente).
 *  2. Mientras tanto la lista refleja lo cacheado de cargas previas (o vacio).
 *  3. Al guardar/editar via [guardarRemoto], el POST hace upsert en backend y
 *     al volver mutamos el cache local con el dto real (estable en id, fecha
 *     y nombre del autor).
 *
 * El campo `imagen` se mantiene local: en V1 NO subimos fotos adjuntas a la
 * resena (foto evidencia ya cubre la prueba visual del item por parte del
 * picker; resenas de cliente con texto y estrellas son suficientes).
 */
object ResenasStore {
    private val porProducto = mutableStateMapOf<String, SnapshotStateList<Resena>>()
    private val cargadosBackend = mutableSetOf<String>()
    private val api = ReviewApi()

    /** Lista mutable de resenas de un producto. Si nunca se cargo, devuelve
     *  vacia — la pantalla deberia llamar [cargar] en paralelo. */
    fun resenas(productoId: String): SnapshotStateList<Resena> =
        porProducto.getOrPut(productoId) { mutableStateListOf() }

    /** Cantidad de resenas en el cache para este producto. */
    fun extras(productoId: String): Int = resenas(productoId).size

    /** La resena del usuario actual para este producto, o null si todavia no
     *  la escribio (o no fue cargada del backend). */
    fun miResena(productoId: String): Resena? = resenas(productoId).firstOrNull { it.propia }

    /** Hidrata el cache desde el backend. Idempotente: si ya se cargo, no
     *  vuelve a pegar (salvo [force]=true). Skip si [productoId] no parece UUID
     *  (productos del DemoCatalog usan slug, no se pueden resenar). Falla
     *  silenciosa → deja el cache como estaba (lista vacia si nunca se hidrato). */
    suspend fun cargar(productoId: String, miUserId: String?, force: Boolean = false) {
        if (!productoId.parecesUuid()) return
        if (!force && productoId in cargadosBackend) return
        val resp = runCatching { api.listar(productoId) }.getOrNull() ?: return
        val lista = resp.map { it.toUi(miUserId) }
        val st = resenas(productoId)
        st.clear()
        st.addAll(lista)
        cargadosBackend.add(productoId)
    }

    /** Crear o actualizar mi resena. Hace POST upsert en backend; al volver
     *  reemplaza la resena local del usuario por la persistida (id estable,
     *  fecha del backend, autorNombre real). Devuelve la Resena resultante
     *  o null si fallo o si el productoId no es UUID (DemoCatalog). */
    suspend fun guardarRemoto(productoId: String, estrellas: Int, texto: String): Resena? {
        if (!productoId.parecesUuid()) return null
        val dto = runCatching { api.guardar(productoId, estrellas, texto) }.getOrNull() ?: return null
        val miUserId = TokenStore.user?.id
        val r = dto.toUi(miUserId)
        val st = resenas(productoId)
        val idx = st.indexOfFirst { it.propia }
        if (idx >= 0) st[idx] = r else st.add(0, r)
        cargadosBackend.add(productoId)
        return r
    }

    private fun ResenaDto.toUi(miUserId: String?): Resena = Resena(
        id = id,
        nombre = autorNombre,
        estrellas = estrellas,
        fecha = fechaCorta(createdAt),
        texto = texto,
        propia = miUserId != null && autorUserId == miUserId,
    )
}

/** Heuristica: el string parece un UUID si tiene 36 chars con los 4 guiones
 *  en su posicion. No valida en profundidad — solo descarta slugs y vacios. */
private fun String.parecesUuid(): Boolean =
    length == 36 && this[8] == '-' && this[13] == '-' && this[18] == '-' && this[23] == '-'

/** Formato corto "hoy" / "ayer" / "12 jun" para mostrar la fecha de la resena.
 *  No usa Clock.System.now() para evitar el llamado a getEpochMilliseconds en
 *  scripts; aca SI es legitimo (la app puede llamar a Clock cuando quiera). */
private fun fechaCorta(iso: String): String = runCatching {
    val instant = kotlinx.datetime.Instant.parse(iso)
    val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
    val fecha = instant.toLocalDateTime(tz).date
    val hoy = kotlinx.datetime.Clock.System.now().toLocalDateTime(tz).date
    val diff = fecha.toEpochDays() - hoy.toEpochDays()
    when (diff) {
        0 -> "hoy"
        -1 -> "ayer"
        in -7..-2 -> "hace ${-diff} días"
        else -> "${fecha.dayOfMonth} ${mesCorto(fecha.monthNumber)}"
    }
}.getOrDefault("")

private fun mesCorto(n: Int): String = when (n) {
    1 -> "ene"; 2 -> "feb"; 3 -> "mar"; 4 -> "abr"; 5 -> "may"; 6 -> "jun"
    7 -> "jul"; 8 -> "ago"; 9 -> "sep"; 10 -> "oct"; 11 -> "nov"; 12 -> "dic"
    else -> ""
}
