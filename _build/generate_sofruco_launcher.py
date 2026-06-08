"""Genera adaptive-icon assets para el flavor sofruco.

Estrategia (la correcta segun Android docs):
- El adaptive-icon del flavor main YA define:
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
- Para sofruco SOLO sobrescribimos los dos drawables (background + foreground)
  manteniendo los nombres. Android resuelve los drawables del flavor sofruco
  cuando compila ese variant, sin que tengamos que tocar el XML del adaptive-icon.

Background: drawable vector con color #5F9A3B (verde Sofruco).
Foreground: PNG del isotipo Sofruco (logo.png) sobre canvas transparente con
padding 25% — esto respeta la "safe zone" del adaptive-icon: Android puede
recortarlo en circulo, squircle o roundrect sin perder partes del logo.

Densidades estandar (foreground PNG):
- mdpi:    108x108 (38% del area visible total de 48dp)
- hdpi:    162x162
- xhdpi:   216x216
- xxhdpi:  324x324
- xxxhdpi: 432x432
"""
from PIL import Image
from pathlib import Path

SRC = Path("c:/others/own/docs/_assets/sofruco/logo.png")
SOFRUCO_RES = Path("c:/others/own/frutapp/app/src/sofruco/res")

# Adaptive-icon foreground: 108dp x 108dp donde solo los 72dp centrales son visibles
# en algunos shapes. Densidades en px:
FOREGROUND_SIZES = {
    "mdpi": 108,
    "hdpi": 162,
    "xhdpi": 216,
    "xxhdpi": 324,
    "xxxhdpi": 432,
}


def build_foreground(size: int) -> Image.Image:
    """Canvas transparente con el logo centrado al 55% para que quepa en la
    safe zone (66% del adaptive icon, dejando margen para recorte circular)."""
    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    logo = Image.open(SRC).convert("RGBA")
    inner = int(size * 0.55)
    aspect = logo.width / logo.height
    if aspect >= 1:
        w = inner
        h = int(inner / aspect)
    else:
        h = inner
        w = int(inner * aspect)
    logo_resized = logo.resize((w, h), Image.LANCZOS)
    x = (size - w) // 2
    y = (size - h) // 2
    canvas.alpha_composite(logo_resized, (x, y))
    return canvas


# Foreground PNG por densidad
for density, size in FOREGROUND_SIZES.items():
    out_dir = SOFRUCO_RES / f"drawable-{density}"
    out_dir.mkdir(parents=True, exist_ok=True)
    fg = build_foreground(size)
    fg.save(out_dir / "ic_launcher_foreground.png")
    print(f"  fg {density}: {size}x{size} -> {out_dir}/ic_launcher_foreground.png")

# Background drawable: vector con color verde Sofruco. Sobrescribe el
# ic_launcher_background.xml del main (que era #234B07 FrutApp).
bg_dir = SOFRUCO_RES / "drawable"
bg_dir.mkdir(parents=True, exist_ok=True)
bg_xml = """<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#5F9A3B"
        android:pathData="M0,0h108v108h-108z" />
</vector>
"""
(bg_dir / "ic_launcher_background.xml").write_text(bg_xml, encoding="utf-8")
print(f"  bg -> {bg_dir}/ic_launcher_background.xml")

# El adaptive-icon XML del main (mipmap-anydpi-v26/ic_launcher.xml) NO se
# sobrescribe — referencia @drawable/ic_launcher_background y
# @drawable/ic_launcher_foreground por nombre, que ya estan overrideados arriba.
print("DONE")
