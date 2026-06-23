@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.navigation.shop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.Producto
import cl.frutapp.app.data.ResenasStore
import cl.frutapp.app.data.TokenStore
import cl.frutapp.app.data.remote.CatalogApi
import cl.frutapp.app.data.toProducto
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutLoader
import cl.frutapp.app.ui.rememberImagePickerState
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import cl.frutapp.shared.dto.OrderItemDto
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource

/**
 * Califica tu compra: a partir de los items de un pedido entregado, mapea cada
 * uno al catalogo (por imageKey) y deja calificar producto por producto. Cada
 * card tiene su propio picker de foto: si el usuario adjunta una, sube al
 * backend junto con las estrellas y el comentario (foto en la card del
 * producto en su detalle, persistida en MinIO).
 */
class CalificarPedidoScreen(private val items: List<OrderItemDto>) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        var productos by remember { mutableStateOf<List<Producto>?>(null) }
        var error by remember { mutableStateOf(false) }
        var guardando by remember { mutableStateOf(false) }
        val estrellas = remember { mutableStateMapOf<String, Int>() }
        val comentarios = remember { mutableStateMapOf<String, String>() }
        // Bytes de la foto que el usuario adjunto a cada producto. Clave =
        // p.id (slug); lo escribe la ProductoCalificable via callback y lo lee
        // el bloque de envio. Si no hay entry, no se toca la foto previa.
        val fotos = remember { mutableStateMapOf<String, ByteArray>() }

        LaunchedEffect(Unit) {
            val catalogoResult = runCatching { CatalogApi().products() }
            val catalogo = catalogoResult.getOrNull()
            if (catalogo == null) {
                catalogoResult.exceptionOrNull()?.let {
                    cl.frutapp.app.ui.ErrorReporter.report(screen = "CalificarPedido", action = "load_catalog", error = it)
                }
                error = true
                return@LaunchedEffect
            }
            val porImagen = catalogo.associateBy { it.imageKey }
            val mapeados = items.mapNotNull { porImagen[it.imageKey]?.toProducto() }.distinctBy { it.id }
            // Hidratamos las resenas del usuario para cada producto en paralelo;
            // si ya califico antes, precargamos sus estrellas y comentario.
            // Usamos backendId (UUID); cuando es null (DemoCatalog) el store
            // skipea solo y nunca se llena. Para el form local sigue siendo
            // p.id (slug) la key, asi el estado de UI no depende de la red.
            val miUserId = TokenStore.user?.id
            mapeados.mapNotNull { p ->
                p.backendId?.let { bid -> async { ResenasStore.cargar(bid, miUserId) } }
            }.awaitAll()
            mapeados.forEach { p ->
                val bid = p.backendId ?: return@forEach
                ResenasStore.miResena(bid)?.let { mia ->
                    estrellas[p.id] = mia.estrellas
                    if (mia.texto.isNotBlank()) comentarios[p.id] = mia.texto
                }
            }
            productos = mapeados
        }

        Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable { navigator.pop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                    }
                    Text("Califica tu compra", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                val lista = productos
                when {
                    error -> Centrado("No pudimos cargar los productos.")
                    lista == null -> Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { FrutLoader() }
                    lista.isEmpty() -> Centrado("No hay productos para calificar.")
                    else -> {
                        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                            Text(
                                "¿Cómo estuvo tu pedido? Tu opinión ayuda a otros vecinos.",
                                color = FrutAppColors.InkMuted, fontSize = 13.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                            lista.forEach { p ->
                                ProductoCalificable(
                                    producto = p,
                                    estrellas = estrellas[p.id] ?: 0,
                                    onEstrellas = { estrellas[p.id] = it },
                                    comentario = comentarios[p.id] ?: "",
                                    onComentario = { comentarios[p.id] = it },
                                    fotos = fotos,
                                )
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        val hayCalificacion = lista.any { (estrellas[it.id] ?: 0) > 0 }
                        Box(modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp)) {
                            FrutButtonPrimary(
                                text = if (guardando) "Guardando…" else "Enviar calificación",
                                enabled = hayCalificacion && !guardando,
                                onClick = {
                                    if (guardando) return@FrutButtonPrimary
                                    guardando = true
                                    scope.launch {
                                        // Upsert paralelo: una resena por producto con
                                        // estrellas > 0 Y backendId conocido (los del
                                        // DemoCatalog no se pueden persistir). Si hay
                                        // foto adjunta para ese producto, va en multipart.
                                        data class Tarea(val pid: String, val s: Int, val txt: String, val bytes: ByteArray?)
                                        val tareas = lista.mapNotNull { p ->
                                            val s = estrellas[p.id] ?: 0
                                            val bid = p.backendId
                                            if (s > 0 && !bid.isNullOrEmpty())
                                                Tarea(bid, s, comentarios[p.id]?.trim() ?: "", fotos[p.id])
                                            else null
                                        }
                                        val resultados = tareas.map { t ->
                                            async {
                                                ResenasStore.guardarRemoto(
                                                    productoId = t.pid,
                                                    estrellas = t.s,
                                                    texto = t.txt,
                                                    imagenBytes = t.bytes,
                                                )
                                            }
                                        }.awaitAll()
                                        val exitos = resultados.count { it != null }
                                        val total = tareas.size
                                        if (exitos == total) {
                                            showToast("¡Gracias por calificar! ($exitos)")
                                        } else if (exitos == 0) {
                                            showToast("No pudimos guardar tus calificaciones.")
                                        } else {
                                            showToast("Calificamos $exitos de $total. Reintenta más tarde.")
                                        }
                                        guardando = false
                                        navigator.pop()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductoCalificable(
    producto: Producto,
    estrellas: Int,
    onEstrellas: (Int) -> Unit,
    comentario: String,
    onComentario: (String) -> Unit,
    fotos: SnapshotStateMap<String, ByteArray>,
) {
    // Picker propio de cada card. El padre mantiene el mapa de bytes por
    // producto.id; este Composable solo lee/escribe esa entry y dibuja el
    // preview cuando hay bytes.
    val picker = rememberImagePickerState()
    LaunchedEffect(picker.bytes) {
        val b = picker.bytes
        if (b != null) fotos[producto.id] = b
    }
    val foto = picker.imagen ?: fotos[producto.id]?.let { null }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).background(Color.White, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(producto.imagen),
                    contentDescription = producto.nombre,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(38.dp).padding(4.dp)
                )
            }
            Text(producto.nombre, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(start = 12.dp))
        }
        Row(modifier = Modifier.padding(top = 10.dp)) {
            for (i in 1..5) {
                Icon(
                    imageVector = if (i <= estrellas) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = "$i estrella(s)",
                    tint = FrutAppColors.AmberCoin,
                    modifier = Modifier.size(32.dp).clickable { onEstrellas(i) }.padding(end = 6.dp)
                )
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(64.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (comentario.isEmpty()) {
                Text("Comentario (opcional)", color = FrutAppColors.InkSoft, fontSize = 13.sp)
            }
            BasicTextField(
                value = comentario,
                onValueChange = onComentario,
                textStyle = TextStyle(color = FrutAppColors.Ink, fontSize = 13.sp),
                cursorBrush = SolidColor(FrutAppColors.Brand400),
                modifier = Modifier.fillMaxSize()
            )
        }
        // Boton "Adjuntar foto" + preview cuando hay una elegida. Reusable:
        // el ImagePickerState tiene `bytes` para el multipart y `imagen` para
        // el preview decodificado.
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val bmp = picker.imagen
            if (bmp != null) {
                Image(
                    bitmap = bmp,
                    contentDescription = "Foto adjunta",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).background(Color.Black, RoundedCornerShape(10.dp))
                        .border(1.dp, FrutAppColors.Brand200, RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Foto lista para enviar", color = FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Cambia o quita si fue equivocado", color = FrutAppColors.InkSoft, fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier.size(36.dp).clickable {
                        picker.limpiar()
                        fotos.remove(producto.id)
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, "Quitar foto", tint = FrutAppColors.InkSoft)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(10.dp))
                        .border(1.dp, FrutAppColors.Brand200, RoundedCornerShape(10.dp))
                        .clickable { picker.pick() }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Filled.AddPhotoAlternate,
                        contentDescription = null,
                        tint = FrutAppColors.Brand600,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Adjuntar foto (opcional)", color = FrutAppColors.Brand600, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Centrado(texto: String) {
    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
        Text(texto, color = FrutAppColors.InkMuted, fontSize = 14.sp)
    }
}
