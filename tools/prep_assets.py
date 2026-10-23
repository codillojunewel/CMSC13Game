"""Builds the game's background art from the mockup images in MP_Proposal.pdf.

Each mockup is scaled to the game's 1280x720 design resolution; baked-in
dynamic parts (loading bar, question text, labels...) are erased so the game
can draw live versions at the exact same spots.
Usage: python prep_assets.py <dir with img-00N.png extracted by pdfimages> <out dir>
"""
import sys, os
import numpy as np
from PIL import Image, ImageFilter, ImageDraw

SRC, OUT = sys.argv[1], sys.argv[2]
os.makedirs(OUT, exist_ok=True)


def to1280(path, by_width=False):
    im = Image.open(path).convert('RGB')
    w, h = im.size
    if by_width:
        s = 1280 / w
        nw, nh = 1280, round(h * s)
    else:
        s = 720 / h
        nw, nh = round(w * s), 720
    im = im.resize((nw, nh), Image.LANCZOS)
    x, y = (nw - 1280) // 2, (nh - 720) // 2
    return np.array(im.crop((x, y, x + 1280, y + 720))).astype(np.int32)


def rowfill(a, x0, x1, y0, y1, sx0, sx1):
    """Fill box with each row's median colour taken from the strip sx0..sx1 (same rows)."""
    strip = np.median(a[y0:y1, sx0:sx1], axis=1, keepdims=True)
    a[y0:y1, x0:x1] = strip


def save(a, name, q=92):
    img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8))
    if name.endswith('.jpg'):
        img.save(os.path.join(OUT, name), quality=q, optimize=True)
    else:
        img.save(os.path.join(OUT, name))


P = lambda n: os.path.join(SRC, f'img-00{n}.png')

# ---- splash: remove the static bar fill and the "78%"
a = to1280(P(0))
col = a[545:571, 772:775].mean(axis=1, keepdims=True)
a[545:571, 458:772] = col
a[536:574, 793:845] = a[536:574, 850:902]
save(a, 'splash_bg.jpg')

# ---- blank frame (for How to Play / Settings / Reboot): erase title + loading
f = a.copy()
patch = f[150:225, 1012:1090].copy()
box = (195, 112, 1085, 592)
for yy in range(box[1], box[3], patch.shape[0]):
    for xx in range(box[0], box[2], patch.shape[1]):
        h = min(patch.shape[0], box[3] - yy)
        w = min(patch.shape[1], box[2] - xx)
        f[yy:yy + h, xx:xx + w] = patch[:h, :w]
img = Image.fromarray(f.astype(np.uint8))
region = img.crop(box).filter(ImageFilter.GaussianBlur(1.2))
img.paste(region, box[:2])
f = np.array(img).astype(np.float64)
yy, xx = np.mgrid[0:720, 0:1280]
glow = np.exp(-(((xx - 640) / 420.0) ** 2 + ((yy - 350) / 230.0) ** 2))
f[..., 0] += 10 * glow
f[..., 1] += 40 * glow
f[..., 2] += 90 * glow
save(f, 'frame_bg.jpg')

# ---- main menu + studio start screen: used as-is
save(to1280(P(1)), 'menu_bg.jpg')
save(to1280(P(2)), 'start_bg.jpg')

# ---- game studio: empty the big question monitor
g = to1280(P(3))
rowfill(g, 391, 931, 54, 303, 892, 927)
save(g, 'game_bg.jpg')

# ---- level complete (as-is) and victory variant (button labels erased)
lc = to1280(P(5))
save(lc, 'level_complete_bg.jpg')
v = lc.copy()
rowfill(v, 330, 575, 606, 664, 316, 326)
rowfill(v, 680, 965, 606, 664, 668, 676)
save(v, 'victory_bg.jpg')

# ---- defeat: erase the error code value so the game can print the real one
d = to1280(P(6), by_width=True)
rowfill(d, 938, 1052, 163, 191, 1056, 1095)
save(d, 'defeat_bg.jpg')

# ---- confession HUD frame (4:3 source coordinates, 1080x809)
c = np.array(Image.open(P(4)).convert('RGBA')).astype(np.int32)
# erase the baked confession text inside the bottom box
strip = np.median(c[648:752, 160:172, :3], axis=1, keepdims=True)
c[648:752, 172:928, :3] = strip
alpha = np.full(c.shape[:2], 255, np.int32)
hole = np.zeros(c.shape[:2], bool)
hole[78:580, 82:1008] = True
# keep the control dock (STOP / REC / SNAPSHOT) opaque: a trapezoid panel
dock = Image.new('L', (c.shape[1], c.shape[0]), 0)
ImageDraw.Draw(dock).polygon([(334, 545), (752, 545), (786, 582), (298, 582)], fill=255)
hole &= ~(np.array(dock) > 0)
cy, cx = np.mgrid[0:c.shape[0], 0:c.shape[1]]
hole &= ~(((cx - 542) ** 2 + (cy - 577) ** 2) <= 56 ** 2)
alpha[hole] = 0
c[..., 3] = alpha
Image.fromarray(c.astype(np.uint8), 'RGBA').save(os.path.join(OUT, 'confession_frame.png'), optimize=True)

# ---- junior dev portraits, cropped from the studio start mockup (full resolution)
src = Image.open(P(2)).convert('RGB')
s = 720 / src.size[1]
off = (round(src.size[0] * s) - 1280) // 2
boxes = {'halima': (485, 285, 590, 395), 'isak': (645, 290, 750, 400), 'aya': (800, 285, 905, 395),
         'haley': (960, 315, 1070, 425), 'cameron': (545, 405, 655, 515), 'priya': (730, 430, 840, 540),
         'player': (180, 280, 310, 410)}
for name, (x0, y0, x1, y1) in boxes.items():
    b = tuple(round(v) for v in ((x0 + off) / s, y0 / s, (x1 + off) / s, y1 / s))
    src.crop(b).resize((220, 220), Image.LANCZOS).save(os.path.join(OUT, f'portrait_{name}.png'))
print('assets ok')
