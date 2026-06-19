package cl.frutapp.backend.modules.admin

import cl.frutapp.backend.error.ValidationException
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Unit test puro de [adminOrdersRange] (sin reloj ni BD). */
class AdminOrderRangeTest {

    private val tz = TimeZone.of("America/Santiago")

    @Test
    fun `sin fecha usa ventana reciente de 14 dias, half-open`() {
        val hoy = LocalDate(2026, 6, 19)
        val (start, end, fecha) = adminOrdersRange(null, hoy, tz)
        assertEquals(LocalDate(2026, 6, 5).atStartOfDayIn(tz), start) // hoy - 14
        assertEquals(LocalDate(2026, 6, 20).atStartOfDayIn(tz), end) // hoy + 1 (excluido)
        assertEquals("2026-06-19", fecha)
    }

    @Test
    fun `con fecha usa ese dia puntual, half-open`() {
        val (start, end, fecha) = adminOrdersRange("2026-06-10", LocalDate(2026, 6, 19), tz)
        assertEquals(LocalDate(2026, 6, 10).atStartOfDayIn(tz), start)
        assertEquals(LocalDate(2026, 6, 11).atStartOfDayIn(tz), end)
        assertEquals("2026-06-10", fecha)
    }

    @Test
    fun `fecha invalida lanza ValidationException`() {
        assertFailsWith<ValidationException> {
            adminOrdersRange("ayer", LocalDate(2026, 6, 19), tz)
        }
    }
}
