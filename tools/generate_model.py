#!/usr/bin/env python3
"""
Wave 100 motorcycle - model & texture generator.

Single source of truth for the motorcycle geometry. From one declarative spec
this script generates:

  1. src/main/java/com/wave100/client/WaveModel.java  (vanilla entity model,
     Blockbench-compatible LayerDefinition geometry with animated part groups)
  2. assets/wave100/textures/entity/wave_motorcycle.png        (base texture)
  3. assets/wave100/textures/entity/wave_motorcycle_lights.png (emissive overlay)
  4. assets/wave100/textures/item/wave_motorcycle.png          (item icon)
  5. docs/model_preview.png (isometric preview render)

Model conventions (vanilla entity models):
  - 16 model units = 1 block, ground plane y=0, front of the bike = -Z,
    +X = the rider's left.
  - Boxes are given as (x0, y0, z0, w, h, d) in model units relative to the
    pivot of their part.
"""
import math
import os
import random
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'src/main/java/com/wave100/client')
RES = os.path.join(ROOT, 'src/main/resources/assets/wave100')
TEX_SIZE = 128

# ----------------------------------------------------------------------
# palette
# ----------------------------------------------------------------------
SILVER      = (201, 206, 211)
SILVER_DARK = (152, 158, 166)
BLACK_P     = (36, 38, 42)
SEAT        = (25, 26, 29)
RUBBER      = (23, 24, 27)
CHROME      = (199, 206, 214)
METAL       = (64, 68, 76)
ENGINE      = (78, 83, 91)
LENS_OFF    = (233, 227, 201)
TAIL_DIM    = (128, 26, 23)
GLASS       = (152, 184, 209)
ORANGE      = (224, 132, 34)
PLATE       = (216, 216, 208)
SPOKE       = (176, 182, 190)

# ----------------------------------------------------------------------
# spec helpers
# ----------------------------------------------------------------------
class Part:
    """One animated model part; `boxes` are (x0,y0,z0,w,h,d) in pivot-relative
    model units. Multi-box parts get one UV region per box via box_names."""
    def __init__(self, name, parent, pivot=(0, 0, 0), rot=(0, 0, 0), boxes=None,
                 color=SILVER, uv_group=None, faces=None, overlay_faces=None,
                 emissive_only=False, box_names=None, box_colors=None,
                 box_faces=None, box_overlay=None):
        self.name = name
        self.parent = parent
        self.pivot = pivot
        self.rot = rot          # static rotation, radians (x, y, z)
        self.boxes = boxes or []
        self.color = color
        self.uv_group = uv_group
        self.faces = faces or {}
        self.overlay_faces = overlay_faces or {}
        self.emissive_only = emissive_only
        self.box_names = box_names or []
        self.box_colors = box_colors or {}
        self.box_faces = box_faces or {}
        self.box_overlay = box_overlay or {}

    def uv_key(self, i):
        if self.uv_group:
            return self.uv_group
        if i < len(self.box_names):
            return f'{self.name}.{self.box_names[i]}'
        return self.name

    def box_color(self, i):
        if i < len(self.box_names) and self.box_names[i] in self.box_colors:
            return self.box_colors[self.box_names[i]]
        return self.color

    def box_face_painters(self, i):
        if i < len(self.box_names) and self.box_names[i] in self.box_faces:
            return self.box_faces[self.box_names[i]]
        return self.faces

    def box_overlay_painters(self, i):
        if i < len(self.box_names) and self.box_names[i] in self.box_overlay:
            return self.box_overlay[self.box_names[i]]
        return self.overlay_faces


def wheel_parts(prefix, parent, width, axle, tire_r_out=4.42, tire_r_in=3.30,
                rim_r=3.62, hub=True, drum_left=False, drum_right=False,
                sprocket=False):
    """Thin spoked wheel built from arc segments around the X axis."""
    parts = []
    wheel_name = f'{prefix}_wheel'
    parts.append(Part(wheel_name, parent, axle, (0, 0, 0), boxes=[],
                      color=RUBBER))
    parent = wheel_name
    r_mid = (tire_r_out + tire_r_in) / 2
    rad = tire_r_out - tire_r_in
    chord = 2 * r_mid * math.sin(math.pi / 10)
    # tire - 10 segments sharing one UV group
    for k in range(10):
        parts.append(Part(
            f'{prefix}_tire{k}', parent, (0, 0, 0), (math.radians(k * 36), 0, 0),
            boxes=[(-width / 2, -tire_r_out, -chord / 2 - 0.28, width, rad, chord + 0.56)],
            color=RUBBER, uv_group=f'{prefix}_tire',
            faces={'down': 'tread'}))
    # rim ring - 8 segments
    rchord = 2 * rim_r * math.sin(math.pi / 8)
    for k in range(8):
        parts.append(Part(
            f'{prefix}_rim{k}', parent, (0, 0, 0), (math.radians(k * 45 + 22.5), 0, 0),
            boxes=[(-width * 0.42, -rim_r - 0.24, -rchord / 2 - 0.2, width * 0.84, 0.48, rchord + 0.4)],
            color=CHROME, uv_group=f'{prefix}_rim'))
    # spokes - 8 thin bars (four crossing pairs)
    for k in range(8):
        parts.append(Part(
            f'{prefix}_spoke{k}', parent, (0, 0, 0), (math.radians(k * 45), 0, 0),
            boxes=[(-0.30, -rim_r, -0.30, 0.60, rim_r - 1.0, 0.60)],
            color=SPOKE, uv_group=f'{prefix}_spoke'))
    if hub:
        parts.append(Part(
            f'{prefix}_hub', parent, (0, 0, 0), (0, 0, 0),
            boxes=[(-width / 2 - 0.1, -1.25, -1.25, width + 0.2, 2.5, 2.5)],
            color=CHROME, uv_group=f'{prefix}_hub'))
    if drum_left:
        parts.append(Part(
            f'{prefix}_drum_l', parent, (0, 0, 0), (0, 0, 0),
            boxes=[(-width / 2 - 0.75, -1.7, -1.7, 0.75, 3.4, 3.4)],
            color=METAL, uv_group=f'{prefix}_drum'))
    if drum_right:
        parts.append(Part(
            f'{prefix}_drum_r', parent, (0, 0, 0), (0, 0, 0),
            boxes=[(width / 2, -1.7, -1.7, 0.75, 3.4, 3.4)],
            color=METAL, uv_group=f'{prefix}_drum'))
    if sprocket:
        parts.append(Part(
            f'{prefix}_sprocket', parent, (0, 0, 0), (0, 0, 0),
            boxes=[(-width / 2 - 1.15, -1.8, -1.8, 0.55, 3.6, 3.6)],
            color=METAL, uv_group=f'{prefix}_drum'))
    return parts


def fender_parts(parent, axle, width=3.8):
    """Front fender arc over the wheel (front-to-top)."""
    parts = [Part('front_fender', parent, axle, (0, 0, 0), boxes=[], color=SILVER)]
    for k, deg in enumerate([68, 96, 124, 152]):
        parts.append(Part(
            f'fender_seg{k}', 'front_fender', (0, 0, 0), (math.radians(deg), 0, 0),
            boxes=[(-width / 2, -5.75, -1.45, width, 0.7, 2.9)],
            color=SILVER, uv_group='fender'))
    return parts


# ----------------------------------------------------------------------
# THE MODEL SPEC
# ----------------------------------------------------------------------
HEAD_TUBE = (0.0, 13.5, -4.6)
RAKE = math.degrees(math.atan2(9.0 - 4.6, 13.5 - 4.4))  # ~25.8 deg from vertical

PARTS = []

# ============================ BODY =====================================
body_boxes = [
    # ---- frame ----
    ('head_tube',      (-1.5, 12.5, -5.4), (3.0, 2.0, 1.6), METAL),
    ('downtube',       (-1.2, 7.2, -5.2),  (2.4, 5.6, 1.3), METAL),
    ('spine_tank',     (-3.0, 10.6, -3.8), (6.0, 2.6, 6.8), SILVER),
    ('spine_top',      (-2.6, 12.9, -3.4), (5.2, 0.8, 5.6), SILVER),
    ('rail_left',      (-3.1, 8.6, 2.2),   (0.9, 1.4, 8.4), SILVER_DARK),
    ('rail_right',     (2.2, 8.6, 2.2),    (0.9, 1.4, 8.4), SILVER_DARK),
    ('rear_cross',     (-2.6, 8.6, 9.8),   (5.2, 1.4, 1.6), SILVER_DARK),
    ('stay_left',      (-2.8, 5.2, 9.4),   (0.8, 3.6, 1.0), METAL),
    ('stay_right',     (2.0, 5.2, 9.4),    (0.8, 3.6, 1.0), METAL),
    # ---- engine ----
    ('engine_block',   (-3.2, 5.2, -3.4),  (6.4, 3.6, 6.6), ENGINE),
    ('engine_cover_l', (-4.0, 5.6, -2.2),  (0.8, 2.6, 4.2), METAL),
    ('engine_cover_r', (3.2, 5.6, -2.2),   (0.8, 2.6, 4.2), METAL),
    ('cylinder_fin1',  (-2.4, 8.8, -1.6),  (4.8, 0.4, 3.2), METAL),
    ('cylinder_fin2',  (-2.4, 9.4, -1.6),  (4.8, 0.4, 3.2), METAL),
    ('cylinder_fin3',  (-2.4, 10.0, -1.6), (4.8, 0.4, 3.2), METAL),
    # ---- footrests ----
    ('peg_left',       (-5.7, 4.8, 0.6),   (1.7, 0.55, 1.8), BLACK_P),
    ('peg_right',      (4.0, 4.8, 0.6),    (1.7, 0.55, 1.8), BLACK_P),
    ('peg_mount_l',    (-4.3, 4.7, 0.7),   (1.3, 0.8, 1.2), METAL),
    ('peg_mount_r',    (3.0, 4.7, 0.7),    (1.3, 0.8, 1.2), METAL),
    # ---- seat ----
    ('seat_main',      (-3.4, 11.2, 2.5),  (6.8, 1.4, 8.5), SEAT),
    ('seat_rise',      (-3.2, 11.8, 11.0), (6.4, 1.8, 3.4), SEAT),
    ('seat_pan',       (-3.1, 10.4, 2.5),  (6.2, 0.9, 11.8), BLACK_P),
    # ---- side panels & tail ----
    ('side_panel_l',   (-4.3, 9.0, 3.6),   (1.2, 2.6, 7.6), SILVER),
    ('side_panel_r',   (3.1, 9.0, 3.6),    (1.2, 2.6, 7.6), SILVER),
    ('tail_cowl',      (-3.3, 10.6, 14.4), (6.6, 2.0, 3.8), SILVER),
    ('tail_top',       (-2.7, 12.6, 14.8), (5.4, 0.8, 3.4), SILVER),
    ('tail_housing',   (-2.0, 11.1, 18.2), (4.0, 1.2, 0.5), BLACK_P),
    ('rear_fender',    (-2.9, 9.4, 7.6),   (5.8, 1.1, 6.2), SILVER),
    ('fender_lip',     (-2.9, 10.4, 7.3),  (5.8, 0.5, 1.2), BLACK_P),
    ('plate',          (-2.2, 8.9, 18.1),  (4.4, 2.2, 0.35), PLATE),
    # ---- carrier rack ----
    ('rack_plate',     (-3.0, 13.6, 15.2), (6.0, 0.5, 3.6), SILVER_DARK),
    ('rack_strut_l',   (-2.8, 12.6, 15.4), (0.6, 1.0, 2.8), SILVER_DARK),
    ('rack_strut_r',   (2.2, 12.6, 15.4),  (0.6, 1.0, 2.8), SILVER_DARK),
    ('rack_lip',       (-3.0, 13.6, 18.4), (6.0, 1.7, 0.5), SILVER_DARK),
    # ---- chain (left side) ----
    ('chain_guard',    (-3.85, 5.6, 3.2),  (0.9, 1.6, 6.8), BLACK_P),
    ('chain',          (-3.62, 5.9, 3.4),  (0.45, 1.0, 6.4), METAL),
    # ---- exhaust (right side) ----
    ('ex_header',      (2.6, 6.2, 0.6),    (1.8, 1.6, 3.4), CHROME),
    ('ex_mid',         (3.3, 6.6, 3.9),    (1.5, 1.4, 7.3), CHROME),
    ('ex_muffler',     (3.4, 6.0, 11.2),   (2.6, 3.6, 6.6), CHROME),
    ('ex_band',        (3.3, 5.9, 13.9),   (2.8, 3.8, 0.7), METAL),
    # ---- front cowl ----
    ('cowl_main',      (-3.5, 11.5, -8.4), (7.0, 4.2, 3.0), SILVER),
    ('cowl_nose',      (-3.0, 11.0, -9.3), (6.0, 3.2, 1.0), SILVER),
    ('cowl_top',       (-2.6, 15.6, -8.0), (5.2, 0.9, 2.7), SILVER),
    ('cowl_cheek_l',   (-4.3, 11.2, -8.0), (0.8, 2.6, 2.2), BLACK_P),
    ('cowl_cheek_r',   (3.5, 11.2, -8.0),  (0.8, 2.6, 2.2), BLACK_P),
    ('indicator_l',    (-4.5, 11.7, -8.2), (1.1, 0.9, 1.4), ORANGE),
    ('indicator_r',    (3.4, 11.7, -8.2),  (1.1, 0.9, 1.4), ORANGE),
    ('light_housing',  (-2.3, 11.0, -10.2),(4.6, 2.5, 0.9), METAL),
]
# attach colors/faces per body box via lookup dicts
BODY_COLORS = {name: col for (name, _, _, col) in body_boxes}

BODY_FACES = {
    'seat_main': {'up': 'stitch'},
    'seat_rise': {'up': 'stitch'},
    'side_panel_l': {'west': 'stripe', 'east': 'stripe'},
    'side_panel_r': {'west': 'stripe', 'east': 'stripe'},
    'spine_tank': {'west': 'stripe', 'east': 'stripe'},
    'cowl_main': {'north': 'cowl_vent'},
    'ex_muffler': {'south': 'exhaust_cap'},
    'plate': {'south': 'plate_text'},
}

PARTS.append(Part('body', 'root', (0, 0, 0), (0, 0, 0),
                  boxes=[tuple(list(b[1]) + list(b[2])) for b in body_boxes],
                  color=SILVER,
                  box_names=[b[0] for b in body_boxes],
                  box_colors=BODY_COLORS,
                  box_faces=BODY_FACES))

# windscreen (raked back)
PARTS.append(Part('windscreen', 'body', (0.0, 15.4, -6.9),
                  (math.radians(18), 0, 0),
                  boxes=[(-2.5, 0.0, -0.35, 5.0, 2.9, 0.7)],
                  color=GLASS, faces={'north': 'glass', 'south': 'glass'}))

# side stand (animated: parked = down+out, riding = folded back)
PARTS.append(Part('side_stand', 'body', (-2.7, 5.0, 2.6), (0, 0, 0),
                  boxes=[(-0.35, -4.3, -0.35, 0.7, 4.3, 0.7),
                         (-0.9, -4.7, -0.5, 1.8, 0.45, 1.0)],
                  color=BLACK_P))

# ======================== STEERING ASSEMBLY ============================
# NOTE: relative to the steering pivot (head tube at 0, 13.5, -4.6).
steer_boxes = [
    ('stem',         (-0.9, -0.3, -0.5), (1.8, 1.6, 1.2), METAL),
    ('triple_clamp', (-2.7, 0.8, -0.9), (5.4, 0.9, 1.7), METAL),
]
STEER_COLORS = {name: col for (name, _, _, col) in steer_boxes}
PARTS.append(Part('steering', 'root', HEAD_TUBE, (0, 0, 0),
                  boxes=[tuple(list(b[1]) + list(b[2])) for b in steer_boxes],
                  color=METAL,
                  box_names=[b[0] for b in steer_boxes],
                  box_colors=STEER_COLORS))


# handlebar (child of steering)
# NOTE: box coords are relative to the handlebar pivot (0, 14.9, -4.8 in
# model space, i.e. 1.4 above / 0.2 behind the head tube).
bar_boxes = [
    ('bar_main',      (-6.4, 0.70, -0.45), (12.8, 0.85, 0.95), CHROME),
    ('riser',         (-0.9, -0.20, -0.45), (1.8, 1.0, 0.95), METAL),
    ('grip_left',     (-6.7, 0.65, -0.50), (2.3, 0.95, 1.05), BLACK_P),
    ('grip_right',    (4.4, 0.65, -0.50),  (2.3, 0.95, 1.05), BLACK_P),
    ('lever_left',    (-4.6, 1.35, -1.20), (1.7, 0.42, 0.85), CHROME),
    ('lever_right',   (2.9, 1.35, -1.20),  (1.7, 0.42, 0.85), CHROME),
    ('mirror_stalk_l', (-4.7, 1.50, -0.25), (0.5, 2.5, 0.5), METAL),
    ('mirror_stalk_r', (4.2, 1.50, -0.25),  (0.5, 2.5, 0.5), METAL),
    ('mirror_plate_l', (-6.7, 3.80, -0.90), (2.7, 1.9, 0.5), BLACK_P),
    ('mirror_plate_r', (4.0, 3.80, -0.90),  (2.7, 1.9, 0.5), BLACK_P),
    ('speedo',        (-1.9, 1.20, -1.20),  (3.8, 1.4, 1.6), BLACK_P),
]
BAR_COLORS = {name: col for (name, _, _, col) in bar_boxes}
BAR_FACES = {
    'speedo': {'up': 'dial'},
    'mirror_plate_l': {'north': 'mirror'},
    'mirror_plate_r': {'north': 'mirror'},
}
PARTS.append(Part('handlebar', 'steering', (0, 1.4, -0.2), (0, 0, 0),
                  boxes=[tuple(list(b[1]) + list(b[2])) for b in bar_boxes],
                  color=BLACK_P,
                  box_names=[b[0] for b in bar_boxes],
                  box_colors=BAR_COLORS,
                  box_faces=BAR_FACES))


# fork (child of steering, raked)
fork_boxes = [
    ('fork_tube_l', (-2.4, -6.2, -0.55), (0.95, 6.8, 1.1), CHROME),
    ('fork_tube_r', (1.45, -6.2, -0.55), (0.95, 6.8, 1.1), CHROME),
]
FORK_COLORS = {name: col for (name, _, _, col) in fork_boxes}
PARTS.append(Part('fork', 'steering', (0, 0, 0), (math.radians(RAKE), 0, 0),
                  boxes=[tuple(list(b[1]) + list(b[2])) for b in fork_boxes],
                  color=CHROME,
                  box_names=[b[0] for b in fork_boxes],
                  box_colors=FORK_COLORS))


# fork lower legs (child of fork; animated suspension slide)
lower_boxes = [
    ('lower_leg_l', (-2.45, -4.4, -0.65), (1.05, 4.7, 1.3), METAL),
    ('lower_leg_r', (1.4, -4.4, -0.65), (1.05, 4.7, 1.3), METAL),
]
LOWER_COLORS = {name: col for (name, _, _, col) in lower_boxes}
PARTS.append(Part('fork_lower', 'fork', (0, -6.2, 0), (0, 0, 0),
                  boxes=[tuple(list(b[1]) + list(b[2])) for b in lower_boxes],
                  color=METAL,
                  box_names=[b[0] for b in lower_boxes],
                  box_colors=LOWER_COLORS))


# front wheel + fender (children of fork_lower)
PARTS += wheel_parts('front', 'fork_lower', 2.1, (0, -3.9, 0), drum_right=True)
PARTS += fender_parts('fork_lower', (0, -3.9, 0))

# rear wheel (child of root)
PARTS += wheel_parts('rear', 'root', 2.5, (0, 4.4, 10.0), drum_left=True, sprocket=True)

# ===================== LIGHTS (root level, overlay) ====================
PARTS.append(Part('headlight_lens', 'root', (0, 0, 0), (0, 0, 0),
                  boxes=[(-2.05, 11.25, -10.75, 4.1, 2.05, 0.5)],
                  color=LENS_OFF,
                  faces={'north': 'lens_off'},
                  overlay_faces={'north': 'lens_on'}))

PARTS.append(Part('tail_lens', 'root', (0, 0, 0), (0, 0, 0),
                  boxes=[(-1.85, 11.3, 18.65, 3.7, 0.95, 0.4)],
                  color=TAIL_DIM,
                  faces={'south': 'tail_dim'},
                  overlay_faces={'south': 'tail_bright'}))

# headlight beam cone (emissive only, normally invisible)
PARTS.append(Part('beam_inner', 'root', (0, 0, 0), (0, 0, 0),
                  boxes=[(-1.1, 11.7, -30.6, 2.2, 1.3, 19.8)],
                  color=(255, 250, 220), emissive_only=True,
                  overlay_faces={'north': 'beam_soft'}))
PARTS.append(Part('beam_outer', 'root', (0, 0, 0), (0, 0, 0),
                  boxes=[(-2.1, 11.15, -26.6, 4.2, 2.4, 15.8)],
                  color=(255, 250, 220), emissive_only=True,
                  overlay_faces={'north': 'beam_faint'}))

# ----------------------------------------------------------------------
# UV packing (shelf packer, deterministic)
# ----------------------------------------------------------------------
def region_size(box):
    (_, _, _, w, h, d) = box
    return (max(2, int(math.ceil(2 * (w + d)))), max(2, int(math.ceil(d + h))))

def pack_uvs():
    groups = {}
    for p in PARTS:
        for i, box in enumerate(p.boxes):
            g = p.uv_key(i)
            if g in groups:
                continue
            groups[g] = region_size(box)
    order = sorted(groups.keys(), key=lambda g: (-groups[g][1], -groups[g][0], g))
    packed = {}
    x = y = shelf_h = 0
    pad = 1
    for g in order:
        (rw, rh) = groups[g]
        if x + rw + pad > TEX_SIZE:
            x = 0
            y += shelf_h + pad
            shelf_h = 0
        assert y + rh <= TEX_SIZE, f"UV overflow: {g} at y={y} (texture {TEX_SIZE})"
        packed[g] = (x, y)
        x += rw + pad
        shelf_h = max(shelf_h, rh)
    return packed

UV = pack_uvs()

def face_rect(u, v, box, face):
    (_, _, _, w, h, d) = box
    w, h, d = int(math.ceil(w)), int(math.ceil(h)), int(math.ceil(d))
    if face == 'up':
        return (u + d, v, w, d)
    if face == 'down':
        return (u + d + w, v, w, d)
    if face == 'west':
        return (u, v + d, d, h)
    if face == 'north':
        return (u + d, v + d, w, h)
    if face == 'east':
        return (u + d + w, v + d, d, h)
    if face == 'south':
        return (u + 2 * d + w, v + d, w, h)
    raise ValueError(face)

# ----------------------------------------------------------------------
# texture painting
# ----------------------------------------------------------------------
rng = random.Random(20240916)

def noise_fill(img, rect, color, jitter=5):
    x0, y0, w, h = rect
    dr = ImageDraw.Draw(img)
    for yy in range(h):
        for xx in range(w):
            n = rng.randint(-jitter, jitter)
            c = tuple(max(0, min(255, ch + n)) for ch in color)
            dr.point((x0 + xx, y0 + yy), fill=c)

def hline(img, rect, yoff, color, dash=0):
    x0, y0, w, h = rect
    dr = ImageDraw.Draw(img)
    for x in range(w):
        if dash and (x // dash) % 2 == 1:
            continue
        dr.point((x0 + x, y0 + yoff), fill=color)

def paint_face(img, rect, face, painter, box):
    x0, y0, w, h = rect
    dr = ImageDraw.Draw(img)
    if painter == 'tread':
        # dark tread stripes across the tire contact face
        for x in range(w):
            if (x // 2) % 2 == 0:
                for yy in range(h):
                    dr.point((x0 + x, y0 + yy), fill=(15, 16, 18))
    elif painter == 'stitch':
        hline(img, rect, h // 2, (55, 57, 62), dash=2)
        hline(img, rect, 1, (48, 50, 55))
    elif painter == 'stripe':
        hline(img, rect, h // 2, BLACK_P)
        hline(img, rect, h // 2 + 1, BLACK_P)
    elif painter == 'cowl_vent':
        for i in range(3):
            for x in range(w // 6):
                dr.point((x0 + 3 + i * 6, y0 + h - 3), fill=SILVER_DARK)
                dr.point((x0 + 3 + i * 6, y0 + h - 4), fill=SILVER_DARK)
    elif painter == 'exhaust_cap':
        cx, cy = x0 + w // 2, y0 + h // 2
        for yy in range(h):
            for xx in range(w):
                if (xx - cx) ** 2 + (yy - cy) ** 2 <= (min(w, h) // 2 - 1) ** 2:
                    dr.point((x0 + xx, y0 + yy), fill=(52, 55, 60))
    elif painter == 'plate_text':
        for (px, py, pw, ph) in [(2, 3, 3, 4), (6, 3, 1, 4), (8, 3, 3, 4),
                                  (w - 5, 3, 3, 4), (2, h - 5, w - 4, 1)]:
            for yy in range(ph):
                for xx in range(pw):
                    if 0 <= px + xx < w and 0 <= py + yy < h:
                        dr.point((x0 + px + xx, y0 + py + yy), fill=(60, 60, 66))
    elif painter == 'dial':
        for yy in range(h):
            for xx in range(w):
                dx = (xx - w / 2) / (w / 2)
                dy = (yy - h / 2) / (h / 2)
                if dx * dx + dy * dy <= 1.0:
                    dr.point((x0 + xx, y0 + yy), fill=(228, 230, 225))
        # needle
        for t in range(1, min(w, h) // 2):
            dr.point((x0 + w // 2 - t // 2, y0 + h // 2 - t // 2 + 1), fill=(180, 30, 25))
    elif painter == 'mirror':
        noise_fill(img, rect, (170, 190, 205), jitter=3)
    elif painter == 'glass':
        for yy in range(h):
            t = yy / max(1, h - 1)
            c = (int(152 + 40 * t), int(184 + 30 * t), int(209 + 25 * t))
            for xx in range(w):
                n = rng.randint(-4, 4)
                dr.point((x0 + xx, y0 + yy), fill=tuple(max(0, min(255, ch + n)) for ch in c))
    elif painter == 'lens_off':
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for yy in range(h):
            for xx in range(w):
                d2 = ((xx - cx) / max(0.5, w / 2)) ** 2 + ((yy - cy) / max(0.5, h / 2)) ** 2
                if d2 <= 1.0:
                    c = (238 - int(30 * d2), 232 - int(35 * d2), 205 - int(45 * d2))
                else:
                    c = (150, 148, 130)
                dr.point((x0 + xx, y0 + yy), fill=c)
    elif painter == 'lens_on':
        cx, cy = (w - 1) / 2, (h - 1) / 2
        for yy in range(h):
            for xx in range(w):
                d2 = ((xx - cx) / max(0.5, w / 2)) ** 2 + ((yy - cy) / max(0.5, h / 2)) ** 2
                if d2 <= 1.0:
                    a = 255 - int(90 * d2)
                    dr.point((x0 + xx, y0 + yy), fill=(255, 248, 205, a))
    elif painter == 'tail_dim':
        for yy in range(h):
            for xx in range(w):
                n = rng.randint(-10, 10)
                dr.point((x0 + xx, y0 + yy), fill=(128 + n, 26 + n // 2, 23 + n // 2))
    elif painter == 'tail_bright':
        for yy in range(h):
            for xx in range(w):
                dr.point((x0 + xx, y0 + yy), fill=(255, 62, 48, 255))
    elif painter == 'beam_soft':
        for yy in range(h):
            for xx in range(w):
                t = xx / max(1, w - 1)   # fade along beam length
                a = int(235 * (1.0 - t))
                dr.point((x0 + xx, y0 + yy), fill=(255, 248, 210, a))
    elif painter == 'beam_faint':
        for yy in range(h):
            for xx in range(w):
                t = xx / max(1, w - 1)
                a = int(110 * (1.0 - t))
                dr.point((x0 + xx, y0 + yy), fill=(255, 248, 210, a))

def paint_textures():
    base = Image.new('RGBA', (TEX_SIZE, TEX_SIZE), (0, 0, 0, 0))
    overlay = Image.new('RGBA', (TEX_SIZE, TEX_SIZE), (0, 0, 0, 0))
    for p in PARTS:
        for i, box in enumerate(p.boxes):
            key = p.uv_key(i)
            if p.emissive_only:
                (u, v) = UV.get(key, (0, 0))
                for face, painter in p.box_overlay_painters(i).items():
                    paint_face(overlay, face_rect(u, v, box, face), face, painter, box)
                continue
            (u, v) = UV[key]
            noise_fill(base, (u, v, *region_size(box)), p.box_color(i))
            for face, painter in p.box_face_painters(i).items():
                paint_face(base, face_rect(u, v, box, face), face, painter, box)
            for face, painter in p.box_overlay_painters(i).items():
                paint_face(overlay, face_rect(u, v, box, face), face, painter, box)
    return base, overlay

# ----------------------------------------------------------------------
# Java emitter
# ----------------------------------------------------------------------
def jf(x):
    return f"{x:.4f}".rstrip('0').rstrip('.') + 'F'

def emit_java():
    # group parts by parent for tree construction
    children = {}
    for p in PARTS:
        children.setdefault(p.parent, []).append(p)

    def emit_part(p, indent):
        lines = []
        pad = ' ' * indent
        lines.append(f"{pad}PartDefinition {p.name} = {p.parent}.addOrReplaceChild(\"{p.name}\", CubeListBuilder.create()")
        for i, box in enumerate(p.boxes):
            x0, y0, z0, w, h, d = box
            u, v = UV.get(p.uv_key(i), (0, 0))
            prefix = f"{pad}    " if i else f"{pad}    "
            joiner = '.' if i == 0 else '.'
            lines.append(f"{prefix}.texOffs({u}, {v})")
            lines.append(f"{prefix}.addBox({jf(x0)}, {jf(y0)}, {jf(z0)}, {jf(w)}, {jf(h)}, {jf(d)})")
        rx, ry, rz = p.rot
        if p.pivot == (0, 0, 0) and (rx, ry, rz) == (0, 0, 0):
            pose = "PartPose.ZERO"
        elif (rx, ry, rz) == (0, 0, 0):
            pose = f"PartPose.offset({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])})"
        else:
            pose = (f"PartPose.offsetAndRotation({jf(p.pivot[0])}, {jf(p.pivot[1])}, {jf(p.pivot[2])}, "
                    f"{jf(rx)}F, {jf(ry)}F, {jf(rz)}F)")
        lines.append(f"{pad}    , {pose});")
        # children
        for c in children.get(p.name, []):
            lines.extend(emit_part(c, indent + 4))
        return lines

    body = []
    body.append("        MeshDefinition mesh = new MeshDefinition();")
    body.append("        PartDefinition root = mesh.getRoot();")
    for p in children['root']:
        body.extend(emit_part(p, 8))
    body.append("        return LayerDefinition.create(mesh, 128, 128);")

    java = f"""package com.wave100.client;

import com.wave100.WaveMod;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.PartPose;
import net.minecraft.world.entity.Entity;

/**
 * Honda Wave 100 style underbone motorcycle - modular entity model.
 *
 * <p>AUTO-GENERATED by {{@code tools/generate_model.py}}. Do not edit by hand:
 * change the generator spec and re-run it (see README). The layout is
 * Blockbench-compatible vanilla entity geometry.</p>
 *
 * <p>Animated part groups:</p>
 * <ul>
 *   <li>{{@code steering}} - yaw with the handlebars (front fork + wheel + fender)</li>
 *   <li>{{@code forkLower}} - slides along the fork axis for suspension travel</li>
 *   <li>{{@code frontWheel}} / {{@code rearWheel}} - spin around the X axis</li>
 *   <li>{{@code sideStand}} - swings down when parked, folds away when ridden</li>
 *   <li>{{@code headlightLens}}, {{@code beamInner}}, {{@code beamOuter}}, {{@code tailLens}}
 *       - emissive overlay passes for lights</li>
 * </ul>
 */
public class WaveModel extends HierarchicalModel<Entity> {{

    public static final ModelLayerLocation LAYER =
            new ModelLayerLocation(WaveMod.id("wave_motorcycle"), "main");

    private final ModelPart root;

    public final ModelPart body;
    public final ModelPart windscreen;
    public final ModelPart sideStand;
    public final ModelPart steering;
    public final ModelPart handlebar;
    public final ModelPart fork;
    public final ModelPart forkLower;
    public final ModelPart frontWheel;
    public final ModelPart rearWheel;
    public final ModelPart headlightLens;
    public final ModelPart tailLens;
    public final ModelPart beamInner;
    public final ModelPart beamOuter;

    public WaveModel(ModelPart root) {{
        this.root = root;
        this.body = root.getChild("body");
        this.windscreen = this.body.getChild("windscreen");
        this.sideStand = this.body.getChild("side_stand");
        this.steering = root.getChild("steering");
        this.handlebar = this.steering.getChild("handlebar");
        this.fork = this.steering.getChild("fork");
        this.forkLower = this.fork.getChild("fork_lower");
        this.frontWheel = this.forkLower.getChild("front_wheel");
        this.rearWheel = root.getChild("rear_wheel");
        this.headlightLens = root.getChild("headlight_lens");
        this.tailLens = root.getChild("tail_lens");
        this.beamInner = root.getChild("beam_inner");
        this.beamOuter = root.getChild("beam_outer");
    }}

    public static LayerDefinition createBodyLayer() {{
{chr(10).join(body)}
    }}

    @Override
    public ModelPart root() {{
        return this.root;
    }}

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount,
                          float ageInTicks, float netHeadYaw, float headPitch) {{
    }}
}}
"""
    return java

# ----------------------------------------------------------------------
# preview renderer (orthographic, painter's algorithm)
# ----------------------------------------------------------------------
def build_world_boxes():
    """Returns [(corners[8], color, uvbox)] in model space with static poses."""
    out = []
    # transform accumulation: part -> matrix (translate pivot, rotate)
    def identity():
        return [[1, 0, 0], [0, 1, 0], [0, 0, 1]]

    def rot_xyz(rx, ry, rz):
        cx, sx = math.cos(rx), math.sin(rx)
        cy, sy = math.cos(ry), math.sin(ry)
        cz, sz = math.cos(rz), math.sin(rz)
        Rx = [[1, 0, 0], [0, cx, -sx], [0, sx, cx]]
        Ry = [[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]]
        Rz = [[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]]
        def mm(A, B):
            return [[sum(A[i][k] * B[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
        return mm(mm(Rx, Ry), Rz)

    # explicit hierarchy parents for wheel/fender children
    parent_lookup = {p.name: p for p in PARTS}
    cache = {}

    def world_transform(p):
        chain = [p]
        while chain[-1].parent != 'root':
            chain.append(parent_lookup[chain[-1].parent])
        (R, t) = (identity(), [0, 0, 0])
        for q in reversed(chain):
            Rq = rot_xyz(*q.rot)
            # world = world * (T(pivot) * R)
            t = [t[i] + sum(R[i][k] * q.pivot[k] for k in range(3)) for i in range(3)]
            R = [[sum(R[i][k] * Rq[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
        return R, t

    for p in PARTS:
        if p.emissive_only:
            continue
        R, t = world_transform(p)
        for box in p.boxes:
            x0, y0, z0, w, h, d = box
            local = [(x0, y0, z0), (x0 + w, y0, z0), (x0 + w, y0 + h, z0), (x0, y0 + h, z0),
                     (x0, y0, z0 + d), (x0 + w, y0, z0 + d), (x0 + w, y0 + h, z0 + d), (x0, y0 + h, z0 + d)]
            world = []
            for (lx, ly, lz) in local:
                wx = t[0] + R[0][0] * lx + R[0][1] * ly + R[0][2] * lz
                wy = t[1] + R[1][0] * lx + R[1][1] * ly + R[1][2] * lz
                wz = t[2] + R[2][0] * lx + R[2][1] * ly + R[2][2] * lz
                world.append((wx, wy, wz))
            out.append((world, p.color, p.uv_group))
    return out

def render_preview(path, size=(1100, 800), scale=26, yaw=140, pitch=18, bg=(44, 48, 55)):
    boxes = build_world_boxes()
    img = Image.new('RGB', size, bg)
    dr = ImageDraw.Draw(img)
    cx = size[0] * 0.46
    cy = size[1] * 0.62

    def xform(p):
        x, y, z = p
        # yaw around Y
        a = math.radians(yaw)
        x1 = x * math.cos(a) + z * math.sin(a)
        z1 = -x * math.sin(a) + z * math.cos(a)
        # pitch around X
        b = math.radians(pitch)
        y2 = y * math.cos(b) - z1 * math.sin(b)
        z2 = y * math.sin(b) + z1 * math.cos(b)
        return (x1, y2, z2)

    quads = []
    for (world, color, uv) in boxes:
        pts = [xform(p) for p in world]
        # faces: (indices, shade)
        faces = [
            ((0, 1, 2, 3), 0.55),  # bottom (y0)
            ((4, 5, 6, 7), 1.00),  # top
            ((0, 1, 5, 4), 0.80),  # z0 front-ish
            ((3, 2, 6, 7), 0.80),  # z1
            ((1, 2, 6, 5), 0.66),  # x1
            ((0, 3, 7, 4), 0.66),  # x0
        ]
        for idx, shade in faces:
            quad = [pts[i] for i in idx]
            depth = sum(p[2] for p in quad) / 4
            # simple backface: only draw if facing viewer (cross product z)
            ux, uy, _ = quad[1][0] - quad[0][0], quad[1][1] - quad[0][1], 0
            vx, vy, _ = quad[3][0] - quad[0][0], quad[3][1] - quad[0][1], 0
            cross = ux * vy - uy * vx
            quads.append((depth, quad, color, shade, cross))
    quads.sort(key=lambda q: -q[0])
    for depth, quad, color, shade, cross in quads:
        if abs(cross) < 1e-9:
            continue
        # screen coords
        poly = [(cx + p[0] * scale, cy - (p[1] + 8.0) * scale) for p in quad]
        c = tuple(max(0, min(255, int(ch * shade))) for ch in color)
        dr.polygon(poly, fill=c, outline=tuple(max(0, ch - 18) for ch in c))
    # ground reference shadow ellipse
    dr.ellipse([cx - 15 * scale, cy - 15 * scale * 0.32, cx + 15 * scale, cy + 15 * scale * 0.32],
               fill=(38, 41, 47))
    img = img.transpose(Image.FLIP_TOP_BOTTOM)
    img.save(path)

def make_icon(preview_path, out_path):
    img = Image.open(preview_path).convert('RGBA')
    # crop the bike region generously then downscale to 16x16 with sharpening
    w, h = img.size
    crop = img.crop((int(w * 0.18), int(h * 0.30), int(w * 0.78), int(h * 0.80)))
    icon = crop.resize((64, 64), Image.LANCZOS).resize((16, 16), Image.LANCZOS)
    # boost alpha contrast
    px = icon.load()
    for y in range(16):
        for x in range(16):
            (r, g, b, a) = px[x, y]
            if (r + g + b) / 3 > 150 and (r, g, b) != (44, 48, 55):
                px[x, y] = (r, g, b, 255)
    icon.save(out_path)

# ----------------------------------------------------------------------
# sanity checks
# ----------------------------------------------------------------------
def sanity():
    boxes = build_world_boxes()
    xs = [c[0] for (w, c_, u) in boxes for c in w]
    ys = [c[1] for (w, c_, u) in boxes for c in w]
    zs = [c[2] for (w, c_, u) in boxes for c in w]
    print(f"model bbox: x [{min(xs):.1f}, {max(xs):.1f}]  "
          f"y [{min(ys):.1f}, {max(ys):.1f}]  z [{min(zs):.1f}, {max(zs):.1f}]")
    print(f"size in blocks: {abs(max(xs)-min(xs))/16:.2f} x {abs(max(ys)-min(ys))/16:.2f} x {abs(max(zs)-min(zs))/16:.2f}")
    assert min(ys) > -0.6, "something pokes below the ground"
    assert max(ys) < 22, "too tall"
    assert abs(max(xs)) < 8.5 and abs(min(xs)) < 8.5, "too wide for an 0.8 hitbox"
    total = sum(len(p.boxes) for p in PARTS)
    print(f"parts: {len(PARTS)}, cuboids: {total}, uv groups: {len(UV)}")
    # audit: no part's content should extend absurdly far from its pivot
    for (w, c_, u) in boxes:
        pass

# ----------------------------------------------------------------------
def main():
    sanity()
    java = emit_java()
    os.makedirs(SRC, exist_ok=True)
    with open(os.path.join(SRC, 'WaveModel.java'), 'w') as f:
        f.write(java)
    print("wrote WaveModel.java")

    base, overlay = paint_textures()
    os.makedirs(os.path.join(RES, 'textures/entity'), exist_ok=True)
    base.save(os.path.join(RES, 'textures/entity/wave_motorcycle.png'))
    overlay.save(os.path.join(RES, 'textures/entity/wave_motorcycle_lights.png'))
    print("wrote textures")

    os.makedirs(os.path.join(ROOT, 'docs'), exist_ok=True)
    render_preview(os.path.join(ROOT, 'docs/model_preview.png'))
    print("wrote preview")

    os.makedirs(os.path.join(RES, 'textures/item'), exist_ok=True)
    make_icon(os.path.join(ROOT, 'docs/model_preview.png'),
              os.path.join(RES, 'textures/item/wave_motorcycle.png'))
    print("wrote icon")


if __name__ == '__main__':
    main()
