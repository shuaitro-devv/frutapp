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
    /** Nombre base (sin extension) del drawable en composeResources/drawable
     *  con la foto real del producto. Cuando es null el card cae al [emoji]
     *  como placeholder. Los productos Sofruco vienen del scrape de Shopify
     *  con este campo seteado a "sofruco_<handle>". */
    val imageKey: String? = null,
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

    /** Productos demo de Sofruco. Catalogo y precios scrapeados desde
     *  larosasofruco.cl (Shopify /products.json) y curados por categoria.
     *  Generados por _build/curate_sofruco_catalog.py. Si querés refrescar
     *  desde el sitio real, corré el script y pegá el snippet de
     *  _build/brand_catalog_sofruco.kt aca. */
    val sofrucoProducts: List<BrandProduct> = listOf(
        BrandProduct("nuevo-wellmix-energia-150-gr-1-unidad", "Wellmix Energía 100 gr (1 Unidad)", "secos", 2490, "unidad", "🥜", imageKey = "sofruco_nuevo_wellmix_energia_150_gr_1_unidad", badge = "Bestseller"),
        BrandProduct("nuevo-wellmix-proteina-150-gr-1-unidad", "Wellmix Proteína 100 gr (1 Unidad)", "secos", 2490, "unidad", "🥜", imageKey = "sofruco_nuevo_wellmix_proteina_150_gr_1_unidad"),
        BrandProduct("nuevo-wellmix-saludable-100-gr-1-unidad", "Wellmix Saludable 100 gr (1 Unidad)", "secos", 2490, "unidad", "🥜", imageKey = "sofruco_nuevo_wellmix_saludable_100_gr_1_unidad"),
        BrandProduct("nuevo-wellmix-pepitas-de-zapallo-100gr-1-unidad", "Wellmix Semillas de Zapallo 100gr (1 Unidad)", "secos", 2490, "unidad", "🥜", imageKey = "sofruco_nuevo_wellmix_pepitas_de_zapallo_100gr_1_unidad"),
        BrandProduct("ciruela-deshidratada-300-gr", "Ciruela Deshidratada 300 gr", "secos", 2660, "unidad", "🥜", imageKey = "sofruco_ciruela_deshidratada_300_gr"),
        BrandProduct("cajas-naranjas-15-kilos", "Caja Naranjas 15 Kilos", "cajas", 39900, "caja", "🎁", imageKey = "sofruco_cajas_naranjas_15_kilos", badge = "Bestseller"),
        BrandProduct("cornellana-vs-carmenere-magnum", "Cornellana VS Carmenere Magnum (caja madera)", "cajas", 45000, "caja", "🎁", imageKey = "sofruco_cornellana_vs_carmenere_2023_magnum_1_botella_1", badge = "Premium"),
        BrandProduct("la-rosa-cabernet-franc-magnum", "La Rosa Cabernet Franc Magnum (caja madera)", "cajas", 45000, "caja", "🎁", imageKey = "sofruco_la_rosa_carmenere_2022_magnum_1_botella_1_5_lt_c"),
        BrandProduct("la-rosa-carmenere-magnum", "La Rosa Carmenere Magnum (caja madera)", "cajas", 45000, "caja", "🎁", imageKey = "sofruco_la_rosa_carmenere_2022_magnum_1_botella_1_5_lt_c"),
        BrandProduct("peras-1-kilo", "Peras 1 Kilo", "fruta", 1790, "kg", "🍐", imageKey = "sofruco_peras_1_kilo", badge = "Bestseller"),
        BrandProduct("limon-1-kilo", "Limón 1 Kilo", "fruta", 2290, "kg", "🍋", imageKey = "sofruco_limon_1_kilo"),
        BrandProduct("mandarinas-1-5-kilos", "Mandarinas 1,5 Kilos", "fruta", 2611, "1,5 kg", "🍊", imageKey = "sofruco_mandarinas_1_5_kilos_copia"),
        BrandProduct("uva-roja-1-kilo", "Uva Roja 1 Kilo", "fruta", 2890, "kg", "🍇", imageKey = "sofruco_uva_timco_700gr"),
        BrandProduct("kiwi-1-kilo", "Kiwi 1 Kilo (bolsa)", "fruta", 2990, "kg", "🥝", imageKey = "sofruco_kiwi_verde_1_kilo"),
        BrandProduct("jugo-mandarina-3l", "Jugo 100% Natural de Mandarina 3 L (Bag in Box)", "jugos", 9000, "3 L", "🧃", imageKey = "sofruco_jugo_100_natural_de_mandarina_formato_bag_in_box", badge = "Bestseller"),
        BrandProduct("jugo-naranja-3l", "Jugo 100% Natural de Naranja 3 L (Bag in Box)", "jugos", 11250, "3 L", "🧃", imageKey = "sofruco_jugo_de_naranja_3_litros_formato_bag_in_box"),
        BrandProduct("jugo-pera-3l", "Jugo 100% Natural de Pera 3 L (Bag in Box)", "jugos", 11250, "3 L", "🧃", imageKey = "sofruco_jugo_100_natural_de_pera_3_litros_formato_bag_in"),
        BrandProduct("jugo-mandarina-1l-pack3", "Jugo 100% Natural de Mandarina 1 L (3 Unid)", "jugos", 12474, "3 x 1 L", "🧃", imageKey = "sofruco_jugo_100_natural_de_mandarina_1_litro_3_unidades"),
        BrandProduct("jugo-mango-naranja-1l-pack3", "Jugo 100% Natural de Mango Naranja 1 L (3 Unid)", "jugos", 12474, "3 x 1 L", "🧃", imageKey = "sofruco_jugo_100_natural_de_mango_naranja_1_litro_3_unid"),
        BrandProduct("jugo-naranja-1l-pack3", "Jugo 100% Natural de Naranja 1 L (3 Unid)", "jugos", 12474, "3 x 1 L", "🧃", imageKey = "sofruco_jugo_100_natural_de_naranja_1_litro_3_unidades"),
        BrandProduct("agua-mandarina-12u", "Agua Saborizada Mandarina 310 ml (12 unid)", "aguas", 18970, "12 x 310 ml", "💧", imageKey = "sofruco_agua_saborizadas_mandarina_12_unidades_copia", badge = "Bestseller"),
        BrandProduct("agua-limon-12u", "Agua Saborizada Limón 310 ml (12 unid)", "aguas", 22320, "12 x 310 ml", "💧", imageKey = "sofruco_limonada_receta_original_250_ml_12_unidades_copi"),
        BrandProduct("agua-naranja-12u", "Agua Saborizada Naranja 310 ml (12 unid)", "aguas", 22320, "12 x 310 ml", "💧", imageKey = "sofruco_agua_saborizadas_naranja_12_unidades"),
        BrandProduct("agua-pomelo-12u", "Agua Saborizada Pomelo 310 ml (12 unid)", "aguas", 22320, "12 x 310 ml", "💧", imageKey = "sofruco_agua_saborizadas_pomelo_12_unidades"),
        BrandProduct("la-palma-reserva-rose", "La Palma Reserva Rosé (6 botellas 750 ml)", "vinos", 26955, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_chardonnay_6_botellas_750ml_cop", badge = "Bestseller"),
        BrandProduct("la-palma-reserva-sauvignon-blanc", "La Palma Reserva Sauvignon Blanc (6 x 750 ml)", "vinos", 26955, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_chardonnay_6_botellas_750ml_cop"),
        BrandProduct("la-palma-reserva-cabernet", "La Palma Reserva Cabernet Sauvignon (6 x 750 ml)", "vinos", 28750, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_cabernet_sauvignon_6_unidades"),
        BrandProduct("la-palma-reserva-carmenere", "La Palma Reserva Carmenere (6 x 750 ml)", "vinos", 28750, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_carmenere_6_unidades"),
        BrandProduct("la-palma-reserva-chardonnay", "La Palma Reserva Chardonnay (6 x 750 ml)", "vinos", 28750, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_chardonnay_6_unidades"),
        BrandProduct("la-palma-reserva-merlot", "La Palma Reserva Merlot (6 x 750 ml)", "vinos", 28750, "6 x 750 ml", "🍷", imageKey = "sofruco_la_palma_reserva_merlot_6_unidades")
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
