"""Generate DMCL brand assets from one pixel grid. Run: python branding/build.py"""
from pathlib import Path
from PIL import Image

OUT = Path(__file__).parent
RES = OUT.parent / "src/main/resources/assets/dmcl"
C = {".": "#12141C", "G": "#6FBF3E", "I": "#5B63F0", "A": "#FFC247", "W": "#F4F6FA", "D": "#252938"}

def mark():
    g = [["."] * 16 for _ in range(16)]
    def box(x0, y0, x1, y1, ch, cut):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                if (x, y) not in cut:
                    yield x, y, ch
    left = {(x, y) for x, y, _ in box(1, 2, 9, 8, "G", {(1, 2), (9, 2), (1, 8), (9, 8)})}
    left |= {(2, 9), (3, 9), (2, 10)}
    right = {(x, y) for x, y, _ in box(6, 6, 14, 12, "I", {(6, 6), (14, 6), (6, 12), (14, 12)})}
    right |= {(12, 13), (13, 13), (13, 14)}
    for x, y in left: g[y][x] = "G"
    for x, y in right: g[y][x] = "I"
    for x, y in left & right: g[y][x] = "A"
    for x, y in [(3, 4), (4, 4), (5, 4), (6, 4), (7, 4), (3, 6), (4, 6),
                 (11, 8), (12, 8), (8, 10), (9, 10), (10, 10), (11, 10), (12, 10)]:
        g[y][x] = "W"
    return g

FONT = {  # 5x5 pixel letters
 "A": ".###.|#...#|#####|#...#|#...#", "C": ".####|#....|#....|#....|.####",
 "D": "####.|#...#|#...#|#...#|####.", "E": "#####|#....|####.|#....|#####",
 "F": "#####|#....|####.|#....|#....", "I": "#####|..#..|..#..|..#..|#####",
 "L": "#....|#....|#....|#....|#####", "M": "#...#|##.##|#.#.#|#...#|#...#",
 "N": "#...#|##..#|#.#.#|#..##|#...#", "O": ".###.|#...#|#...#|#...#|.###.",
 "R": "####.|#...#|####.|#..#.|#...#", "S": ".####|#....|.###.|....#|####.",
 "T": "#####|..#..|..#..|..#..|..#..",
}

def text_cells(s, x, y):
    for ch in s:
        if ch == " ":
            x += 4; continue
        for r, row in enumerate(FONT[ch].split("|")):
            for c, v in enumerate(row):
                if v == "#": yield x + c, y + r
        x += 6

def render(cells, w, h, fname, scale):
    """cells: list of (x, y, color, size_in_cells) on a w*h cell canvas."""
    rects = "".join(f'<rect x="{x}" y="{y}" width="{s}" height="{s}" fill="{c}"/>' for x, y, c, s in cells)
    svg = (f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {w} {h}" width="{w*scale}" height="{h*scale}" '
           f'shape-rendering="crispEdges"><rect width="{w}" height="{h}" fill="{C["."]}"/>{rects}</svg>')
    (OUT / f"{fname}.svg").write_text(svg)
    im = Image.new("RGB", (w * scale, h * scale), C["."])
    px = im.load()
    for x, y, c, s in cells:
        rgb = Image.new("RGB", (1, 1), c).getpixel((0, 0))
        for yy in range(y * scale, (y + s) * scale):
            for xx in range(x * scale, (x + s) * scale):
                px[xx, yy] = rgb
    return im

m = mark()
icon_cells = [(x, y, C[v], 1) for y, row in enumerate(m) for x, v in enumerate(row) if v != "."]
icon = render(icon_cells, 16, 16, "icon", 32)
icon.save(OUT / "icon-512.png")
icon.resize((128, 128), Image.NEAREST).save(RES / "icon.png")

# banner: 320x160 cells at 4px = 1280x640
W, H = 320, 160
cells = [(20 + x * 5, 55 + y * 5, C[v], 5) for y, row in enumerate(m) for x, v in enumerate(row) if v != "."]
for x, y in text_cells("DMCL", 0, 0):
    cells.append((120 + x * 8, 44 + y * 8, C["W"], 8))
for i in range(12):  # alternating green/indigo bridge bar
    cells.append((120 + i * 8, 92, C["I"] if i % 2 else C["G"], 8))
for line, ty in (("MINECRAFT", 108), ("TO DISCORD", 130)):
    for x, y in text_cells(line, 0, 0):
        cells.append((120 + x * 3, ty + y * 3, "#9AA3C0", 3))
render(cells, W, H, "banner", 4).save(OUT / "banner.png")
print("ok")
