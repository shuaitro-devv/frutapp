package cl.frutapp.app.ui.theme

/**
 * Catalogo demo por brand. Solo para la "edicion white-label" — las pantallas
 * de catalogo real siguen consultando al backend; este modulo existe para que el
 * APK Sofruco muestre productos plausibles SIN tocar el backend.
 *
 * Cuando exista el backend multi-tenant, este archivo se reemplaza por el endpoint
 * `/v1/catalog?brand=sofruco`. Por ahora, los lados de demo (landing/showcase)
 * leen este catalogo segun [ActiveBrand].
 */
data class BrandCategory(
    val id: String,
    val label: String,
    val emoji: String
)

data class BrandProduct(
    val id: String,
    val nombre: String,
    val categoriaId: String,
    val precioCLP: Int,
    val unidad: String,
    val emoji: String,
    /** Pista visual para chips/badges ("Bestseller", "Exclusivo Club", "Nuevo"). */
    val badge: String? = null
)

/** Por que estos productos? Sofruco vende fruta fresca, jugos 100% naturales,
 *  aguas saborizadas (alianza/distribucion), deshidratados (ciruelas), miel,
 *  frutos secos (Wellmix) y vinos premium (La Capitana, La Palma, OSSA).
 *  Catalogo "demo" no precios reales — son ilustrativos para mostrar al sponsor
 *  como queda su tienda en la app. */
object BrandCatalogs {
    val frutapp: List<BrandCategory> = listOf(
        BrandCategory("frutas", "Frutas de la feria", "🍎"),
        BrandCategory("verduras", "Verduras frescas", "🥕"),
        BrandCategory("orgánicos", "Orgánicos", "🌱"),
        BrandCategory("cajas", "Cajas curadas", "📦")
    )

    val sofruco: List<BrandCategory> = listOf(
        BrandCategory("jugos", "Jugos 100% naturales", "🧃"),
        BrandCategory("aguas", "Aguas saborizadas", "💧"),
        BrandCategory("fruta", "Fruta fresca de origen", "🍒"),
        BrandCategory("cajas", "Cajas y regalos", "🎁"),
        BrandCategory("secos", "Ciruelas, miel y frutos secos", "🍯"),
        BrandCategory("vinos", "Vinos La Rosa", "🍷")
    )

    /** Productos demo de Sofruco. Precios redondos a modo ilustrativo. */
    val sofrucoProducts: List<BrandProduct> = listOf(
        BrandProduct("jugo-naranja-1l", "Jugo de naranja 1 L", "jugos", 2490, "1 L", "🧃", badge = "Bestseller"),
        BrandProduct("jugo-manzana-1l", "Jugo de manzana 1 L", "jugos", 2290, "1 L", "🍏"),
        BrandProduct("jugo-arandano-1l", "Jugo de arándano 1 L", "jugos", 3490, "1 L", "🫐"),
        BrandProduct("agua-citrus-500", "Agua saborizada cítrica 500 ml", "aguas", 990, "500 ml", "🍋"),
        BrandProduct("agua-frutilla-500", "Agua saborizada frutilla 500 ml", "aguas", 990, "500 ml", "🍓"),
        BrandProduct("manzana-1kg", "Manzana fuji 1 kg", "fruta", 1990, "kg", "🍎", badge = "De origen"),
        BrandProduct("cereza-500g", "Cereza de exportación 500 g", "fruta", 5990, "500 g", "🍒", badge = "Temporada"),
        BrandProduct("naranja-2kg", "Naranja de jugo 2 kg", "fruta", 3490, "2 kg", "🍊"),
        BrandProduct("caja-frutos-secos", "Caja regalo frutos secos Wellmix", "cajas", 12990, "caja", "🎁", badge = "Exclusivo Club"),
        BrandProduct("caja-vinos-degustacion", "Caja degustación La Rosa (3 vinos)", "cajas", 39990, "caja", "🎁"),
        BrandProduct("ciruela-deshidratada", "Ciruela deshidratada premium 500 g", "secos", 4990, "500 g", "🍐"),
        BrandProduct("miel-multifloral", "Miel multifloral 500 g", "secos", 5990, "500 g", "🍯"),
        BrandProduct("wellmix-mix-300", "Wellmix mix nuts 300 g", "secos", 7990, "300 g", "🥜"),
        BrandProduct("la-capitana-cab", "La Capitana Cabernet Sauvignon", "vinos", 8990, "750 ml", "🍷"),
        BrandProduct("la-palma-syrah", "La Palma Syrah", "vinos", 5990, "750 ml", "🍷"),
        BrandProduct("ossa-icon", "OSSA Icon - edición 200 años", "vinos", 49990, "750 ml", "🍷", badge = "Premium")
    )

    fun categoriesFor(brand: Brand): List<BrandCategory> = when (brand.id) {
        SofrucoBrand.id -> sofruco
        else -> frutapp
    }

    fun productsFor(brand: Brand): List<BrandProduct> = when (brand.id) {
        SofrucoBrand.id -> sofrucoProducts
        else -> emptyList()   // FrutApp usa el catalogo real del backend
    }
}
