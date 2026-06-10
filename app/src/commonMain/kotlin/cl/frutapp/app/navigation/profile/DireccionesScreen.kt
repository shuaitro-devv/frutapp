package cl.frutapp.app.navigation.profile

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Place
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
import cl.frutapp.app.data.Direccion
import cl.frutapp.app.data.DireccionesStore
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/** Direcciones del usuario (mini-flujo dummy): listar, predeterminar, eliminar y agregar. */
class DireccionesScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var agregando by remember { mutableStateOf(false) }

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
                    Text("Direcciones", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                // navigationBarsPadding: respetar barra de nav del sistema (3-btn nav) para que el CTA no quede tapado.
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
                    DireccionesStore.items.forEach { dir ->
                        DireccionCard(
                            dir = dir,
                            esPredeterminada = dir.id == DireccionesStore.predeterminadaId,
                            onPredeterminar = {
                                DireccionesStore.setPredeterminada(dir.id)
                                showToast("Dirección predeterminada actualizada")
                            },
                            onEliminar = {
                                DireccionesStore.remove(dir.id)
                                showToast("Dirección eliminada")
                            }
                        )
                    }

                    if (agregando) {
                        FormularioDireccion(
                            onGuardar = { etiqueta, calle, comuna ->
                                DireccionesStore.add(etiqueta, calle, comuna)
                                agregando = false
                                showToast("Dirección agregada")
                            },
                            onCancelar = { agregando = false }
                        )
                    } else {
                        Box(modifier = Modifier.padding(top = 16.dp)) {
                            FrutButtonPrimary(text = "Agregar dirección", onClick = { agregando = true }, leadingIcon = Icons.Filled.Add)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DireccionCard(
    dir: Direccion,
    esPredeterminada: Boolean,
    onPredeterminar: () -> Unit,
    onEliminar: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 14.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(dir.etiqueta, color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (esPredeterminada) {
                        Box(modifier = Modifier.padding(start = 8.dp).background(FrutAppColors.Brand400, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Predeterminada", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(dir.calle, color = FrutAppColors.Ink, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
                Text(dir.comuna, color = FrutAppColors.InkSoft, fontSize = 12.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!esPredeterminada) {
                Text("Usar como predeterminada", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onPredeterminar))
            } else {
                Spacer(Modifier.size(1.dp))
            }
            Row(modifier = Modifier.clickable(onClick = onEliminar), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "Eliminar", tint = FrutAppColors.Error, modifier = Modifier.size(16.dp))
                Text("Eliminar", color = FrutAppColors.Error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}

@Composable
private fun FormularioDireccion(onGuardar: (String, String, String) -> Unit, onCancelar: () -> Unit) {
    var etiqueta by remember { mutableStateOf("") }
    var calle by remember { mutableStateOf("") }
    var comuna by remember { mutableStateOf("") }
    val valido = calle.isNotBlank() && comuna.isNotBlank()

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text("Nueva dirección", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        FrutTextField(value = etiqueta, onValueChange = { etiqueta = it }, label = "Etiqueta (Casa, Trabajo…)", leadingIcon = Icons.Filled.Place)
        Spacer(Modifier.height(10.dp))
        FrutTextField(value = calle, onValueChange = { calle = it }, label = "Calle y número")
        Spacer(Modifier.height(10.dp))
        FrutTextField(value = comuna, onValueChange = { comuna = it }, label = "Comuna")
        Spacer(Modifier.height(14.dp))
        FrutButtonPrimary(text = "Guardar dirección", onClick = { onGuardar(etiqueta, calle, comuna) }, enabled = valido)
        Text(
            "Cancelar",
            color = FrutAppColors.InkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onCancelar),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
