#!/usr/bin/env python3
"""Genera los modelos 3D de las armas (item models JSON), su paleta de colores y la textura del proyectil.

Uso: python3 tools/gen_weapons.py          (requiere Pillow)
Escribe en src/main/resources/assets/halomobs/:
  textures/item/weapons.png      paleta 16x16 (16 muestras de 4x4 px)
  textures/entity/bolt.png       destello del proyectil (se tiñe con el color del arma en el juego)
  models/item/<arma>.json        modelo de cubos; ademas tools/weapon_boxes.json (lo usa preview.py)

Los modelos son de CAJAS propias (marcador de posición): el addon Bedrock guarda las armas originales en
su resource pack (attachables). Para usar esas, convierte cada geo del arma a models/item y quita estas.

Convención del item: el cañón apunta a +Y, la altura (miras) es +Z, el ancho es X; la empuñadura está en el origen.
En el mob, el hueso rightItem lo gira -90° en X, así +Y -> hacia delante del mob y +Z -> arriba.
"""
import json, os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(ROOT, 'src/main/resources/assets/halomobs')

PAL = ['steel', 'dark', 'purple', 'lpurple', 'blue', 'green', 'rust', 'drkrust', 'bone', 'pink', 'silver', 'black', 'gold', 'orange', 'white', 'teal']
RGB = {'steel': (0x5a, 0x60, 0x6c), 'dark': (0x2a, 0x2d, 0x34), 'purple': (0x6b, 0x4a, 0xa8), 'lpurple': (0x9a, 0x7b, 0xd6),
       'blue': (0x5e, 0xc8, 0xff), 'green': (0x7d, 0xff, 0x9a), 'rust': (0x8a, 0x4b, 0x2c), 'drkrust': (0x4d, 0x2a, 0x1a),
       'bone': (0xc9, 0xb7, 0x8a), 'pink': (0xff, 0x6a, 0xd5), 'silver': (0xaa, 0xb2, 0xbd), 'black': (0x15, 0x16, 0x1a),
       'gold': (0xd4, 0xa6, 0x3a), 'orange': (0xff, 0xa8, 0x3c), 'white': (0xe8, 0xf7, 0xff), 'teal': (0x2f, 0x7f, 0x8a)}

def b(x0, x1, y0, y1, z0, z1, c): return [x0, x1, y0, y1, z0, z1, c]

# cajas: [x0,x1, y0,y1 (largo, cañon +Y), z0,z1 (alto), color]
W = {
 'plasma_pistol': [b(-1,1,-1,1,-4,0,'dark'), b(-1.5,1.5,-1,6,0,3,'purple'), b(-1,1,6,8,0.5,2.5,'blue'), b(-0.5,0.5,0,5,3,4,'lpurple'), b(-1.6,-1.5,1,5,0.5,2.5,'blue'), b(1.5,1.6,1,5,0.5,2.5,'blue')],
 'plasma_rifle': [b(-1,1,-1,1,-4,0,'dark'), b(-1.5,1.5,-3,9,-1,3,'purple'), b(-0.6,0.6,3,13,1,2,'blue'), b(-2,-1,9,14,0,3,'lpurple'), b(1,2,9,14,0,3,'lpurple'),
                  b(-1,1,-7,-3,0,2,'purple'), b(-0.5,0.5,-2,8,3,4.2,'lpurple')],
 'needler': [b(-1,1,-1,1,-4,0,'dark'), b(-1.2,1.2,-2,8,-1,2.5,'purple'), b(-0.8,0.8,0,7,2.5,4.5,'pink'), b(-1,1,8,10,0,2,'steel'), b(-0.5,0.5,-2,0,-2,0,'lpurple')],
 'plasma_repeater': [b(-1,1,-1,1,-4,0,'dark'), b(-1.8,1.8,-3,10,-1,3,'steel'), b(1.8,2.6,1,8,0,2,'green'), b(-2.6,-1.8,1,8,0,2,'green'), b(-0.8,0.8,10,16,0.8,2.2,'purple'),
                     b(-1,1,-7,-3,0,2,'dark'), b(-0.6,0.6,0,8,3,4,'lpurple')],
 'carbine': [b(-1,1,-1,1,-4,0,'dark'), b(-1,1,-8,-2,0,2.5,'purple'), b(-1.2,1.2,-2,6,-1,3,'purple'), b(-0.7,0.7,6,17,0.8,2.2,'silver'),
             b(-0.7,0.7,0,5,3,4.5,'black'), b(-0.4,0.4,5,5.8,3.2,4.3,'white')],
 'beam_rifle': [b(-1,1,-1,1,-4,0,'dark'), b(-1.5,1.5,-4,8,-1,3.5,'teal'), b(-0.8,0.8,8,21,1,2.8,'steel'), b(-0.6,0.6,21,23,1.2,2.6,'green'),
                b(-0.7,0.7,0,6,3.5,5,'black'), b(-1,1,-8,-4,0,2.5,'teal')],
 'spiker': [b(-1,1,-1,1,-4,0,'dark'), b(-1.5,1.5,-3,9,-1,3.5,'rust'), b(-1,1,9,13,0,2.5,'drkrust'), b(-0.4,0.4,2,3,3.5,6,'bone'),
            b(-0.4,0.4,5,6,3.5,6,'bone'), b(-0.4,0.4,8,9,3.5,6,'bone'), b(-1,1,-6,-3,0,2,'drkrust')],
 'mauler': [b(-1,1,-1,1,-4,0,'dark'), b(-2,2,-2,8,-1,4,'rust'), b(-1.2,-0.1,8,10,0.4,1.8,'drkrust'), b(0.1,1.2,8,10,0.4,1.8,'drkrust'),
            b(-1.2,-0.1,8,10,2.2,3.6,'drkrust'), b(0.1,1.2,8,10,2.2,3.6,'drkrust'), b(-1,1,-6,-2,0,2,'drkrust')],
 'brute_shot': [b(-1,1,-1,1,-4,0,'dark'), b(-2,2,-3,9,-2,3,'rust'), b(-2.3,2.3,0,4,3,6,'drkrust'), b(-1,1,9,13,0,2,'steel'),
                b(-0.3,0.3,4,14,-3.2,-1.2,'silver'), b(-1,1,-6,-3,-1,1,'drkrust')],
 'energy_sword': [b(-1,1,-4,2,-1,1,'dark'), b(-3,3,2,3.6,-0.8,0.8,'gold'), b(-2,-1,3.6,15,-1,1,'blue'), b(1,2,3.6,15,-1,1,'blue'), b(-0.6,0.6,4,12,-0.6,0.6,'white'),
                  b(-1.6,-1.2,15,16.4,-0.6,0.6,'blue'), b(1.2,1.6,15,16.4,-0.6,0.6,'blue')],
}
NAMES = {'plasma_pistol': ('Plasma Pistol', 'Pistola de plasma'), 'plasma_rifle': ('Plasma Rifle', 'Rifle de plasma'),
         'needler': ('Needler', 'Needler'), 'plasma_repeater': ('Plasma Repeater', 'Repetidor de plasma'),
         'carbine': ('Covenant Carbine', 'Carabina Covenant'), 'beam_rifle': ('Beam Rifle', 'Rifle de haz'),
         'spiker': ('Spiker', 'Spiker'), 'mauler': ('Mauler', 'Mauler'), 'brute_shot': ('Brute Shot', 'Brute Shot'),
         'energy_sword': ('Energy Sword', 'Espada de energía')}

def swatch(c):
    i = PAL.index(c); return (i % 4) * 4, (i // 4) * 4

def main():
    os.makedirs(f'{A}/textures/item', exist_ok=True); os.makedirs(f'{A}/models/item', exist_ok=True)
    pal = Image.new('RGBA', (16, 16))
    for i, n in enumerate(PAL):
        for dx in range(4):
            for dy in range(4): pal.putpixel(((i % 4) * 4 + dx, (i // 4) * 4 + dy), RGB[n] + (255,))
    pal.save(f'{A}/textures/item/weapons.png')
    # destello del proyectil: disco con borde duro (el render usa cutout) -> se tiñe en el juego
    bolt = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d <= 7.2:
                v = 255 if d <= 3.2 else (225 if d <= 5.2 else 175)
                bolt.putpixel((x, y), (v, v, v, 255))
    bolt.save(f'{A}/textures/entity/bolt.png')
    for wid, boxes in W.items():
        els = []
        for x0, x1, y0, y1, z0, z1, c in boxes:
            u, v = swatch(c); uv = [u + 1, v + 1, u + 3, v + 3]
            els.append({'from': [x0 + 8, y0 + 8, z0 + 8], 'to': [x1 + 8, y1 + 8, z1 + 8],
                        'faces': {f: {'uv': uv, 'texture': '#0'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}})
        model = {'textures': {'0': 'halomobs:item/weapons', 'particle': 'halomobs:item/weapons'}, 'elements': els,
                 'display': {
                     'gui': {'rotation': [0, -90, -45], 'scale': [0.62, 0.62, 0.62]},
                     'fixed': {'rotation': [0, -90, -45], 'scale': [0.55, 0.55, 0.55]},
                     'ground': {'rotation': [-90, 0, 0], 'translation': [0, -2, 0], 'scale': [0.5, 0.5, 0.5]},
                     'thirdperson_righthand': {'rotation': [0, 180, 0], 'scale': [0.9, 0.9, 0.9]},
                     'firstperson_righthand': {'rotation': [0, 180, 0], 'translation': [0, 2, 4], 'scale': [0.8, 0.8, 0.8]}}}
        json.dump(model, open(f'{A}/models/item/{wid}.json', 'w'), indent=1)
    json.dump(W, open(f'{ROOT}/tools/weapon_boxes.json', 'w'), indent=1)
    json.dump({'rgb': RGB, 'pal': PAL}, open(f'{ROOT}/tools/weapon_palette.json', 'w'))
    for lang, idx, fn in (('en_us', 0, 'en_us'), ('es_mx', 1, 'es_mx')):
        p = f'{A}/lang/{fn}.json'; d = json.load(open(p))
        for wid, nm in NAMES.items(): d[f'item.halomobs.{wid}'] = nm[idx]
        d['entity.halomobs.bolt'] = 'Halo Projectile' if idx == 0 else 'Proyectil Halo'
        json.dump(d, open(p, 'w'), indent=1, ensure_ascii=False)
    print('ok', len(W), 'armas')

if __name__ == '__main__':
    main()
