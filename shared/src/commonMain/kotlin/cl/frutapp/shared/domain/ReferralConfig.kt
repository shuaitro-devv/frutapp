package cl.frutapp.shared.domain

/**
 * Constantes del programa de referidos. Fuente unica de verdad para el
 * monto de los bonos: el service backend los usa al pagar y el DTO de
 * `/v1/referrals/verify` los expone a la landing para que el copy siempre
 * quede sincronizado. La app KMP los importa directamente.
 *
 * Si los cambias, el efecto es inmediato en backend + app (rebuild). La
 * landing los pilla via API (revalidate 5min).
 */
object ReferralConfig {
    /** FrutCoins que recibe el REFERIDOR cuando su referido completa la
     *  primera entrega. */
    const val BONO_REFERIDOR: Int = 200

    /** FrutCoins que recibe el REFERIDO al completar su primera entrega
     *  (bono de bienvenida por venir referido). */
    const val BONO_REFERIDO: Int = 100
}
