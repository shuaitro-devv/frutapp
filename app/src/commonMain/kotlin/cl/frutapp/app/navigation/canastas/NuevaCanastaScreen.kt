package cl.frutapp.app.navigation.canastas

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.CanastaItem
import cl.frutapp.app.data.CanastaStore
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

private val EMOJIS_CANASTA = listOf("🧺", "🏠", "🔥", "💪", "👶", "🍅", "🌿", "🍎", "🥗", "🍇", "🎉", "🎄")

/** Crear nueva canasta: emoji picker + nombre. Si se invoca con `itemsIniciales`
 *  (típicamente desde el carrito o un pedido), la canasta arranca con esos productos. */
class NuevaCanastaScreen(
    private val itemsIniciales: List<CanastaItem> = emptyList()
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var nombre by remember { mutableStateOf("") }
        var emoji by remember { mutableStateOf("🧺") }

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
                    Text("Nueva canasta", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp)) {
                    Spacer(Modifier.height(16.dp))
                    Text("Elige un ícono", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    EmojiPicker(seleccionado = emoji, onSeleccionar = { emoji = it })

                    Spacer(Modifier.height(22.dp))
                    Text("Nombre de la canasta", color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    FrutTextField(
                        value = nombre,
                        onValueChange = { nombre = it },
                        label = "Ej: Casa, Asado, Niños…"
                    )

                    Spacer(Modifier.height(20.dp))
                    if (itemsIniciales.isNotEmpty()) {
                        Text(
                            "Vas a guardar ${itemsIniciales.size} producto(s) en esta canasta.",
                            color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Text(
                            "Después podrás agregarle productos desde el detalle de cada uno o desde tu carrito.",
                            color = FrutAppColors.InkMuted, fontSize = 12.sp
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
                    FrutButtonPrimary(
                        text = "Crear canasta",
                        enabled = nombre.isNotBlank(),
                        onClick = {
                            val c = CanastaStore.crear(nombre = nombre.trim(), emoji = emoji, items = itemsIniciales)
                            showToast("¡Canasta creada!")
                            // Navegar al detalle de la canasta recién creada (reemplazando esta).
                            navigator.replace(CanastaDetailScreen(c.id))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmojiPicker(seleccionado: String, onSeleccionar: (String) -> Unit) {
    // 2 filas de 6 emojis cada una
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EMOJIS_CANASTA.chunked(6).forEach { fila ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fila.forEach { e ->
                    val sel = e == seleccionado
                    Box(
                        modifier = Modifier.weight(1f).height(54.dp)
                            .background(if (sel) FrutAppColors.Brand400 else FrutAppColors.Brand50, RoundedCornerShape(12.dp))
                            .border(if (sel) 0.dp else 1.dp, FrutAppColors.Brand100, RoundedCornerShape(12.dp))
                            .clickable { onSeleccionar(e) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(e, fontSize = 26.sp)
                    }
                }
            }
        }
    }
}
