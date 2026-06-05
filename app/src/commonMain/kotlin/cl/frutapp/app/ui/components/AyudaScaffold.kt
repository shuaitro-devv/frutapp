package cl.frutapp.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.frutapp.app.ui.theme.FrutAppColors

data class AyudaPregunta(val q: String, val a: String)
data class AyudaSeccion(val titulo: String, val preguntas: List<AyudaPregunta>)
data class AyudaContacto(
    val icon: ImageVector,
    val titulo: String,
    val detalle: String,
    val onClick: () -> Unit
)

/**
 * Scaffold de pantalla de Ayuda: TopBar + Hero + FAQ por secciones + cards de contacto.
 * Reusado por la AyudaScreen del cliente y por la StaffAyudaScreen de picker/repartidor —
 * misma UI, distinto contenido. Antes el cliente tenia su propia implementacion de 312
 * lineas; mover una segunda copia para staff hubiera duplicado ~150 lineas de layout
 * y abierto deuda visual (cualquier cambio de marca habria que aplicarlo dos veces).
 */
@Composable
fun AyudaScaffold(
    heroTitulo: String,
    heroSubtitulo: String,
    secciones: List<AyudaSeccion>,
    contactos: List<AyudaContacto>,
    onBack: () -> Unit,
    contactosTitulo: String = "¿No encontraste lo que buscabas?",
    titulo: String = "Ayuda"
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(FrutAppColors.Brand50, CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = FrutAppColors.Ink, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = titulo,
                    color = FrutAppColors.Brand800,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                item { HeroAyuda(titulo = heroTitulo, subtitulo = heroSubtitulo) }

                secciones.forEach { seccion ->
                    item {
                        Text(
                            seccion.titulo,
                            color = FrutAppColors.Brand800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 22.dp, bottom = 6.dp)
                        )
                    }
                    items(seccion.preguntas.size) { idx ->
                        FaqRow(pregunta = seccion.preguntas[idx])
                    }
                }

                if (contactos.isNotEmpty()) {
                    item {
                        Text(
                            contactosTitulo,
                            color = FrutAppColors.Brand800,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }
                    items(contactos.size) { idx ->
                        ContactoCard(contactos[idx])
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroAyuda(titulo: String, subtitulo: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .background(FrutAppColors.Brand50, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(44.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(titulo, color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                subtitulo,
                color = FrutAppColors.InkMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun FaqRow(pregunta: AyudaPregunta) {
    var abierta by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, FrutAppColors.Brand50, RoundedCornerShape(14.dp))
            .clickable { abierta = !abierta }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                pregunta.q,
                color = FrutAppColors.Ink,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (abierta) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = FrutAppColors.Brand600,
                modifier = Modifier.size(22.dp)
            )
        }
        AnimatedVisibility(visible = abierta) {
            Text(
                pregunta.a,
                color = FrutAppColors.InkMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ContactoCard(contacto: AyudaContacto) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp)
            .background(FrutAppColors.Cream, RoundedCornerShape(14.dp))
            .clickable(onClick = contacto.onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(FrutAppColors.Brand400, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(contacto.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
            Text(contacto.titulo, color = FrutAppColors.Brand800, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(contacto.detalle, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
