package cl.frutapp.app.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import cl.frutapp.app.data.SessionStorage

/**
 * Brand white-label: paleta, nombre, slogan, copy de los coins y catalogo demo.
 *
 * El motor de la app es el mismo para todas las marcas (FrutApp, Sofruco, futuros
 * partners). Cada Brand cambia colores, texto y catalogo de demo, NO la logica
 * de pedidos/picker/repartidor.
 *
 * Activacion: [ActiveBrand.current] se setea en el arranque (MainActivity) leyendo
 * BuildConfig.BRAND_ID del flavor. El theme y [FrutAppColors] delegan a ese Brand,
 * asi cualquier pantalla ya escrita con `FrutAppColors.Brand400` cambia de paleta
 * sin tocar codigo.
 *
 * Para preview/tests, [LocalBrand] se puede sobreescribir con `CompositionLocalProvider`.
 */
interface Brand {
    /** Identificador estable. Usado por flavors, analytics y catalogo. */
    val id: String
    /** Nombre visible (header, splash, copy). */
    val displayName: String
    /** Slogan/tagline corto para hero/landing. */
    val slogan: String
    /** Como se llaman los coins/puntos para este brand. Ej: "FrutCoins", "Club Sofruco". */
    val coinsName: String
    /** Singular/plural cortos del coin para textos como "+50 FrutCoin". */
    val coinUnit: String
    val palette: BrandPalette
}

/** Tokens de color. Mismos nombres que `FrutAppColors` para que el resto del codigo
 *  no cambie cuando se conecta el Brand activo. */
data class BrandPalette(
    val brand50: Color,
    val brand100: Color,
    val brand200: Color,
    val brand400: Color,
    val brand600: Color,
    val brand800: Color,
    val amberCoin: Color,
    val amberSoft: Color,
    val cream: Color,
    val background: Color,
    val ink: Color,
    val inkMuted: Color,
    val inkSoft: Color,
    val error: Color,
    val warning: Color
)

/** FrutApp - paleta verde original (memoria `project-frutapp-brand`). */
object FrutAppBrand : Brand {
    override val id: String = "frutapp"
    override val displayName: String = "FrutApp"
    override val slogan: String = "De la cosecha a tu mesa"
    override val coinsName: String = "FrutCoins"
    override val coinUnit: String = "FrutCoin"
    override val palette: BrandPalette = BrandPalette(
        brand50 = Color(0xFFEAF3DE),
        brand100 = Color(0xFFDCE9C8),
        brand200 = Color(0xFF97C459),
        brand400 = Color(0xFF639922),
        brand600 = Color(0xFF3B6D11),
        brand800 = Color(0xFF27500A),
        amberCoin = Color(0xFFBA7517),
        amberSoft = Color(0xFFFAEEDA),
        cream = Color(0xFFF1EFE8),
        background = Color(0xFFFAF8F1),
        ink = Color(0xFF1F2A14),
        inkMuted = Color(0xFF5C6B4A),
        inkSoft = Color(0xFF8A9377),
        error = Color(0xFFE24B4A),
        warning = Color(0xFFE2A04A)
    )
}

/** Sofruco - edicion white-label para sponsor La Rosa Sofruco. Paleta naranjo-verde
 *  alineada al branding del holding (verde campo + naranjo Club + burdeo viña). */
object SofrucoBrand : Brand {
    override val id: String = "sofruco"
    override val displayName: String = "Sofruco"
    override val slogan: String = "Del campo a tu mesa, en horas"
    override val coinsName: String = "Club Sofruco"
    override val coinUnit: String = "punto Club"
    override val palette: BrandPalette = BrandPalette(
        // Verde Sofruco como brand principal
        brand50 = Color(0xFFE9F1E2),
        brand100 = Color(0xFFCFE0BD),
        brand200 = Color(0xFF9DC6A9),    // salvia
        brand400 = Color(0xFF5F9A3B),    // verde primary
        brand600 = Color(0xFF477529),
        brand800 = Color(0xFF2F5A1C),    // oscuro
        // Naranjo Club Sofruco como acento ("coins")
        amberCoin = Color(0xFFEE7D1B),
        amberSoft = Color(0xFFFCE6CF),
        // Cremas y fondos
        cream = Color(0xFFF4EFE8),
        background = Color(0xFFFBF7F0),
        // Texto: tinte mas calido por el naranjo
        ink = Color(0xFF231A12),
        inkMuted = Color(0xFF6E2A38),    // burdeo de la viña como muted decorativo
        inkSoft = Color(0xFF8A7766),
        error = Color(0xFFC4423E),
        warning = Color(0xFFE2A04A)
    )
}

/** Brand activo en runtime. Es un [mutableStateOf] para que cualquier Composable
 *  que lo lea (FrutAppTheme, LocalBrand, etc.) se recomponga automaticamente cuando
 *  se cambia con [setAndPersist].
 *
 *  Estrategia de carga al arranque (MainActivity):
 *  1. Si el usuario eligio un brand desde la app (override persistido), se usa ese.
 *  2. Si no, se usa el BRAND_ID que el flavor dejo en BuildConfig.
 *  3. Si nada de eso aplica (JVM/iOS sin flavor), queda FrutApp por default.
 *
 *  El override persistido es lo que permite el toggle "Modo FrutApp / Modo Sofruco"
 *  desde Perfil sin reinstalar el APK — util para demos al sponsor.
 */
object ActiveBrand {
    private const val KEY_OVERRIDE = "active_brand_override"

    private var _current by mutableStateOf<Brand>(FrutAppBrand)
    val current: Brand get() = _current

    /** Setea el brand SIN persistir. Usado por MainActivity al arrancar para
     *  reflejar el BRAND_ID del flavor cuando no hay override del usuario. */
    fun set(id: String) {
        _current = byId(id)
    }

    /** Setea el brand Y lo persiste como override del usuario. El proximo arranque
     *  va a respetar esta eleccion por sobre el flavor. Disparado desde el
     *  selector de la pantalla de Perfil. */
    fun setAndPersist(id: String) {
        _current = byId(id)
        SessionStorage.putString(KEY_OVERRIDE, id)
    }

    /** Devuelve el override persistido si existe, o null. MainActivity lo consulta
     *  para decidir si respeta el flavor o la eleccion previa del usuario. */
    fun persistedOverride(): String? = SessionStorage.getString(KEY_OVERRIDE)

    /** Limpia el override (vuelve a respetar el flavor en el proximo arranque). */
    fun clearOverride() {
        SessionStorage.remove(KEY_OVERRIDE)
    }

    /** Catalogo de brands conocidos. Para agregar uno nuevo, sumalo aca y a [byId]. */
    val all: List<Brand> = listOf(FrutAppBrand, SofrucoBrand)

    private fun byId(id: String): Brand = when (id.lowercase()) {
        SofrucoBrand.id -> SofrucoBrand
        else -> FrutAppBrand
    }
}
