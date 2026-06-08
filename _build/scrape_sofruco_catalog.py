"""Scrape del catalogo de larosasofruco.cl via Shopify products.json.

Shopify expone /products.json publicamente — devuelve JSON con productos
(nombre, precio, imagenes, variantes). No necesita login ni token.

Por producto:
- Baja la primera imagen al maximo tamaño disponible (Shopify maneja
  parametros &width=N en la URL; sacando el sufijo _NNNx queda el original).
- Guarda con nombre canonico kebab-case basado en el handle del producto.
- Categoriza por product_type para mapear a las categorias de BrandCatalogs.

Output:
- Imagenes en app/src/commonMain/composeResources/drawable/sofruco_*.png
- catalogo_sofruco_scraped.json con metadata para que el desarrollador
  copie/pegue en BrandCatalog.kt (no se autogenera Kotlin para no
  pisar cambios manuales del catalogo demo).
"""
import json
import re
import sys
import time
from pathlib import Path
from urllib.parse import urlsplit
from urllib.request import Request, urlopen

BASE = "https://larosasofruco.cl"
DRAWABLE_DIR = Path("c:/others/own/frutapp/app/src/commonMain/composeResources/drawable")
META_OUT = Path("c:/others/own/frutapp/_build/catalogo_sofruco_scraped.json")
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 FrutApp-Demo-Scraper/1.0"


def fetch(url: str, binary: bool = False) -> bytes | str:
    req = Request(url, headers={"User-Agent": UA, "Accept": "*/*"})
    with urlopen(req, timeout=30) as r:
        data = r.read()
    return data if binary else data.decode("utf-8", errors="replace")


def kebab(s: str) -> str:
    s = re.sub(r"[^a-z0-9]+", "_", s.lower()).strip("_")
    return re.sub(r"_+", "_", s)


def page_products(page: int) -> list[dict]:
    """Shopify pagina /products.json con 30 productos por pagina."""
    body = fetch(f"{BASE}/products.json?limit=250&page={page}")
    return json.loads(body).get("products", [])


def best_image_url(p: dict) -> str | None:
    # Toma el primer "src" disponible. Quitamos cualquier sufijo de tamaño
    # (_180x, _600x.progressive) para pedir el original.
    images = p.get("images") or []
    if not images:
        return None
    src = images[0].get("src", "")
    if not src:
        return None
    src = re.sub(r"_(\d+x\d*)(?=\.)", "", src)
    return src


def category_from_type(product_type: str | None, title: str) -> str:
    t = (product_type or "").lower()
    name = (title or "").lower()
    if "jugo" in t or "jugo" in name:
        return "jugos"
    if "agua" in t or "agua" in name or "saborizad" in name:
        return "aguas"
    if "fruta" in t or "fruta" in name:
        return "fruta"
    if "caja" in t or "caja" in name or "regalo" in name:
        return "cajas"
    if "vino" in t or "vino" in name or "viña" in name:
        return "vinos"
    if any(k in name for k in ["ciruela", "miel", "frut", "seco", "wellmix", "almendra", "nuez"]):
        return "secos"
    return "fruta"  # fallback razonable para sofruco


def main() -> int:
    DRAWABLE_DIR.mkdir(parents=True, exist_ok=True)
    all_products: list[dict] = []
    page = 1
    while True:
        batch = page_products(page)
        if not batch:
            break
        all_products.extend(batch)
        print(f"page {page}: {len(batch)} productos (total {len(all_products)})")
        if len(batch) < 250:
            break
        page += 1
        time.sleep(0.4)

    print(f"\nTotal productos vistos: {len(all_products)}")
    print(f"Descargando imagenes a {DRAWABLE_DIR}")

    out: list[dict] = []
    descargadas = 0
    for p in all_products:
        title = p.get("title", "").strip()
        handle = p.get("handle", "")
        ptype = p.get("product_type", "")
        variants = p.get("variants") or []
        price = None
        if variants:
            try:
                price = int(round(float(variants[0].get("price", "0"))))
            except (TypeError, ValueError):
                price = None
        img_url = best_image_url(p)
        if not img_url:
            continue
        if img_url.startswith("//"):
            img_url = "https:" + img_url

        asset_name = f"sofruco_{kebab(handle)[:48]}".rstrip("_")
        suffix = Path(urlsplit(img_url).path).suffix.lower() or ".jpg"
        asset_path = DRAWABLE_DIR / f"{asset_name}{suffix}"
        if not asset_path.exists():
            try:
                blob = fetch(img_url, binary=True)
                asset_path.write_bytes(blob)
                descargadas += 1
                time.sleep(0.15)
            except Exception as e:
                print(f"  ! no pude bajar {img_url}: {e}")
                continue
        else:
            descargadas += 1

        out.append({
            "id": handle,
            "asset_name": asset_name,
            "asset_file": asset_path.name,
            "title": title,
            "price_clp": price,
            "category_demo_id": category_from_type(ptype, title),
            "product_type": ptype,
            "image_url": img_url,
        })

    META_OUT.write_text(json.dumps(out, indent=2, ensure_ascii=False), encoding="utf-8")
    print(f"\nGuardadas {descargadas} imagenes")
    print(f"Metadata escrita a {META_OUT}")
    print("Resumen por categoria:")
    counts: dict[str, int] = {}
    for item in out:
        counts[item["category_demo_id"]] = counts.get(item["category_demo_id"], 0) + 1
    for cat, n in sorted(counts.items(), key=lambda kv: -kv[1]):
        print(f"  {cat}: {n}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
