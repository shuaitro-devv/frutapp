package cl.frutapp.app.data

import androidx.compose.runtime.mutableStateListOf

/** Tipo de material reciclable. Las coins varían según costo de tratamiento y demanda. */
data class TipoReciclaje(
    val codigo: String,
    val nombre: String,
    val emoji: String,
    /** "+8 coins/kg" o "+30 coins/u" — para mostrar en la card. */
    val coinsLabel: String
)

/** Modo en el que FrutApp retira el material reciclable. */
data class ModoRetiro(
    val codigo: String,
    val nombre: String,
    val detalle: String,
    val costo: String, // "$2.990 o canjeable con FrutCoins", "Gratis", "Gratis · con tu pedido"
    val emoji: String
)

/** Registro de un retiro pasado (dummy para el historial). */
data class RetiroHistorial(
    val tipo: String,
    val cantidad: String,
    val coinsGanadas: Int,
    val fechaRelativa: String
)

/**
 * Estado dummy del módulo de reciclaje:
 * - tipos de material aceptados
 * - modos de retiro disponibles
 * - historial de retiros del usuario
 * - lead capture para usuarios fuera de las comunas piloto
 */
object ReciclaStore {

    /** Comunas en las que opera el piloto actual. */
    val comunasPiloto = listOf("Ñuñoa", "Providencia", "Las Condes", "La Reina")

    val tipos = listOf(
        TipoReciclaje("PLASTICO", "Plástico", "🥤", "+8 coins/kg"),
        TipoReciclaje("PAPEL", "Papel/Cartón", "📦", "+5 coins/kg"),
        TipoReciclaje("VIDRIO", "Vidrio", "🍾", "+10 coins/kg"),
        TipoReciclaje("ORGANICO", "Orgánico", "🍃", "+6 coins/kg"),
        TipoReciclaje("PILAS", "Pilas", "🔋", "+30 coins/u")
    )

    val modos = listOf(
        ModoRetiro(
            codigo = "PUERTA",
            nombre = "Retiro en tu puerta",
            detalle = "Pasamos a buscarlo en horario coordinado",
            costo = "$2.990 o canjea con FrutCoins",
            emoji = "🚪"
        ),
        ModoRetiro(
            codigo = "PUNTO",
            nombre = "Punto de acopio",
            detalle = "Te decimos el más cercano",
            costo = "Gratis",
            emoji = "📍"
        ),
        ModoRetiro(
            codigo = "PEDIDO",
            nombre = "Con tu próximo pedido",
            detalle = "Cuando entreguemos lo tuyo, retiramos lo reciclable",
            costo = "Gratis",
            emoji = "📦"
        )
    )

    /** Historial dummy del usuario. */
    val historial = mutableStateListOf(
        RetiroHistorial("Cartón", "1,2 kg", 6, "hace 3 días"),
        RetiroHistorial("Pilas", "4 u", 120, "hace 1 semana"),
        RetiroHistorial("Vidrio", "800 g", 8, "hace 2 semanas"),
        RetiroHistorial("Plástico", "2,5 kg", 20, "hace 1 mes")
    )

    /** Total de cajas FrutApp pendientes de devolver (dummy: viene de pedidos recientes). */
    val cajasFrutAppPendientes = 1

    /**
     * Aliados certificados de referencia. Cuando arme el piloto real va a depender del
     * acuerdo comercial — esto es para mostrar el ecosistema en la demo.
     */
    val aliadosReferencia = listOf("TriCiclos", "Recicla Tus Pilas", "Reciclaje Limpio", "EcoCentros municipales")
}
