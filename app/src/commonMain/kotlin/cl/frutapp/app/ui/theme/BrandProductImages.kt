@file:OptIn(ExperimentalResourceApi::class)

package cl.frutapp.app.ui.theme

import frutapp.app.generated.resources.Res
import frutapp.app.generated.resources.sofruco_agua_saborizadas_mandarina_12_unidades_copia
import frutapp.app.generated.resources.sofruco_agua_saborizadas_naranja_12_unidades
import frutapp.app.generated.resources.sofruco_agua_saborizadas_pomelo_12_unidades
import frutapp.app.generated.resources.sofruco_cajas_naranjas_15_kilos
import frutapp.app.generated.resources.sofruco_ciruela_deshidratada_300_gr
import frutapp.app.generated.resources.sofruco_cornellana_vs_carmenere_2023_magnum_1_botella_1
import frutapp.app.generated.resources.sofruco_jugo_100_natural_de_mandarina_1_litro_3_unidades
import frutapp.app.generated.resources.sofruco_jugo_100_natural_de_mandarina_formato_bag_in_box
import frutapp.app.generated.resources.sofruco_jugo_100_natural_de_mango_naranja_1_litro_3_unid
import frutapp.app.generated.resources.sofruco_jugo_100_natural_de_naranja_1_litro_3_unidades
import frutapp.app.generated.resources.sofruco_jugo_100_natural_de_pera_3_litros_formato_bag_in
import frutapp.app.generated.resources.sofruco_jugo_de_naranja_3_litros_formato_bag_in_box
import frutapp.app.generated.resources.sofruco_kiwi_verde_1_kilo
import frutapp.app.generated.resources.sofruco_la_palma_reserva_cabernet_sauvignon_6_unidades
import frutapp.app.generated.resources.sofruco_la_palma_reserva_carmenere_6_unidades
import frutapp.app.generated.resources.sofruco_la_palma_reserva_chardonnay_6_botellas_750ml_cop
import frutapp.app.generated.resources.sofruco_la_palma_reserva_chardonnay_6_unidades
import frutapp.app.generated.resources.sofruco_la_palma_reserva_merlot_6_unidades
import frutapp.app.generated.resources.sofruco_la_rosa_carmenere_2022_magnum_1_botella_1_5_lt_c
import frutapp.app.generated.resources.sofruco_limon_1_kilo
import frutapp.app.generated.resources.sofruco_limonada_receta_original_250_ml_12_unidades_copi
import frutapp.app.generated.resources.sofruco_mandarinas_1_5_kilos_copia
import frutapp.app.generated.resources.sofruco_nuevo_wellmix_energia_150_gr_1_unidad
import frutapp.app.generated.resources.sofruco_nuevo_wellmix_pepitas_de_zapallo_100gr_1_unidad
import frutapp.app.generated.resources.sofruco_nuevo_wellmix_proteina_150_gr_1_unidad
import frutapp.app.generated.resources.sofruco_nuevo_wellmix_saludable_100_gr_1_unidad
import frutapp.app.generated.resources.sofruco_peras_1_kilo
import frutapp.app.generated.resources.sofruco_uva_timco_700gr
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Mapeo `imageKey` (String) -> DrawableResource para los 28 assets Sofruco
 * scrapeados desde larosasofruco.cl. Compose-MP genera el binding de cada
 * archivo en composeResources/drawable como un property `Res.drawable.<file>`
 * a compile-time; aca lo resolvemos por nombre porque [BrandProduct.imageKey]
 * vive en el catalogo demo como String.
 *
 * Para refrescar: si re-scrapeás el sitio (puede haber productos nuevos o
 * renombrados), regenerá el snippet en _build/brand_catalog_sofruco.kt
 * y sumá los imports + when nuevos aca.
 */
fun brandProductDrawable(imageKey: String?): DrawableResource? = when (imageKey) {
    "sofruco_nuevo_wellmix_energia_150_gr_1_unidad" -> Res.drawable.sofruco_nuevo_wellmix_energia_150_gr_1_unidad
    "sofruco_nuevo_wellmix_proteina_150_gr_1_unidad" -> Res.drawable.sofruco_nuevo_wellmix_proteina_150_gr_1_unidad
    "sofruco_nuevo_wellmix_saludable_100_gr_1_unidad" -> Res.drawable.sofruco_nuevo_wellmix_saludable_100_gr_1_unidad
    "sofruco_nuevo_wellmix_pepitas_de_zapallo_100gr_1_unidad" -> Res.drawable.sofruco_nuevo_wellmix_pepitas_de_zapallo_100gr_1_unidad
    "sofruco_ciruela_deshidratada_300_gr" -> Res.drawable.sofruco_ciruela_deshidratada_300_gr
    "sofruco_cajas_naranjas_15_kilos" -> Res.drawable.sofruco_cajas_naranjas_15_kilos
    "sofruco_cornellana_vs_carmenere_2023_magnum_1_botella_1" -> Res.drawable.sofruco_cornellana_vs_carmenere_2023_magnum_1_botella_1
    "sofruco_la_rosa_carmenere_2022_magnum_1_botella_1_5_lt_c" -> Res.drawable.sofruco_la_rosa_carmenere_2022_magnum_1_botella_1_5_lt_c
    "sofruco_peras_1_kilo" -> Res.drawable.sofruco_peras_1_kilo
    "sofruco_limon_1_kilo" -> Res.drawable.sofruco_limon_1_kilo
    "sofruco_mandarinas_1_5_kilos_copia" -> Res.drawable.sofruco_mandarinas_1_5_kilos_copia
    "sofruco_uva_timco_700gr" -> Res.drawable.sofruco_uva_timco_700gr
    "sofruco_kiwi_verde_1_kilo" -> Res.drawable.sofruco_kiwi_verde_1_kilo
    "sofruco_jugo_100_natural_de_mandarina_formato_bag_in_box" -> Res.drawable.sofruco_jugo_100_natural_de_mandarina_formato_bag_in_box
    "sofruco_jugo_de_naranja_3_litros_formato_bag_in_box" -> Res.drawable.sofruco_jugo_de_naranja_3_litros_formato_bag_in_box
    "sofruco_jugo_100_natural_de_pera_3_litros_formato_bag_in" -> Res.drawable.sofruco_jugo_100_natural_de_pera_3_litros_formato_bag_in
    "sofruco_jugo_100_natural_de_mandarina_1_litro_3_unidades" -> Res.drawable.sofruco_jugo_100_natural_de_mandarina_1_litro_3_unidades
    "sofruco_jugo_100_natural_de_mango_naranja_1_litro_3_unid" -> Res.drawable.sofruco_jugo_100_natural_de_mango_naranja_1_litro_3_unid
    "sofruco_jugo_100_natural_de_naranja_1_litro_3_unidades" -> Res.drawable.sofruco_jugo_100_natural_de_naranja_1_litro_3_unidades
    "sofruco_agua_saborizadas_mandarina_12_unidades_copia" -> Res.drawable.sofruco_agua_saborizadas_mandarina_12_unidades_copia
    "sofruco_limonada_receta_original_250_ml_12_unidades_copi" -> Res.drawable.sofruco_limonada_receta_original_250_ml_12_unidades_copi
    "sofruco_agua_saborizadas_naranja_12_unidades" -> Res.drawable.sofruco_agua_saborizadas_naranja_12_unidades
    "sofruco_agua_saborizadas_pomelo_12_unidades" -> Res.drawable.sofruco_agua_saborizadas_pomelo_12_unidades
    "sofruco_la_palma_reserva_chardonnay_6_botellas_750ml_cop" -> Res.drawable.sofruco_la_palma_reserva_chardonnay_6_botellas_750ml_cop
    "sofruco_la_palma_reserva_cabernet_sauvignon_6_unidades" -> Res.drawable.sofruco_la_palma_reserva_cabernet_sauvignon_6_unidades
    "sofruco_la_palma_reserva_carmenere_6_unidades" -> Res.drawable.sofruco_la_palma_reserva_carmenere_6_unidades
    "sofruco_la_palma_reserva_chardonnay_6_unidades" -> Res.drawable.sofruco_la_palma_reserva_chardonnay_6_unidades
    "sofruco_la_palma_reserva_merlot_6_unidades" -> Res.drawable.sofruco_la_palma_reserva_merlot_6_unidades
    else -> null
}
