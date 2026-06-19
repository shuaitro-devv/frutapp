package cl.frutapp.backend.modules.catalog

import kotlin.test.Test
import kotlin.test.assertEquals

/** Unit test puro de [slugify] (sin BD ni Docker). */
class SlugifyTest {

    @Test
    fun `nombre con espacios`() {
        assertEquals("manzana-verde", slugify("Manzana Verde"))
    }

    @Test
    fun `quita acentos y enie`() {
        assertEquals("pina-golden", slugify("Piña Goldén"))
    }

    @Test
    fun `colapsa simbolos y recorta guiones de los bordes`() {
        assertEquals("aji-cia", slugify("  Ají & Cía!!  "))
    }

    @Test
    fun `solo simbolos o vacio cae a producto`() {
        assertEquals("producto", slugify("   "))
        assertEquals("producto", slugify("!!!"))
    }
}
