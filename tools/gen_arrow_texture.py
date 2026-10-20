"""Draws the off-screen arrow (assets/wheredidiputit/textures/gui/arrow.png): a white arrowhead pointing right,
outlined in dark grey so it shows on bright skies. The game tints it with the highlight color, which leaves the
outline dark. Drawn at 4x and scaled down for smooth edges. Run: python tools/gen_arrow_texture.py"""
from pathlib import Path

from PIL import Image, ImageDraw

SIZE = 16
SCALE = 4
OUT = Path(__file__).resolve().parent.parent / "common/src/main/resources/assets/wheredidiputit/textures/gui/arrow.png"


def arrow(inset):
    s = SCALE
    # Tip on the right, two barbs at the left and a notch between them.
    points = [(15.0 - inset, 8.0), (2.0 + inset, 2.0 + inset * 1.6), (5.5 + inset, 8.0), (2.0 + inset, 14.0 - inset * 1.6)]
    return [(x * s, y * s) for x, y in points]


def main():
    big = Image.new("RGBA", (SIZE * SCALE, SIZE * SCALE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(big)
    draw.polygon(arrow(0.0), fill=(40, 40, 40, 230))
    draw.polygon(arrow(1.3), fill=(255, 255, 255, 255))
    small = big.resize((SIZE, SIZE), Image.LANCZOS)
    OUT.parent.mkdir(parents=True, exist_ok=True)
    small.save(OUT)
    print(f"wrote {OUT}")


if __name__ == "__main__":
    main()
