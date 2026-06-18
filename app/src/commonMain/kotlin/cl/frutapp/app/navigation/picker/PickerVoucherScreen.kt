package cl.frutapp.app.navigation.picker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cl.frutapp.app.ui.components.FrutButtonOutline
import cl.frutapp.app.ui.components.FrutButtonPrimary
import cl.frutapp.app.ui.showToast
import cl.frutapp.app.ui.theme.FrutAppColors
import kotlin.math.abs

/**
 * Comprobante imprimible del pedido. Lo que el picker pega en la caja antes del handoff
 * a despacho: identifica el pedido inequivocamente (ID legible + QR), resume el contenido
 * y el destino. El repartidor lo usa como contraparte fisica al confirmar la entrega.
 *
 * El QR es un placeholder visual generado deterministicamente a partir del ID. Cuando se
 * integre una libreria de QR (qrcode-kotlin u otra) o el backend genere la imagen, este
 * VoucherQR se reemplaza por el bitmap real.
 */
class PickerVoucherScreen(
    private val pedidoId: String,
    private val estados: Map<Int, EstadoItem>
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val data = remember(pedidoId) { picklistMock(pedidoId) }
        // Fix #1: estados vacios viniendo del historial → sintetizamos COMPLETADO para
        // todos los items, asi el voucher no se imprime con '0 de 12 Completos'.
        val estadosEfectivos = remember(estados, pedidoId) {
            if (estados.isEmpty()) data.items.associate { it.numero to EstadoItem.COMPLETADO }
            else estados
        }
        val completos = estadosEfectivos.values.count { it == EstadoItem.COMPLETADO }
        val sustituidos = estadosEfectivos.values.count { it == EstadoItem.SUSTITUIDO }
        val reducidos = estadosEfectivos.values.count { it == EstadoItem.REDUCIDO }
        val faltantes = estadosEfectivos.values.count { it == EstadoItem.FALTANTE }
        val incidencias = sustituidos + reducidos + faltantes

        Column(modifier = Modifier.fillMaxSize().background(FrutAppColors.Background).statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = FrutAppColors.Brand800)
                }
                Text("Voucher del pedido", color = FrutAppColors.Brand800, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            }
            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp)) {
                VoucherCard(
                    pedidoId = pedidoId,
                    destino = data.destino,
                    sector = data.sector,
                    items = data.totalItems,
                    completos = completos,
                    incidencias = incidencias
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Pega este voucher a la caja antes del handoff a despacho.",
                    color = FrutAppColors.InkSoft,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color.White).navigationBarsPadding().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FrutButtonOutline(
                    text = "Compartir",
                    onClick = { showToast("Compartir - Próximamente") },
                    modifier = Modifier.weight(1f)
                )
                FrutButtonPrimary(
                    text = "Imprimir",
                    onClick = { showToast("Impresora térmica - Próximamente") },
                    modifier = Modifier.weight(1.2f)
                )
            }
        }
    }
}

@Composable
private fun VoucherCard(
    pedidoId: String,
    destino: String,
    sector: String,
    items: Int,
    completos: Int,
    incidencias: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(2.dp, FrutAppColors.Brand400, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        // Cabecera tipo recibo
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(FrutAppColors.Brand600, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) { Text("F", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("FrutApp", color = FrutAppColors.Brand800, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("De la cosecha a tu mesa", color = FrutAppColors.InkSoft, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(14.dp))
        DashedDivider()
        Spacer(Modifier.height(14.dp))
        Text("Voucher del pedido", color = FrutAppColors.InkMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(pedidoId, color = FrutAppColors.Brand800, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        // QR placeholder
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            VoucherQR(seed = pedidoId)
        }
        Spacer(Modifier.height(14.dp))
        DashedDivider()
        Spacer(Modifier.height(14.dp))
        Linea(label = "Destino", valor = destino)
        Linea(label = "Sector", valor = sector)
        Linea(label = "Items", valor = "$items productos")
        Linea(label = "Completos", valor = "$completos de $items")
        if (incidencias > 0) {
            Linea(label = "Incidencias", valor = "$incidencias", color = EstadoPaleta.faltante)
        }
        Spacer(Modifier.height(14.dp))
        DashedDivider()
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Picker: Camila R. · 10:42",
            color = FrutAppColors.InkMuted,
            fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Linea(label: String, valor: String, color: Color = FrutAppColors.Brand800) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, color = FrutAppColors.InkMuted, fontSize = 12.sp, modifier = Modifier.width(90.dp))
        Text(valor, color = color, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DashedDivider() {
    Canvas(modifier = Modifier.fillMaxWidth().height(1.dp)) {
        val dashWidth = 6.dp.toPx()
        val gap = 4.dp.toPx()
        var x = 0f
        while (x < size.width) {
            drawRect(
                color = FrutAppColors.Brand100,
                topLeft = androidx.compose.ui.geometry.Offset(x, 0f),
                size = androidx.compose.ui.geometry.Size(dashWidth, size.height)
            )
            x += dashWidth + gap
        }
    }
}

/**
 * QR placeholder visual: una grilla 21x21 generada deterministicamente del [seed] (el ID
 * del pedido). NO es un QR real — sirve solo para el visual mientras no integremos una
 * libreria de generacion (qrcode-kotlin) o el backend devuelva la imagen.
 */
@Composable
private fun VoucherQR(seed: String) {
    val GRID = 21
    val cells = remember(seed) {
        // PRNG simple deterministico desde el seed: cada celda se enciende segun el hash
        // rotado por su posicion. Bordes con 'finder pattern' (esquinas grandes) para que
        // visualmente parezca un QR real.
        val base = seed.fold(0L) { acc, c -> acc * 31 + c.code }
        Array(GRID) { y ->
            BooleanArray(GRID) { x ->
                val finder = isFinderPattern(x, y, GRID)
                if (finder != null) finder
                else {
                    val h = (base xor (x * 73L + y * 53L)).hashCode()
                    abs(h % 100) < 48
                }
            }
        }
    }
    Box(
        modifier = Modifier
            .size(180.dp)
            .background(Color.White, RoundedCornerShape(8.dp))
            .border(1.dp, FrutAppColors.Brand100, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cellSize = size.width / GRID
            for (y in 0 until GRID) {
                for (x in 0 until GRID) {
                    if (cells[y][x]) {
                        drawRect(
                            color = FrutAppColors.Brand800,
                            topLeft = androidx.compose.ui.geometry.Offset(x * cellSize, y * cellSize),
                            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                        )
                    }
                }
            }
        }
    }
}

/** Finder pattern de un QR (esquinas 7x7 con anillo). null = celda fuera del patron. */
private fun isFinderPattern(x: Int, y: Int, grid: Int): Boolean? {
    val esquinas = listOf(0 to 0, grid - 7 to 0, 0 to grid - 7)
    for ((ox, oy) in esquinas) {
        if (x in ox until ox + 7 && y in oy until oy + 7) {
            val lx = x - ox; val ly = y - oy
            val borde = lx == 0 || lx == 6 || ly == 0 || ly == 6
            val centro = lx in 2..4 && ly in 2..4
            return borde || centro
        }
    }
    return null
}
