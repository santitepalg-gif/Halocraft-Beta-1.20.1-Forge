#!/usr/bin/env python3
"""Vistas previas de los mobs CON arma y del catalogo de armas -> previews/armas_*.png

Uso: python3 tools/gen_weapons.py && python3 tools/preview_weapons.py     (Pillow + numpy)
Es un render de software aproximado (mismo que preview.py), no el render real de Minecraft/GeckoLib:
el arma se coloca en el pivote del hueso rightItem con la misma orientacion que usa el layer del mod.
"""
import copy, json, math, os, sys
import numpy as np
from PIL import Image, ImageDraw

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import preview as P

ROOT = P.ROOT
BOXES = json.load(open(f'{ROOT}/tools/weapon_boxes.json'))
PAL = json.load(open(f'{ROOT}/tools/weapon_palette.json'))
PAL_IDX = {n: i for i, n in enumerate(PAL['pal'])}
PALTEX = np.array(Image.open(f'{P.ASSETS}/textures/item/weapons.png').convert('RGBA'))
WEAPON_SCALE = 1.4   # igual que HaloMobRenderer.WEAPON_SCALE
WDESC = {'texture_width': 16, 'texture_height': 16}
TITLES = dict(P.TITLES)
WNAMES = {'plasma_pistol': 'Plasma Pistol', 'plasma_rifle': 'Plasma Rifle', 'needler': 'Needler', 'plasma_repeater': 'Plasma Repeater',
          'carbine': 'Carbine', 'beam_rifle': 'Beam Rifle', 'spiker': 'Spiker', 'mauler': 'Mauler', 'brute_shot': 'Brute Shot',
          'energy_sword': 'Energy Sword'}

def uvs(color):
    i = PAL_IDX[color]; u, v = (i % 4) * 4 + 1, (i // 4) * 4 + 1
    return {f: {'uv': [u, v], 'uv_size': [2, 2]} for f in ('north', 'south', 'east', 'west', 'up', 'down')}

def cubes_at(weapon, px, py, pz):
    """Cajas del arma en coordenadas crudas de Bedrock, con el agarre en el pivote (px,py,pz) del hueso.
    item(x, y, z) -> render(x, z, -y) con X espejado respecto al modelo Bedrock (GeckoLib invierte X)."""
    out = []
    for x0, x1, y0, y1, z0, z1, c in BOXES[weapon]:
        x0, x1, y0, y1, z0, z1 = [v * WEAPON_SCALE for v in (x0, x1, y0, y1, z0, z1)]
        out.append({'origin': [px - x1, py + z0, pz - y1], 'size': [x1 - x0, z1 - z0, y1 - y0], 'uv': uvs(c)})
    return out

def mob_with_weapon(mid, weapon):
    desc, bones, tex = P.load(mid)
    faces = P.build_faces(desc, bones, tex, scale=P.SIZES[mid]['scale'])
    wb = copy.deepcopy(bones)
    for b in wb.values(): b['cubes'] = []
    hand = wb['rightItem']; px, py, pz = hand['pivot']
    hand['cubes'] = cubes_at(weapon, px, py, pz)
    wf = P.build_faces(WDESC, wb, PALTEX, scale=P.SIZES[mid]['scale'])
    return faces, tex, wf

def main():
    os.makedirs(P.PREV, exist_ok=True)
    PW, PH, ground = 480, 740, 680
    views = ((-32, 'frontal 3/4'), (90, 'perfil'), (148, 'trasera 3/4'))
    cases = [('elite', 'plasma_rifle'), ('elite', 'energy_sword'), ('grunt', 'needler'), ('jackal', 'plasma_pistol'),
             ('skirmisher', 'carbine'), ('brute_capitan', 'spiker')]
    sheet = []
    for mid, wp in cases:
        faces, tex, wf = mob_with_weapon(mid, wp)
        lo, hi = P.bounds(faces)
        h = hi[1]; ext = math.hypot(hi[0] - lo[0], hi[2] - lo[2]) * 0.95
        sc = min(560 / (h * 1.04), PW * 0.9 / ext)
        out = Image.new('RGB', (PW * 3, PH)); dr = ImageDraw.Draw(out)
        for i, (yaw, label) in enumerate(views):
            both_rgb, both_a = render_both(faces, tex, wf, yaw, 12, sc, PW, PH, ground)
            bg = P.backdrop(PW, PH); bg = P.shadow(bg, PW // 2, ground, int(sc * 8)); bg.paste(both_rgb, (0, 0), both_a)
            out.paste(bg, (i * PW, 0))
            dr.text((i * PW + 20, 76), label, font=P.font(16), fill=(160, 172, 190))
        dr.text((20, 22), f'{TITLES[mid]} + {WNAMES[wp]}', font=P.font(34), fill=(240, 244, 250))
        name = f'armas_{mid}_{wp}.png'
        out.save(f'{P.PREV}/{name}'); sheet.append((mid, wp, faces, tex, wf)); print('preview', name)

    # lineup: todos con arma
    LW, LH, gr = 2600, 900, 800
    step = LW // len(sheet)
    sc = 800 * 0.78 / max(P.bounds(f)[1][1] for _, _, f, _, _ in sheet)
    bg = P.backdrop(LW, LH)
    for i, (mid, wp, faces, tex, wf) in enumerate(sheet):
        cxm = i * step + step // 2
        bg = P.shadow(bg, cxm, gr, 60)
        rgb, a = render_both(faces, tex, wf, -28, 12, sc, 420, LH, gr)
        bg.paste(rgb, (cxm - 210, 0), a)
    d = ImageDraw.Draw(bg)
    for i, (mid, wp, *_r) in enumerate(sheet):
        t = f'{TITLES[mid]}'; w = d.textlength(t, font=P.font(22))
        d.text((i * step + step // 2 - w / 2, gr + 24), t, font=P.font(22), fill=(235, 240, 248))
        t2 = WNAMES[wp]; w2 = d.textlength(t2, font=P.font(18))
        d.text((i * step + step // 2 - w2 / 2, gr + 54), t2, font=P.font(18), fill=(160, 172, 190))
    d.text((24, 18), 'Halo Mobs: mobs con arma', font=P.font(30), fill=(240, 244, 250))
    bg.save(f'{P.PREV}/armas_lineup.png'); print('preview armas_lineup')

    # catalogo de armas (perfil)
    ids = list(BOXES); CW, CH = 560, 330
    cat = Image.new('RGB', (CW * 5, CH * 2)); dr = ImageDraw.Draw(cat)
    for k, wid in enumerate(ids):
        bones = {'w': {'name': 'w', 'cubes': [{'origin': [-x1, z0, -y1], 'size': [x1 - x0, z1 - z0, y1 - y0], 'uv': uvs(c)}
                                               for x0, x1, y0, y1, z0, z1, c in BOXES[wid]]}}
        wf = P.build_faces(WDESC, bones, PALTEX, scale=1.0)
        lo, hi = P.bounds(wf)
        span = max(hi[2] - lo[2], 6)
        sc = min(CW * 0.78 / span, 30)
        cx = -(lo[2] + hi[2]) / 2 * 0  # centrado horizontal por el desplazamiento del render
        tile = P.backdrop(CW, CH)
        rgb, a = P.render(wf, PALTEX, 90, 10, sc, CW, CH, CH * 0.58, cxo=((lo[2] + hi[2]) / 2) * sc * 1.0)
        tile.paste(rgb, (0, 0), a)
        ImageDraw.Draw(tile).text((18, 14), WNAMES[wid], font=P.font(24), fill=(240, 244, 250))
        cat.paste(tile, ((k % 5) * CW, (k // 5) * CH))
    cat.save(f'{P.PREV}/armas_catalogo.png'); print('preview armas_catalogo')

def render_both(faces, tex, wfaces, yaw, pitch, sc, W, H, groundY):
    """Cuerpo (textura del mob) + arma (paleta) en UN solo atlas y un z-buffer compartido."""
    th, tw = tex.shape[0], tex.shape[1]
    atlas = np.zeros((max(th, 16), tw + 16, 4), np.uint8)
    atlas[:th, :tw] = tex; atlas[:16, tw:tw + 16] = PALTEX
    shifted = [(Pp, U, V, n, (rx + tw, ry, rw, rh), fl) for Pp, U, V, n, (rx, ry, rw, rh), fl in wfaces]
    return P.render(faces + shifted, atlas, yaw, pitch, sc, W, H, groundY)

if __name__ == '__main__':
    main()
