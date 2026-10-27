#!/usr/bin/env python3
"""Importa modelos, texturas y animaciones del resource pack Bedrock "halocraftr" al mod.

Uso:  python3 tools/import_halocraftr.py /ruta/a/halocraftr   (carpeta ya descomprimida)
Genera en src/main/resources/assets/halomobs/:
  geo/entity/<id>.geo.json          modelo original (formato 1.12, lo lee GeckoLib)
  textures/entity/<id>.png          textura original
  textures/entity/<id>_glowmask.png zonas emisivas (canal verde del *_mer.png)
  animations/entity/<id>.animation.json   clips idle/walk/run/attack/dead ya convertidos
Las expresiones Molang de los clips se evaluan a valores fijos (GeckoLib no conoce
query.target_x_rotation, etc.).  Tambien imprime tamaño para las hitboxes.
"""
import json, math, os, re, sys
from PIL import Image

SRC = sys.argv[1] if len(sys.argv) > 1 else '/tmp/hcr/halocraftr'
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, 'src/main/resources/assets/halomobs')

# id: (geo, textura, mer, escala del addon, {rol: (archivo de animacion, clip)})
A = 'animations/'
MOBS = {
    'elite': ('models/entity/elites/halo_elite_menor.geo.json', 'halo_elite_menor', 'halo_elite_menor_mer', 0.85, {
        'idle': (A + 'elites_animation/new/elite_arma_sin_armas.animation.json', 'animation.elite.armasinarmas_general'),
        'walk': (A + 'elites_animation/new/elite_arma_sin_armas.animation.json', 'animation.elite.armasinarmas_correr'),
        'run': (A + 'elites_animation/new/elite_arma_sin_armas.animation.json', 'animation.elite.armasinarmas_correr'),
        'attack': (A + 'elites_animation/new/elite_arma_sola.animation.json', 'animation.elite.armauno_golpe1'),
        'dead': (A + 'elites_animation/new/elite_dead.animation.json', 'animation.elite_dead.dead1')}),
    'grunt': ('models/entity/Grunt/halo_grunt.geo.json', 'halo_grunt_menor', 'halo_grunt_menor_mer', 0.7, {
        'idle': (A + 'grunt_animation/new/halo_grunt_sinarmas.animation.json', 'animation.grunt.armasinarmas_general'),
        'walk': (A + 'grunt_animation/new/halo_grunt_sinarmas.animation.json', 'animation.grunt.armasinarmas_correr'),
        'run': (A + 'grunt_animation/new/halo_grunt_sinarmas.animation.json', 'animation.grunt.armasinarmas_correr'),
        'attack': (A + 'grunt_animation/new/halo_grunt_armasola.animation.json', 'animation.grunt.armauno_golpe1'),
        'dead': (A + 'grunt_animation/new/halo_grunt_armasola.animation.json', 'animation.grunt.dead1')}),
    'jackal': ('models/entity/jackal/halo_jackals.geo.json', 'halo_jackals', 'halo_jackals_mer', 0.7, {
        'idle': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.general'),
        'walk': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.moving'),
        'run': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.running'),
        'attack': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.attack1'),
        'dead': (A + 'jackals_animation/halo_jackals_dead.animation.json', 'animation.halo_jackals.dead')}),
    'skirmisher': ('models/entity/jackal/halo_skimisher.geo.json', 'halo_skimisher', 'halo_skimisher_mer', 0.7, {
        'idle': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.general'),
        'walk': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.moving'),
        'run': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.running'),
        'attack': (A + 'jackals_animation/halo_jackals.animation.json', 'animation.halo_jackals.attack1'),
        'dead': (A + 'jackals_animation/halo_jackals_dead.animation.json', 'animation.halo_jackals.dead')}),
    'hunter': ('models/entity/halo_hunter.geo.json', 'halo_hunter', 'halo_hunter_mer', 1.0, {
        'idle': (A + 'halo_hunter.animation.json', 'animation.halo_hunter.general'),
        'walk': (A + 'halo_hunter.animation.json', 'animation.halo_hunter.correr'),
        'run': (A + 'halo_hunter.animation.json', 'animation.halo_hunter.correr'),
        'attack': (A + 'halo_hunter.animation.json', 'animation.halo_hunter.golpe1'),
        'dead': (A + 'halo_hunter.animation.json', 'animation.halo_hunter.dead')}),
    'brute_capitan': ('models/entity/brute/halo_brute_capitan.geo.json', 'halo_brute_menor', 'halo_brute_menor_mer', 0.85, {
        'idle': (A + 'brutes_animation/new/brute_sin_armas.animation.json', 'animation.brutes.armasinarmas_general'),
        'walk': (A + 'brutes_animation/new/brute_sin_armas.animation.json', 'animation.brutes.armasinarmas_correr'),
        'run': (A + 'brutes_animation/new/brute_sin_armas.animation.json', 'animation.brutes.armasinarmas_correr'),
        'attack': (A + 'brutes_animation/new/brute_sin_armas.animation.json', 'animation.brutes.armasinarmas_golpe1'),
        'dead': (A + 'brutes_animation/new/brute.animation.json', 'animation.brutes.dead1')}),
    'brute_cacique': ('models/entity/brute/halo_brute_cacique.geo.json', 'halo_brute_cacique', 'halo_brute_cacique_mer', 1.0, {
        'idle': (A + 'brutes_animation/halo_brute_cacique.animation.json', 'animation.brute_cacique.general'),
        'walk': (A + 'brutes_animation/halo_brute_cacique.animation.json', 'animation.brute_cacique.running'),
        'run': (A + 'brutes_animation/halo_brute_cacique.animation.json', 'animation.brute_cacique.running'),
        'attack': (A + 'brutes_animation/halo_brute_cacique.animation.json', 'animation.brute_cacique.attack1'),
        'dead': (A + 'brutes_animation/halo_brute_cacique_d.animation.json', 'animation.brute_cacique_d.base1')}),
}

# ---------------------------------------------------------------- Molang -> numero
def ev(expr, t):
    s = str(expr).strip()
    if s.lower() == 'nan' or s == '': return 0.0
    s = re.sub(r'(?i)\bnan\b', '0', s)
    s = re.sub(r'(?i)query\.(anim_time|life_time)', str(t), s)
    s = re.sub(r'(?i)query\.\w+', '0', s)
    s = re.sub(r'(?i)(variable|v|temp|t)\.\w+', '0', s)
    s = re.sub(r'\bthis\b', '0', s)
    s = re.sub(r'(?i)\bmath\.', '', s)
    s = s.replace('&&', ' and ').replace('||', ' or ')
    m = re.match(r'^(.*?)\?(.*?):(.*)$', s)
    if m: s = f'(({m.group(2)}) if ({m.group(1)}) else ({m.group(3)}))'
    ns = dict(sin=lambda x: math.sin(math.radians(x)), cos=lambda x: math.cos(math.radians(x)), abs=abs, min=min, max=max,
              sqrt=math.sqrt, pow=pow, floor=math.floor, ceil=math.ceil, round=round, pi=math.pi,
              clamp=lambda x, a, b: max(a, min(b, x)), lerp=lambda a, b, k: a + (b - a) * k, random=lambda a=0, b=1: (a + b) / 2)
    try:
        return float(eval(s, {'__builtins__': {}}, ns))
    except Exception:
        return 0.0

def clean(v, t=0.0):
    if isinstance(v, str): return round(ev(v, t), 5)
    if isinstance(v, list): return [clean(x, t) for x in v]
    if isinstance(v, dict):
        out = {}
        for k, x in v.items():
            try: tt = float(k)
            except ValueError: tt = t
            out[k] = clean(x, tt)
        return out
    return v

def load_clip(rel, name):
    d = json.load(open(os.path.join(SRC, rel), encoding='utf-8'))['animations']
    return clean(d[name])

# ---------------------------------------------------------------- importar
info = {}
for mid, (geo, tex, mer, scale, clips) in MOBS.items():
    g = json.load(open(os.path.join(SRC, geo), encoding='utf-8'))
    geom = g['minecraft:geometry'][0]
    bones = {b['name'] for b in geom['bones']}
    os.makedirs(f'{OUT}/geo/entity', exist_ok=True)
    json.dump(g, open(f'{OUT}/geo/entity/{mid}.geo.json', 'w'), indent=1)
    # textura + mascara de brillo
    im = Image.open(os.path.join(SRC, f'textures/entity/pamobile/{tex}.png')).convert('RGBA')
    os.makedirs(f'{OUT}/textures/entity', exist_ok=True)
    im.save(f'{OUT}/textures/entity/{mid}.png')
    gm = Image.new('RGBA', im.size, (0, 0, 0, 0))
    mp = os.path.join(SRC, f'textures/entity/pamobile/{mer}.png')
    if os.path.exists(mp):
        m = Image.open(mp).convert('RGBA').resize(im.size)
        px, mx, gx = im.load(), m.load(), gm.load()
        for y in range(im.size[1]):
            for x in range(im.size[0]):
                if mx[x, y][1] > 110 and px[x, y][3] > 0: gx[x, y] = px[x, y]
    gm.save(f'{OUT}/textures/entity/{mid}_glowmask.png')
    # animaciones
    anims, missing = {}, set()
    for role, (rel, clip) in clips.items():
        a = load_clip(rel, clip)
        a['bones'] = a.get('bones', {})
        missing |= set(a['bones']) - bones
        if role in ('idle', 'walk', 'run'): a['loop'] = True
        elif role == 'dead': a['loop'] = 'hold_on_last_frame'
        else: a.pop('loop', None)
        if 'animation_length' not in a: a['animation_length'] = 1.0
        anims[f'animation.halo.{mid}.{role}'] = a
    os.makedirs(f'{OUT}/animations/entity', exist_ok=True)
    json.dump({'format_version': '1.8.0', 'animations': anims}, open(f'{OUT}/animations/entity/{mid}.animation.json', 'w'), indent=1)
    # tamaño para hitbox
    ys = [c['origin'][1] + c['size'][1] for b in geom['bones'] for c in b.get('cubes', [])]
    xs = [v for b in geom['bones'] for c in b.get('cubes', []) for v in (c['origin'][0], c['origin'][0] + c['size'][0])]
    h = max(ys) / 16 * scale
    info[mid] = dict(scale=scale, height=round(h, 2), width=round(min(1.3, max(0.5, (max(xs) - min(xs)) / 16 * scale * 0.55)), 2))
    print(f'{mid:14s} {len(bones):3d} huesos  tex {im.size}  escala {scale}  alto {h:.2f}  sin hueso en modelo: {sorted(missing) or "-"}')
json.dump(info, open(os.path.join(ROOT, 'tools/mob_sizes.json'), 'w'), indent=1)
