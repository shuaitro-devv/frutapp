"""Curacion del scraping: re-clasifica aguas (que mi heuristica metio en jugos),
selecciona N productos por categoria balanceando precio y formato, y genera
un snippet Kotlin con la lista lista para pegar en BrandCatalog.kt.

Reglas de curacion para que el demo se vea ordenado:
- Aguas: cualquier titulo con "agua saborizad" o "limonada".
- Jugos: 1L y 5L (no 60-unidades-mayorista).
- Fruta: kilos chicos (1-3 kg), no cajas 15kg (esas van a cajas).
- Cajas: cualquier "caja" o "pack" o "vertical".
- Vinos: botellas sueltas o packs de 6.
- Secos: Wellmix mas representativos.

Output: _build/brand_catalog_sofruco.kt (snippet listo para reemplazar la
lista actual). El script imprime tambien stats por categoria.
"""
import json
import re
from collections import defaultdict
from pathlib import Path

DATA = Path("c:/others/own/frutapp/_build/catalogo_sofruco_scraped.json")
OUT = Path("c:/others/own/frutapp/_build/brand_catalog_sofruco.kt")

EMOJI = {
    "jugos": "🧃",
    "aguas": "💧",
    "fruta": "🍒",
    "cajas": "🎁",
    "secos": "🥜",
    "vinos": "🍷",
}


def reclassify(p: dict) -> str:
    title = p["title"].lower()
    cat = p["category_demo_id"]
    if "agua saborizad" in title or "limonada" in title:
        return "aguas"
    if cat == "fruta" and ("caja" in title or "15 kilos" in title):
        return "cajas"
    return cat


def kotlin_escape(s: str) -> str:
    return s.replace("\\", "\\\\").replace("\"", "\\\"")


def main():
    data = json.loads(DATA.read_text(encoding="utf-8"))
    for p in data:
        p["cat"] = reclassify(p)

    by_cat = defaultdict(list)
    for p in data:
        by_cat[p["cat"]].append(p)

    # Curacion: filtrar packs gigantes (>= 36 unidades o titulos con "60 unid", "MAYORISTA")
    def pasa_filtro(p):
        t = p["title"].lower()
        if any(k in t for k in ["60 unid", "144", "240 unid", "mayorista", "promocion oferta", "tu pack"]):
            return False
        return True

    # Tope por categoria
    LIMITES = {"jugos": 6, "aguas": 4, "fruta": 5, "cajas": 4, "secos": 5, "vinos": 6}
    seleccion = []
    for cat, items in by_cat.items():
        candidatos = [p for p in items if pasa_filtro(p)]
        candidatos.sort(key=lambda p: (p.get("price_clp") or 999_999, p["title"]))
        seleccion.extend(candidatos[: LIMITES.get(cat, 4)])

    # Banderas demo: badges para los 3 mas caros (Premium) y los 3 mas baratos por categoria (Bestseller)
    badges = {}
    by_cat_sel = defaultdict(list)
    for p in seleccion:
        by_cat_sel[p["cat"]].append(p)
    for cat, items in by_cat_sel.items():
        if not items:
            continue
        items_sorted = sorted(items, key=lambda p: -(p.get("price_clp") or 0))
        if items_sorted and items_sorted[0].get("price_clp", 0) >= 30000:
            badges[items_sorted[0]["id"]] = "Premium"
        items_sorted_asc = sorted(items, key=lambda p: (p.get("price_clp") or 999_999))
        if items_sorted_asc:
            badges[items_sorted_asc[0]["id"]] = "Bestseller"

    # Render Kotlin
    lines = []
    lines.append("    val sofrucoProducts: List<BrandProduct> = listOf(")
    for p in seleccion:
        title = kotlin_escape(p["title"]).replace("\n", " ").strip()
        precio = p.get("price_clp") or 0
        cat = p["cat"]
        emoji = EMOJI.get(cat, "🍎")
        badge = badges.get(p["id"])
        badge_kt = f", badge = \"{badge}\"" if badge else ""
        # asset_name (sin extension) es el imageKey
        asset = p["asset_name"]
        lines.append(
            f"        BrandProduct(\"{p['id']}\", \"{title}\", \"{cat}\", "
            f"{precio}, \"unidad\", \"{emoji}\", imageKey = \"{asset}\"{badge_kt}),"
        )
    # Quita la coma del ultimo
    lines[-1] = lines[-1].rstrip(",")
    lines.append("    )")

    OUT.write_text("\n".join(lines), encoding="utf-8")
    print(f"Snippet generado en {OUT}")
    print(f"Productos seleccionados: {len(seleccion)}")
    for cat, items in by_cat_sel.items():
        print(f"  {cat}: {len(items)}")


if __name__ == "__main__":
    main()
