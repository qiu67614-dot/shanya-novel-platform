"""Generate the local layered mountain-study illustration used by the home screen."""

from pathlib import Path
import random

from PIL import Image, ImageDraw, ImageFilter


random.seed(17)
OUT = Path(__file__).resolve().parents[1] / "static" / "home"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1400, 820


def poly(draw, points, fill, outline=None, width=1):
    draw.polygon(points, fill=fill)
    if outline:
        draw.line(points + [points[0]], fill=outline, width=width, joint="curve")


base = Image.new("RGB", (W, H), "#e4ded0")
d = ImageDraw.Draw(base)
d.rectangle((0, 0, W, 600), fill="#e8e4d8")
d.rectangle((0, 605, W, H), fill="#b8a28b")

# Window sky and overlapping mountain silhouettes.
d.rectangle((100, 72, 1000, 554), fill="#cdd9d0")
for y in range(73, 554):
    t = (y - 73) / 481
    d.line((101, y, 999, y), fill=(int(204 - t * 40), int(217 - t * 27), int(210 - t * 32)))
d.ellipse((680, 130, 815, 265), fill="#e6dfbd")
poly(d, [(100, 370), (200, 318), (295, 345), (414, 232), (550, 310), (657, 250), (773, 349), (920, 293), (1000, 344), (1000, 554), (100, 554)], "#849a91")
poly(d, [(100, 421), (260, 384), (360, 431), (470, 340), (610, 408), (760, 338), (900, 407), (1000, 385), (1000, 554), (100, 554)], "#5e7c70")
poly(d, [(100, 486), (210, 452), (355, 473), (517, 426), (678, 476), (795, 422), (1000, 457), (1000, 554), (100, 554)], "#344f49")
for i in range(28):
    x = 130 + i * 32
    y = 510 + random.randint(-23, 18)
    d.line((x, 550, x, y), fill="#27443d", width=random.randint(3, 6))
    d.ellipse((x - 12, y - 16, x + 11, y + 11), fill="#28463d")

# Window lattice, side wall, cabinet and quiet small objects.
d.rectangle((88, 60, 1013, 570), outline="#5f5749", width=20)
d.rectangle((531, 67, 552, 560), fill="#665c4e")
d.rectangle((99, 315, 1003, 329), fill="#665c4e")
d.rectangle((1008, 0, W, 605), fill="#d5c9b4")
d.rectangle((1064, 154, 1338, 628), fill="#776b58")
d.rectangle((1084, 174, 1318, 606), fill="#9c876e")
for y in (286, 420, 552):
    d.rectangle((1077, y, 1327, y + 13), fill="#625847")
for x, y, ww, hh, color in [
    (1100, 195, 24, 90, "#354b43"), (1128, 207, 32, 77, "#b6674d"),
    (1166, 190, 19, 95, "#6f473d"), (1191, 215, 35, 70, "#d5c6aa"),
    (1110, 328, 24, 88, "#c7b39a"), (1142, 337, 32, 79, "#3b5b4c"),
    (1181, 321, 18, 95, "#6b5b4b"), (1210, 350, 44, 67, "#b77055"),
    (1101, 460, 27, 85, "#7d4b3e"), (1132, 473, 35, 72, "#d7c8a8"),
    (1174, 461, 23, 84, "#3f5546"), (1203, 477, 34, 68, "#a76b4e")]:
    d.rectangle((x, y, x + ww, y + hh), fill=color)
    d.line((x + 4, y + 8, x + ww - 4, y + 8), fill="#e5d8be", width=2)
d.rectangle((0, 590, W, 609), fill="#655b4b")

# Fine grain makes the image feel printed rather than geometrically flat.
grain = Image.new("RGBA", (W, H), (0, 0, 0, 0))
gd = ImageDraw.Draw(grain)
for _ in range(16000):
    x, y = random.randrange(W), random.randrange(H)
    gd.point((x, y), fill=(55, 46, 38, random.randint(6, 25)))
base = Image.alpha_composite(base.convert("RGBA"), grain)
base.save(OUT / "study-back.png", optimize=True)

front = Image.new("RGBA", (W, H), (0, 0, 0, 0))
f = ImageDraw.Draw(front)
# Desk, edge and the stack of real books.
poly(f, [(0, 626), (1400, 626), (1400, 756), (0, 756)], "#584a3d")
poly(f, [(0, 626), (1400, 626), (1400, 649), (0, 649)], "#876a50")
f.line((0, 663, W, 663), fill="#aa8665", width=3)
for yy, ww, col in [(612, 242, "#b3654c"), (595, 224, "#e4d4b5"), (578, 218, "#37574c")]:
    f.rounded_rectangle((99, yy, 99 + ww, yy + 19), radius=2, fill=col, outline="#3f3c33", width=2)
    f.line((105, yy + 5, 100 + ww - 5, yy + 5), fill="#f0e7d4", width=2)
# Ceramic cup, paper and pen.
f.ellipse((1054, 584, 1214, 615), fill="#e3d1b7", outline="#5a594d", width=4)
f.rounded_rectangle((1070, 570, 1194, 626), radius=11, fill="#f3e6ce", outline="#5a594d", width=4)
f.ellipse((1080, 565, 1183, 583), fill="#77644e")
f.arc((1175, 572, 1235, 620), -65, 115, fill="#594d42", width=7)
# Open book; the centre crease is intentionally a crisp line.
poly(f, [(450, 582), (681, 564), (695, 644), (459, 663), (416, 640)], "#b8a98e", "#49453b", 4)
poly(f, [(695, 644), (710, 564), (957, 584), (986, 639), (938, 663)], "#b8a98e", "#49453b", 4)
poly(f, [(447, 575), (680, 553), (701, 629), (457, 649)], "#f1e8d2", "#665e4f", 3)
poly(f, [(710, 553), (950, 576), (973, 647), (701, 629)], "#f0e6d1", "#665e4f", 3)
f.line((700, 630, 695, 558), fill="#a98d70", width=4)
for y in (582, 595, 608, 621):
    f.line((480, y, 665, y - 15), fill="#a49c87", width=2)
    f.line((732, y - 15, 934, y + 3), fill="#a49c87", width=2)
f.line((870, 635, 1040, 585), fill="#594c3e", width=5)
f.polygon([(1036, 583), (1053, 578), (1043, 593)], fill="#b56c4d")
front.save(OUT / "study-front.png", optimize=True)

page = Image.new("RGBA", (W, H), (0, 0, 0, 0))
p = ImageDraw.Draw(page)
poly(p, [(704, 551), (945, 574), (968, 640), (704, 624)], (251, 242, 220, 225), (103, 92, 75, 210), 3)
for y in (579, 592, 605):
    p.line((731, y - 16, 934, y + 3), fill=(150, 141, 122, 130), width=2)
page.save(OUT / "study-page.png", optimize=True)

light = Image.new("RGBA", (W, H), (0, 0, 0, 0))
l = ImageDraw.Draw(light)
for radius in range(290, 0, -5):
    alpha = int(0.14 * (1 - radius / 290) * 255)
    l.ellipse((745 - radius, 345 - radius, 745 + radius, 345 + radius), fill=(247, 225, 166, alpha))
light = light.filter(ImageFilter.GaussianBlur(55))
light.save(OUT / "study-light.png", optimize=True)

for file in OUT.glob("*.png"):
    print(f"{file.name}: {file.stat().st_size // 1024} KB")
