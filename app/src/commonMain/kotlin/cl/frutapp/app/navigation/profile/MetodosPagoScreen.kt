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
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.data.MetodoPago
import cl.frutapp.app.data.MetodosPagoStore
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.components.FrutTextField
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors

/** Métodos de pago del usuario (mini-flujo dummy): listar, predeterminar, eliminar y agregar. */
class MetodosPagoScreen : Screen {
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
                    Text("Métodos de pago", color = FrutAppColors.Brand800, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                }

                // navigationBarsPadding: respetar barra de nav del sistema (3-btn nav) para que el CTA no quede tapado.
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
                    MetodosPagoStore.items.forEach { metodo ->
                        MetodoCard(
                            metodo = metodo,
                            esPredeterminado = metodo.id == MetodosPagoStore.predeterminadoId,
                            onPredeterminar = {
                                MetodosPagoStore.setPredeterminado(metodo.id)
                                showToast("Método predeterminado actualizado")
                            },
                            onEliminar = {
                                MetodosPagoStore.remove(metodo.id)
                                showToast("Método eliminado")
                            }
                        )
                    }

                    if (agregando) {
                        FormularioMetodo(
                            onGuardar = { tipo, ultimos4, vence ->
                                MetodosPagoStore.add(tipo, ultimos4, vence)
                                agregando = false
                                showToast("Método de pago agregado")
                            },
                            onCancelar = { agregando = false }
                        )
                    } else {
                        Box(modifier = Modifier.padding(top = 16.dp)) {
                            FrutButtonPrimary(text = "Agregar método de pago", onClick = { agregando = true }, leadingIcon = Icons.Filled.Add)
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
private fun MetodoCard(
    metodo: MetodoPago,
    esPredeterminado: Boolean,
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
                Icon(Icons.Filled.CreditCard, contentDescription = null, tint = FrutAppColors.Brand600, modifier = Modifier.size(22.dp))
            }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${metodo.tipo} •••• ${metodo.ultimos4}", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (esPredeterminado) {
                        Box(modifier = Modifier.padding(start = 8.dp).background(FrutAppColors.Brand400, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("Predeterminado", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text("Vence ${metodo.vence}", color = FrutAppColors.InkSoft, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!esPredeterminado) {
                Text("Usar como predeterminado", color = FrutAppColors.Brand600, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.clickable(onClick = onPredeterminar))
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
private fun FormularioMetodo(onGuardar: (String, String, String) -> Unit, onCancelar: () -> Unit) {
    var tipo by remember { mutableStateOf("Visa") }
    var numero by remember { mutableStateOf("") }
    var vence by remember { mutableStateOf("") }
    val digitos = numero.filter { it.isDigit() }
    val valido = digitos.length >= 4 && vence.isNotBlank()

    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Text("Nueva tarjeta", color = FrutAppColors.Brand800, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("Visa", "Mastercard").forEach { t ->
                TipoChip(label = t, selected = tipo == t, onClick = { tipo = t })
            }
        }
        Spacer(Modifier.height(12.dp))
        FrutTextField(value = numero, onValueChange = { numero = it }, label = "Número de tarjeta", leadingIcon = Icons.Filled.CreditCard, keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(10.dp))
        FrutTextField(value = vence, onValueChange = { vence = it }, label = "Vencimiento (MM/AA)", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(14.dp))
        FrutButtonPrimary(text = "Guardar tarjeta", onClick = { onGuardar(tipo, digitos.takeLast(4), vence) }, enabled = valido)
        Text(
            "Cancelar",
            color = FrutAppColors.InkSoft,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable(onClick = onCancelar),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TipoChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(if (selected) FrutAppColors.Brand400 else Color.White, RoundedCornerShape(10.dp))
            .border(1.dp, if (selected) FrutAppColors.Brand400 else FrutAppColors.Brand100, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(label, color = if (selected) Color.White else FrutAppColors.Brand800, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
