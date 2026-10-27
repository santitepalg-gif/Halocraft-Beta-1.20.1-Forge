#!/usr/bin/env python3
"""Renderiza vistas previas de los modelos del mod (geo + textura) en previews/.

Uso: python3 tools/preview.py        (requiere Pillow y numpy)
Pose de reposo, 3 vistas por mob + comparativo de tamaños (con la escala del addon).
Convencion Bedrock: frente = -Z; rotaciones en grados (x positivo inclina hacia el frente).
"""
import json, math, os
import numpy as np
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src/main/resources/assets/halomobs')
PREV = os.path.join(ROOT, 'previews')
SIZES = json.load(open(os.path.join(ROOT, 'tools/mob_sizes.json')))
TITLES = {'elite': 'Elite (Menor)', 'grunt': 'Grunt', 'jackal': 'Jackal', 'skirmisher': 'Skirmisher', 'hunter': 'Hunter',
          'brute_capitan': 'Brute Capitán', 'brute_cacique': 'Brute Cacique'}

def rot(j):
    """Matriz 3x3 de una rotacion Bedrock [x,y,z] en grados (aplica X, luego Y, luego Z)."""
    ax, ay, az = math.radians(-j[0]), math.radians(j[1]), math.radians(-j[2])
    cx, sx, cy, sy, cz, sz = math.cos(ax), math.sin(ax), math.cos(ay), math.sin(ay), math.cos(az), math.sin(az)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]]); Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx

def load(mid):
    g = json.load(open(f'{ASSETS}/geo/entity/{mid}.geo.json'))['minecraft:geometry'][0]
    tex = np.array(Image.open(f'{ASSETS}/textures/entity/{mid}.png').convert('RGBA'))
    bones = {b['name']: b for b in g['bones']}
    return g['description'], bones, tex

def chain(bones, name):
    out = []
    while name:
        b = bones[name]; out.append(b); name = b.get('parent')
    return out

def build_faces(desc, bones, tex, scale=1.0):
    TW, TH = desc['texture_width'], desc['texture_height']
    kx, ky = tex.shape[1] / TW, tex.shape[0] / TH
    faces = []
    for bn, b in bones.items():
        if b.get('neverRender'): continue
        ch = chain(bones, bn)
        for c in b.get('cubes', []):
            o = np.array(c['origin'], float); s = np.array(c['size'], float); inf = c.get('inflate', b.get('inflate', 0)) or 0
            o2 = o - inf; s2 = s + 2 * inf
            x0, y0, z0 = o2; x1, y1, z1 = o2 + s2
            fd = {
                'north': ((x1, y1, z0), (-s2[0], 0, 0), (0, -s2[1], 0), (0, 0, -1)),
                'south': ((x0, y1, z1), (s2[0], 0, 0), (0, -s2[1], 0), (0, 0, 1)),
                'east': ((x1, y1, z1), (0, 0, -s2[2]), (0, -s2[1], 0), (1, 0, 0)),
                'west': ((x0, y1, z0), (0, 0, s2[2]), (0, -s2[1], 0), (-1, 0, 0)),
                'up': ((x1, y1, z1), (-s2[0], 0, 0), (0, 0, -s2[2]), (0, 1, 0)),
                'down': ((x1, y0, z0), (-s2[0], 0, 0), (0, 0, s2[2]), (0, -1, 0))}
            uv = c.get('uv'); mirror = c.get('mirror', b.get('mirror', False))
            dx, dy, dz = s
            rects = {}
            if isinstance(uv, list):
                u, v = uv
                rects = {'north': (u + dz, v + dz, dx, dy), 'south': (u + 2 * dz + dx, v + dz, dx, dy),
                         'east': (u, v + dz, dz, dy), 'west': (u + dz + dx, v + dz, dz, dy),
                         'up': (u + dz, v, dx, dz), 'down': (u + dz + dx, v, dx, dz)}
                if mirror:
                    rects['east'], rects['west'] = rects['west'], rects['east']
            elif isinstance(uv, dict):
                for fn, f in uv.items():
                    rects[fn] = (f['uv'][0], f['uv'][1], f['uv_size'][0], f['uv_size'][1])
            piv = np.array(c.get('pivot', [0, 0, 0]), float); crot = c.get('rotation')
            for fn, (P, U, V, n) in fd.items():
                if fn not in rects: continue
                P, U, V, n = map(lambda a: np.array(a, float), (P, U, V, n))
                if crot and any(crot):
                    R = rot(crot); P = R @ (P - piv) + piv; U, V, n = R @ U, R @ V, R @ n
                for bb in ch:
                    br = bb.get('rotation')
                    if br and any(br):
                        R = rot(br); pv = np.array(bb.get('pivot', [0, 0, 0]), float)
                        P = R @ (P - pv) + pv; U, V, n = R @ U, R @ V, R @ n
                rx, ry, rw, rh = rects[fn]
                flip = mirror and isinstance(uv, list)
                faces.append((P * scale, U * scale, V * scale, n, (rx * kx, ry * ky, rw * kx, rh * ky), flip))
    return faces

LIGHT = np.array([0.35, 0.8, -0.5]); LIGHT /= np.linalg.norm(LIGHT)

def render(faces, tex, yaw, pitch, sc, W, H, groundY, ss=3, cxo=0.0):
    th, ph = math.radians(yaw), math.radians(pitch)
    cam = np.array([math.sin(th) * math.cos(ph), math.sin(ph), -math.cos(th) * math.cos(ph)])
    f = -cam; r = np.cross(f, [0, 1, 0.0]); r /= np.linalg.norm(r); u = np.cross(r, f)
    Ws, Hs = W * ss, H * ss
    img = np.zeros((Hs, Ws, 3), np.float32); zb = np.full((Hs, Ws), 1e9, np.float32); al = np.zeros((Hs, Ws), np.float32)
    k = sc * ss; cx, cy = Ws / 2 + cxo * ss, groundY * ss
    pr = lambda p: (cx + k * np.dot(r, p), cy - k * np.dot(u, p))
    TH_, TW_ = tex.shape[0], tex.shape[1]
    for P, U, V, n, (rx, ry, rw, rh), flip in faces:
        if np.dot(n, cam) <= 1e-4: continue
        p0 = pr(P); pu = pr(P + U); pv = pr(P + V); puv = pr(P + U + V)
        A = (pu[0] - p0[0], pu[1] - p0[1]); B = (pv[0] - p0[0], pv[1] - p0[1])
        det = A[0] * B[1] - A[1] * B[0]
        if abs(det) < 1e-6: continue
        xs = [p0[0], pu[0], pv[0], puv[0]]; ys = [p0[1], pu[1], pv[1], puv[1]]
        x0, x1 = max(0, int(min(xs)) - 1), min(Ws, int(max(xs)) + 2); y0, y1 = max(0, int(min(ys)) - 1), min(Hs, int(max(ys)) + 2)
        if x0 >= x1 or y0 >= y1: continue
        gx, gy = np.meshgrid(np.arange(x0, x1) + 0.5, np.arange(y0, y1) + 0.5)
        dx, dy = gx - p0[0], gy - p0[1]
        s = (dx * B[1] - dy * B[0]) / det; t = (A[0] * dy - A[1] * dx) / det
        mask = (s >= 0) & (s < 1) & (t >= 0) & (t < 1)
        z = np.dot(f, P) + s * np.dot(f, U) + t * np.dot(f, V)
        sub = zb[y0:y1, x0:x1]; mask &= z < sub
        if not mask.any(): continue
        ss_ = 1 - s if flip else s
        tx = np.clip(np.floor(rx + ss_ * rw).astype(int) if rw >= 0 else np.floor(rx + ss_ * rw).astype(int), 0, TW_ - 1)
        ty = np.clip(np.floor(ry + t * rh).astype(int), 0, TH_ - 1)
        texel = tex[ty, tx]
        mask &= texel[..., 3] > 20
        nl = n / (np.linalg.norm(n) + 1e-9)
        lt = 0.78 + 0.42 * max(0.0, float(np.dot(nl, LIGHT)))
        col = texel[..., :3].astype(np.float32) * lt
        img[y0:y1, x0:x1][mask] = col[mask]; sub[mask] = z[mask]; al[y0:y1, x0:x1][mask] = 1
    rgb = Image.fromarray(np.clip(img, 0, 255).astype(np.uint8), 'RGB')
    a = Image.fromarray((al * 255).astype(np.uint8), 'L')
    return rgb.resize((W, H), Image.LANCZOS), a.resize((W, H), Image.LANCZOS)

def font(sz):
    for p in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'):
        if os.path.exists(p): return ImageFont.truetype(p, sz)
    return ImageFont.load_default()

def backdrop(W, H):
    bg = Image.new('RGB', (W, H)); d = ImageDraw.Draw(bg)
    for y in range(H):
        k = y / H; d.line([(0, y), (W, y)], fill=(int(34 + 22 * k), int(40 + 22 * k), int(52 + 24 * k)))
    return bg

def shadow(bg, cx, gy, w):
    sh = Image.new('RGBA', bg.size, (0, 0, 0, 0))
    ImageDraw.Draw(sh).ellipse([cx - w, gy - w * 0.2, cx + w, gy + w * 0.2], fill=(0, 0, 0, 110))
    return Image.alpha_composite(bg.convert('RGBA'), sh).convert('RGB')

def bounds(faces):
    pts = np.array([P + a * U + b * V for P, U, V, *_ in faces for a in (0, 1) for b in (0, 1)])
    return pts.min(0), pts.max(0)

def main():
    os.makedirs(PREV, exist_ok=True)
    built = {}
    for mid in SIZES:
        desc, bones, tex = load(mid)
        faces = build_faces(desc, bones, tex, scale=SIZES[mid]['scale'])
        built[mid] = (faces, tex)
    PW, PH, ground = 480, 740, 680
    views = ((-32, 'frontal 3/4'), (90, 'perfil'), (148, 'trasera 3/4'))
    for mid, (faces, tex) in built.items():
        lo, hi = bounds(faces)
        h = hi[1]; ext = math.hypot(hi[0] - lo[0], hi[2] - lo[2]) * 0.95
        sc = min(560 / (h * 1.04), PW * 0.9 / ext)
        out = Image.new('RGB', (PW * 3, PH)); dr = ImageDraw.Draw(out)
        for i, (yaw, label) in enumerate(views):
            bg = backdrop(PW, PH); bg = shadow(bg, PW // 2, ground, int(sc * 8))
            rgb, a = render(faces, tex, yaw, 12, sc, PW, PH, ground)
            bg.paste(rgb, (0, 0), a); out.paste(bg, (i * PW, 0))
            dr.text((i * PW + 20, 76), label, font=font(16), fill=(160, 172, 190))
        dr.text((20, 22), TITLES[mid], font=font(34), fill=(240, 244, 250))
        out.save(f'{PREV}/{mid}.png')
        print('preview', mid)
    names = list(built); LW, LH, gr = 2600, 860, 800
    sc = 800 * 0.78 / max(bounds(built[m][0])[1][1] for m in built)
    bg = backdrop(LW, LH); step = LW // len(names)
    for i, mid in enumerate(names):
        faces, tex = built[mid]; cxm = i * step + step // 2
        bg = shadow(bg, cxm, gr, 60)
        rgb, a = render(faces, tex, -28, 12, sc, 420, LH, gr)
        bg.paste(rgb, (cxm - 210, 0), a)
    d = ImageDraw.Draw(bg)
    for i, mid in enumerate(names):
        t = TITLES[mid]; w = d.textlength(t, font=font(22))
        d.text((i * step + step // 2 - w / 2, gr + 34), t, font=font(22), fill=(235, 240, 248))
    d.text((24, 18), 'Halo Mobs: comparación de tamaños', font=font(30), fill=(240, 244, 250))
    bg.save(f'{PREV}/lineup.png')

if __name__ == '__main__':
    main()
