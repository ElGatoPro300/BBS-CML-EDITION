#!/usr/bin/env python3
"""
Sprite sheet extractor tool.
Usage: python extract_sprite.py <sheet> [options]
"""

import argparse
import os
import sys
from pathlib import Path

try:
    from PIL import Image
except ImportError:
    print("Pillow is required: pip install Pillow")
    sys.exit(1)


def parse_color(value):
    """Parse a color string: 'R,G,B' or 'R,G,B,A' or hex '#RRGGBB'."""
    value = value.strip()

    if value.startswith("#"):
        hex_val = value.lstrip("#")

        if len(hex_val) == 6:
            r, g, b = int(hex_val[0:2], 16), int(hex_val[2:4], 16), int(hex_val[4:6], 16)
            return (r, g, b)
        elif len(hex_val) == 8:
            r, g, b, a = int(hex_val[0:2], 16), int(hex_val[2:4], 16), int(hex_val[4:6], 16), int(hex_val[6:8], 16)
            return (r, g, b, a)

    parts = [int(x.strip()) for x in value.split(",")]

    if len(parts) == 3:
        return tuple(parts)
    elif len(parts) == 4:
        return tuple(parts)

    raise argparse.ArgumentTypeError(f"Invalid color '{value}'. Use R,G,B or #RRGGBB")


def color_distance(c1, c2):
    """Euclidean distance between two RGB(A) colors."""
    return sum((a - b) ** 2 for a, b in zip(c1[:3], c2[:3])) ** 0.5


def remove_color(img, color, tolerance):
    """Replace pixels matching color (within tolerance) with transparency."""
    img = img.convert("RGBA")
    pixels = img.load()
    w, h = img.size

    for y in range(h):
        for x in range(w):
            px = pixels[x, y]

            if color_distance(px, color) <= tolerance:
                pixels[x, y] = (px[0], px[1], px[2], 0)

    return img


def extract_sprites(sheet_path, sprite_w, sprite_h, gap_x, gap_y, offset_x, offset_y,
                    remove_colors, tolerance, output_dir, prefix, rows, cols, skip_empty, empty_threshold):
    sheet = Image.open(sheet_path).convert("RGBA")
    sw, sh = sheet.size

    for color in remove_colors:
        sheet = remove_color(sheet, color, tolerance)

    os.makedirs(output_dir, exist_ok=True)

    col_count = cols if cols > 0 else (sw - offset_x + gap_x) // (sprite_w + gap_x)
    row_count = rows if rows > 0 else (sh - offset_y + gap_y) // (sprite_h + gap_y)

    saved = 0
    skipped = 0

    for row in range(row_count):
        for col in range(col_count):
            x = offset_x + col * (sprite_w + gap_x)
            y = offset_y + row * (sprite_h + gap_y)

            if x + sprite_w > sw or y + sprite_h > sh:
                continue

            sprite = sheet.crop((x, y, x + sprite_w, y + sprite_h))

            if skip_empty:
                pixels = list(sprite.getdata())
                visible = sum(1 for p in pixels if p[3] > empty_threshold)

                if visible == 0:
                    skipped += 1
                    continue

            index = row * col_count + col
            filename = f"{prefix}_{row:02d}_{col:02d}_{index:04d}.png"
            out_path = os.path.join(output_dir, filename)
            sprite.save(out_path)
            saved += 1

    print(f"Done. Saved {saved} sprites to '{output_dir}'" + (f" (skipped {skipped} empty)" if skipped else ""))
    return saved


def interactive_mode():
    print("=== Sprite Sheet Extractor ===\n")

    sheet_path = input("Sprite sheet path: ").strip().strip('"')

    if not os.path.isfile(sheet_path):
        print(f"File not found: {sheet_path}")
        sys.exit(1)

    img = Image.open(sheet_path)
    print(f"Sheet size: {img.width} x {img.height} px\n")

    sprite_w = int(input("Sprite width (px): ").strip())
    sprite_h = int(input("Sprite height (px)  [Enter = same as width]: ").strip() or sprite_w)

    gap_x_str = input("Gap between columns (px) [default 0]: ").strip()
    gap_x = int(gap_x_str) if gap_x_str else 0
    gap_y_str = input("Gap between rows (px)    [default = gap X]: ").strip()
    gap_y = int(gap_y_str) if gap_y_str else gap_x

    offset_x_str = input("Sheet offset X (px) [default 0]: ").strip()
    offset_x = int(offset_x_str) if offset_x_str else 0
    offset_y_str = input("Sheet offset Y (px) [default 0]: ").strip()
    offset_y = int(offset_y_str) if offset_y_str else 0

    cols_str = input("Number of columns [default auto]: ").strip()
    cols = int(cols_str) if cols_str else 0
    rows_str = input("Number of rows    [default auto]: ").strip()
    rows = int(rows_str) if rows_str else 0

    remove_colors = []
    tolerance = 0

    color_str = input("\nBackground color to remove (R,G,B or #RRGGBB, blank to skip): ").strip()

    if color_str:
        remove_colors.append(parse_color(color_str))
        tol_str = input("Color tolerance (0–441, default 30): ").strip()
        tolerance = float(tol_str) if tol_str else 30.0

        while True:
            extra = input("Remove another color? (blank to stop): ").strip()

            if not extra:
                break

            remove_colors.append(parse_color(extra))

    skip_str = input("\nSkip fully transparent sprites? (y/n) [default y]: ").strip().lower()
    skip_empty = skip_str != "n"

    default_out = str(Path(sheet_path).stem) + "_sprites"
    out_str = input(f"Output directory [default '{default_out}']: ").strip()
    output_dir = out_str if out_str else default_out

    prefix_str = input(f"File prefix [default 'sprite']: ").strip()
    prefix = prefix_str if prefix_str else "sprite"

    print()
    extract_sprites(
        sheet_path, sprite_w, sprite_h, gap_x, gap_y, offset_x, offset_y,
        remove_colors, tolerance, output_dir, prefix, rows, cols, skip_empty, empty_threshold=4
    )


def main():
    if len(sys.argv) == 1:
        interactive_mode()
        return

    parser = argparse.ArgumentParser(description="Extract sprites from a sprite sheet.")
    parser.add_argument("sheet", help="Path to the sprite sheet image")
    parser.add_argument("-W", "--width", type=int, required=True, help="Sprite width in pixels")
    parser.add_argument("-H", "--height", type=int, help="Sprite height in pixels (default: same as width)")
    parser.add_argument("--gap-x", type=int, default=0, help="Horizontal gap between sprites (default: 0)")
    parser.add_argument("--gap-y", type=int, help="Vertical gap between sprites (default: same as --gap-x)")
    parser.add_argument("--offset-x", type=int, default=0, help="X offset into the sheet (default: 0)")
    parser.add_argument("--offset-y", type=int, default=0, help="Y offset into the sheet (default: 0)")
    parser.add_argument("--cols", type=int, default=0, help="Number of columns (default: auto)")
    parser.add_argument("--rows", type=int, default=0, help="Number of rows (default: auto)")
    parser.add_argument("--remove-color", type=parse_color, action="append", default=[], metavar="COLOR",
                        help="Background color to remove, e.g. '98,48,130' or '#622082'. Can be repeated.")
    parser.add_argument("--tolerance", type=float, default=30.0,
                        help="Color removal tolerance 0–441 (default: 30)")
    parser.add_argument("--keep-empty", action="store_true",
                        help="Keep fully transparent sprites (skipped by default)")
    parser.add_argument("-o", "--output", default=None, help="Output directory (default: <sheet>_sprites)")
    parser.add_argument("--prefix", default="sprite", help="Output filename prefix (default: sprite)")

    args = parser.parse_args()

    sprite_h = args.height if args.height else args.width
    gap_y = args.gap_y if args.gap_y is not None else args.gap_x
    output_dir = args.output if args.output else Path(args.sheet).stem + "_sprites"

    extract_sprites(
        args.sheet, args.width, sprite_h, args.gap_x, gap_y,
        args.offset_x, args.offset_y, args.remove_color, args.tolerance,
        output_dir, args.prefix, args.rows, args.cols,
        skip_empty=not args.keep_empty, empty_threshold=4
    )


if __name__ == "__main__":
    main()
