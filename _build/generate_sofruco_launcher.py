"""Genera mipmaps de Android para el flavor sofruco a partir de docs/_assets/sofruco/logo.png.

Densidades estandar de Android:
- mdpi:    48x48
- hdpi:    72x72
- xhdpi:   96x96
- xxhdpi: 144x144
- xxxhdpi:192x192

El logo Sofruco no es cuadrado perfecto (443x427), asi que lo centramos en un canvas
cuadrado verde Sofruco (#5F9A3B) con padding 8% para que en el adaptive-icon de
Android no se corte el isotipo del arbol con el corte circular del launcher.
"""
from PIL import Image
from pathlib import Path

SRC = Path("c:/others/own/docs/_assets/sofruco/logo.png")
ROUND_SRC = SRC  # Mismo origen; el sistema lo recorta circular si corresponde
SOFRUCO_RES = Path("c:/others/own/frutapp/app/src/sofruco/res")
GREEN = (95, 154, 59, 255)  # #5F9A3B verde Sofruco

DENSITIES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}


def build_icon(size: int) -> Image.Image:
    canvas = Image.new("RGBA", (size, size), GREEN)
    logo = Image.open(SRC).convert("RGBA")
    # Resize manteniendo aspecto para que quepa al 84% del canvas (padding seguro adaptive)
    inner = int(size * 0.84)
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


for density, size in DENSITIES.items():
    out_dir = SOFRUCO_RES / f"mipmap-{density}"
    out_dir.mkdir(parents=True, exist_ok=True)
    icon = build_icon(size)
    icon.save(out_dir / "ic_launcher.png")
    icon.save(out_dir / "ic_launcher_round.png")
    print(f"  {density}: {size}x{size} -> {out_dir}/ic_launcher.png")

# Sobrescribe el adaptive-icon del main para que en API26+ tambien use el PNG
# (sin esto, el sistema usa el adaptive XML del main que apunta a vectores FrutApp).
anydpi = SOFRUCO_RES / "mipmap-anydpi-v26"
anydpi.mkdir(parents=True, exist_ok=True)
# Trick: declarar el adaptive-icon como pure bitmap usando el ic_launcher PNG como background y un foreground transparente.
adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher" />
    <foreground android:drawable="@android:color/transparent" />
</adaptive-icon>
"""
(anydpi / "ic_launcher.xml").write_text(adaptive_xml, encoding="utf-8")
(anydpi / "ic_launcher_round.xml").write_text(adaptive_xml, encoding="utf-8")
print(f"  adaptive-icon override -> {anydpi}/ic_launcher.xml")
print("DONE")
