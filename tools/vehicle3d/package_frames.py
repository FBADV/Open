"""Empacota os quadros renderizados no app.

uso: python3 tools/vehicle3d/package_frames.py <pasta_render>
     (<pasta_render>/day/NN.png, night/NN.png, top_day.png, top_night.png)
Recorta todos os quadros pelo mesmo retângulo (o carro não "pula" no giro),
converte para WebP com transparência e grava em app/src/main/assets/car360/.
"""
import glob, os, shutil, sys
from PIL import Image

src = sys.argv[1]
out = os.path.join(os.path.dirname(__file__), "..", "..", "app", "src", "main", "assets", "car360")
frames = sorted(glob.glob(f"{src}/day/*.png")) + sorted(glob.glob(f"{src}/night/*.png"))
bb = None
for f in frames:
    b = Image.open(f).getbbox()
    bb = b if bb is None else (min(bb[0], b[0]), min(bb[1], b[1]), max(bb[2], b[2]), max(bb[3], b[3]))
W, H = Image.open(frames[0]).size
bb = (max(0, bb[0] - 8), max(0, bb[1] - 8), min(W, bb[2] + 8), min(H, bb[3] + 8))
shutil.rmtree(out, ignore_errors=True)
for mode in ("day", "night"):
    os.makedirs(f"{out}/{mode}", exist_ok=True)
    for i, f in enumerate(sorted(glob.glob(f"{src}/{mode}/*.png"))):
        Image.open(f).crop(bb).save(f"{out}/{mode}/{i:02d}.webp", "WEBP", quality=86, method=6)
    top = f"{src}/top_{mode}.png"
    if os.path.exists(top):
        im = Image.open(top); im = im.crop(im.getbbox()).rotate(-90, expand=True)  # frente para a esquerda
        im.save(f"{out}/top_{mode}.webp", "WEBP", quality=88)
print("ok:", out, bb)
