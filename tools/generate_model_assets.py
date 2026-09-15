#!/usr/bin/env python3
"""
Generates the Wave Motorcycle resource-pack assets.

Outputs:
  src/main/resources/resourcepack/
    pack.mcmeta
    assets/minecraft/models/item/paper_<mdl>.json      (ItemDisplay parts, CMI lookup)
    assets/minecraft/models/item/nether_star_1001.json (the key item)
    assets/minecraft/textures/item/*.png
  tools/blockbench_src/                                (human-editable source)
    <part>.blockbench.json
    wavemotorcycle/textures/item/*.png

Run:  python3 tools/generate_model_assets.py

The server displays each part with an ItemDisplay holding PAPER with a custom
model data value; the client resolves the model at
  assets/minecraft/models/item/<item>_<custom_model_data>.json
so the pack models live in the minecraft namespace.

All geometry is authored in "chassis units": 1 unit = 1/16 block, +Z forward,
+Y up, ground at y=0. Part models are centered on their anchor point:
  body            anchor = chassis center (ground, middle of wheelbase)
  front_assembly  anchor = front wheel plane (z = +10.8 units)
  handlebar       anchor = front wheel plane
  headlight       anchor = front wheel plane
  rear_light      anchor = rear wheel plane (z = -10.8 units)
  front_wheel     anchor = front wheel axle (y = +5.3)
  rear_wheel      anchor = rear wheel axle (y = +5.3)
"""
import json
import math
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "resourcepack")
PACK_MODELS = os.path.join(ROOT, "assets", "minecraft", "models", "item")
PACK_TEXTURES = os.path.join(ROOT, "assets", "minecraft", "textures", "item")
SRC = os.path.join(os.path.dirname(__file__), "blockbench_src")
SRC_TEXTURES = os.path.join(SRC, "wavemotorcycle", "textures", "item")

# ---------------------------------------------------------------------------
# Texture generation (64x64, deterministic dithering)
# ---------------------------------------------------------------------------

def write_png(path, w, h, pixels):
    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        out += struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
        return out

    raw = b""
    for row in pixels:
        raw += b"\x00" + bytes(v for px in row for v in px)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 2, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", ihdr)
    png += chunk(b"IDAT", zlib.compress(raw, 9))
    png += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(png)


def shade(rgb, f):
    return tuple(max(0, min(255, int(c * f))) for c in rgb)


def make_texture(base, seed, style="dither"):
    """64x64 texture. style: dither | noise | chrome | glow | tread"""
    rng = random.Random(seed)
    px = []
    for y in range(64):
        row = []
        for x in range(64):
            c = base
            if style == "dither":
                if (x + y) % 2 == 0:
                    c = shade(c, 0.965)
                if rng.random() < 0.02:
                    c = shade(c, 1.06 if rng.random() < 0.5 else 0.9)
            elif style == "noise":
                c = shade(c, 0.92 + rng.random() * 0.14)
            elif style == "chrome":
                band = (y % 8)
                if band < 2:
                    c = shade(c, 1.18)
                elif band == 3:
                    c = shade(c, 0.82)
                else:
                    c = shade(c, 0.985 if (x + y) % 2 else 1.0)
            elif style == "glow":
                d = ((x - 32) ** 2 + (y - 32) ** 2) ** 0.5 / 45.0
                c = shade(c, 1.35 - 0.45 * d)
            elif style == "tread":
                if (x % 8 < 1 or y % 8 < 1):
                    c = shade(c, 1.25)
                if (x + y) % 2 == 0:
                    c = shade(c, 0.95)
            row.append(c)
        px.append(row)
    return px


MATERIALS = {
    "silver":      ((184, 191, 198), 1, "dither"),
    "silver_dark": ((128, 136, 144), 2, "dither"),
    "black":       ((28, 30, 34), 3, "noise"),
    "rubber":      ((20, 20, 22), 4, "tread"),
    "chrome":      ((205, 213, 220), 5, "chrome"),
    "engine":      ((106, 112, 118), 6, "dither"),
    "glass":       ((216, 200, 144), 7, "glow"),
    "glow":        ((255, 243, 176), 8, "glow"),
    "red_dim":     ((122, 26, 26), 9, "glow"),
    "red_bright":  ((255, 59, 48), 10, "glow"),
    "gold":        ((212, 175, 55), 11, "chrome"),
    "white":       ((226, 226, 220), 12, "dither"),
}


def gen_textures():
    os.makedirs(PACK_TEXTURES, exist_ok=True)
    os.makedirs(SRC_TEXTURES, exist_ok=True)
    for name, (base, seed, style) in MATERIALS.items():
        px = make_texture(base, seed, style)
        write_png(os.path.join(PACK_TEXTURES, name + ".png"), 64, 64, px)
        write_png(os.path.join(SRC_TEXTURES, name + ".png"), 64, 64, px)
    print(f"textures: {len(MATERIALS)} files -> {PACK_TEXTURES}")


# ---------------------------------------------------------------------------
# Model generation
# ---------------------------------------------------------------------------

def faces(x1, y1, z1, x2, y2, z2, mat):
    """UVs are 1:1 with the dithered texture (1 texel per unit)."""
    def face(w, h):
        return {"uv": [0, 0, round(w, 2), round(h, 2)], "texture": "#" + mat}

    return {
        "north": face(x2 - x1, y2 - y1),
        "south": face(x2 - x1, y2 - y1),
        "west":  face(z2 - z1, y2 - y1),
        "east":  face(z2 - z1, y2 - y1),
        "up":    face(x2 - x1, z2 - z1),
        "down":  face(x2 - x1, z2 - z1),
    }


def box(name, x1, y1, z1, x2, y2, z2, mat):
    return {
        "name": name,
        "from": [round(x1, 2), round(y1, 2), round(z1, 2)],
        "to": [round(x2, 2), round(y2, 2), round(z2, 2)],
        "faces": faces(x1, y1, z1, x2, y2, z2, mat),
    }


def ring_elements(prefix, radius, size, mat, width=2.6, count=12):
    """Axis-aligned boxes arranged in a circle (classic low-poly tire/rim)."""
    els = []
    for k in range(count):
        a = 2 * math.pi * k / count
        cx, cy, cz = 0.0, radius * math.cos(a), radius * math.sin(a)
        s = size / 2.0
        els.append(box(f"{prefix}_{k}", cx - width / 2, cy - s, cz - s,
                       cx + width / 2, cy + s, cz + s, mat))
    return els


def wheel_elements(prefix, mat_tire, mat_rim):
    els = []
    # tire (outer ring)
    els += ring_elements(f"{prefix}_tire", 4.9, 2.4, mat_tire, width=2.6, count=12)
    # rim (inner ring)
    els += ring_elements(f"{prefix}_rim", 3.9, 1.7, mat_rim, width=1.2, count=12)
    # 8 thin wire spokes (axis aligned)
    els.append(box(f"{prefix}_spoke_up", -0.6, 0, -0.6, 0.6, 3.7, 0.6, mat_rim))
    els.append(box(f"{prefix}_spoke_down", -0.6, -3.7, -0.6, 0.6, 0, 0.6, mat_rim))
    els.append(box(f"{prefix}_spoke_front", -0.6, -0.6, 0, 0.6, 0.6, 3.7, mat_rim))
    els.append(box(f"{prefix}_spoke_back", -0.6, -0.6, -3.7, 0.6, 0.6, 0, mat_rim))
    for sx, sz in ((1, 1), (1, -1), (-1, 1), (-1, -1)):
        els.append(box(f"{prefix}_spoke_d_{sx}_{sz}",
                       0.4 * sx, 0.4 * sz, 0, 2.7 * sx, 2.7 * sz, 2.4 * (1 if sz > 0 else 0) + (0 if sz > 0 else -2.4), mat_rim))
    # hub
    els.append(box(f"{prefix}_hub", -1.4, -1.4, -1.4, 1.4, 1.4, 1.4, "silver_dark"))
    els.append(box(f"{prefix}_cap", 1.4, -0.9, -0.9, 2.1, 0.9, 0.9, "chrome"))
    return els


def write_model(source_name, pack_filename, elements, textures, display=None):
    """Writes both the pack model (minecraft namespace, CMI lookup) and the
    human-editable Blockbench source (wavemotorcycle namespace)."""
    pack_doc = {
        "name": source_name,
        "from": {},
        "elements": elements,
        "gui_light": "side",
        "textures": {k: f"minecraft:item/{k}" for k in sorted(set(textures))},
    }
    src_doc = {
        "name": source_name,
        "namespace": "wavemotorcycle",
        "res": 16,
        "type": "item",
        "from": {},
        "elements": elements,
        "gui_light": "side",
        "textures": {k: f"wavemotorcycle:textures/item/{k}.png" for k in sorted(set(textures))},
    }
    if display is not None:
        pack_doc["display"] = display
        src_doc["display"] = display

    os.makedirs(PACK_MODELS, exist_ok=True)
    os.makedirs(SRC, exist_ok=True)
    with open(os.path.join(PACK_MODELS, pack_filename), "w") as f:
        json.dump(pack_doc, f, indent=2)
    with open(os.path.join(SRC, source_name + ".blockbench.json"), "w") as f:
        json.dump(src_doc, f, indent=2)
    print(f"model: {pack_filename} + blockbench_src/{source_name}.blockbench.json ({len(elements)} elements)")


def gen_body():
    els = []
    # --- underbone: the signature U-bar (two vertical arms + crossbar) ---
    els.append(box("underbone_left", -6.5, 3, -9, -4.5, 9, 9, "silver"))
    els.append(box("underbone_right", 4.5, 3, -9, 6.5, 9, 9, "silver"))
    els.append(box("underbone_cross", -5.5, 2.5, -2, 5.5, 5, 2, "silver"))
    # --- engine ---
    els.append(box("engine_body", -7, 7, -8, 7, 13, 2, "engine"))
    for k in range(3):
        z = -6 + k * 4
        els.append(box(f"fin_left_{k}", -7.6, 8, z, -7, 12, z + 2.4, "engine"))
        els.append(box(f"fin_right_{k}", 7, 8, z, 7.6, 12, z + 2.4, "engine"))
    els.append(box("engine_cover", -6, 12, -6, 6, 14.5, 4, "silver_dark"))
    # --- frame spine (under-seat backbone to the front cowl) ---
    els.append(box("spine_low", -4, 11, 0, 4, 13.5, 12, "silver_dark"))
    els.append(box("spine_high", -3.5, 13, 8, 3.5, 15.5, 14, "silver_dark"))
    # --- front cowl (silver nose) ---
    els.append(box("cowl_main", -8, 9, 14, 8, 15, 19, "silver"))
    els.append(box("cowl_top", -7, 14.5, 13, 7, 16, 19, "silver"))
    els.append(box("cowl_jaw", -6.5, 8, 15, 6.5, 9.5, 18.5, "silver_dark"))
    # --- tank / seat base ---
    els.append(box("tank", -6.5, 11.5, -14, 6.5, 14, 12, "black"))
    # --- seat (long, slim) ---
    els.append(box("seat_base", -5.5, 13, -17, 5.5, 15, 8, "black"))
    els.append(box("seat_pad", -5, 14.8, -16.5, 5, 15.6, 7, "rubber"))
    # --- rear tail ---
    els.append(box("tail", -5.5, 11.5, -19, 5.5, 14, -14, "black"))
    els.append(box("tail_cap", -4.5, 13.5, -19.5, 4.5, 15, -18.5, "silver_dark"))
    # --- rear fender ---
    els.append(box("rear_fender", -6, 10.8, -14, 6, 12.4, -8, "black"))
    els.append(box("rear_fender_lip", -6, 11.8, -14.6, 6, 12.8, -14, "black"))
    # --- license plate (Vietnamese style) ---
    els.append(box("plate", -3, 10.2, -14.8, 3, 12.6, -14.4, "white"))
    # --- exhaust (right side) ---
    els.append(box("exhaust_elbow", 7, 8, -6, 12, 10.5, -3.5, "chrome"))
    els.append(box("exhaust_pipe", 10, 8.8, -4, 12, 10.4, -13.5, "chrome"))
    els.append(box("exhaust_muffler", 9.5, 8.2, -13.5, 12.5, 11.8, -19, "chrome"))
    # --- footrests ---
    els.append(box("footrest_left", -9.5, 9.2, 0, -6.5, 10.6, 3, "rubber"))
    els.append(box("footrest_right", 6.5, 9.2, 0, 9.5, 10.6, 3, "rubber"))
    # --- rear carrier (luggage rack) ---
    els.append(box("carrier_plate", -5, 15.8, -20, 5, 16.6, -17, "silver"))
    els.append(box("carrier_strut_l", -4, 14.5, -18.5, -3.2, 16, -17.5, "silver_dark"))
    els.append(box("carrier_strut_r", 3.2, 14.5, -18.5, 4, 16, -17.5, "silver_dark"))
    mats = ["silver", "silver_dark", "black", "rubber", "chrome", "engine", "white"]
    write_model("body", "paper_1.json", els, mats)


def gen_front_assembly():
    els = []
    # fork tubes (vertical, low-poly)
    els.append(box("fork_left", -5, 0.5, -1.5, -3, 16.5, 0.5, "chrome"))
    els.append(box("fork_right", 3, 0.5, -1.5, 5, 16.5, 0.5, "chrome"))
    els.append(box("fork_crown", -5.5, 14.5, -2, 5.5, 17, 1, "silver_dark"))
    # front fender over the wheel
    els.append(box("fender", -7, 11, -4, 7, 12.8, 3, "black"))
    els.append(box("fender_lip", -7, 12, -4.6, 7, 13.2, -4, "black"))
    # headlight mount
    els.append(box("hl_mount", -2, 9.5, 3, 2, 12, 5.5, "silver"))
    write_model("front_assembly", "paper_2.json", els, ["chrome", "silver_dark", "black", "silver"])


def gen_handlebar():
    els = []
    els.append(box("stem", -1.5, 12.5, -1, 1.5, 15.5, 1, "silver_dark"))
    els.append(box("bar", -9, 15, -1, 9, 16.8, 1, "black"))
    els.append(box("grip_left", -10, 14.6, -1.5, -8.5, 17.4, 1.5, "rubber"))
    els.append(box("grip_right", 8.5, 14.6, -1.5, 10, 17.4, 1.5, "rubber"))
    # mirrors
    els.append(box("mirror_stalk_l", -10, 16.8, -0.5, -9.2, 19.5, 0.5, "chrome"))
    els.append(box("mirror_head_l", -11.5, 19.5, -1, -8.5, 22, 0.8, "silver"))
    els.append(box("mirror_stalk_r", 9.2, 16.8, -0.5, 10, 19.5, 0.5, "chrome"))
    els.append(box("mirror_head_r", 8.5, 19.5, -1, 11.5, 22, 0.8, "silver"))
    write_model("handlebar", "paper_3.json", els, ["silver_dark", "black", "rubber", "chrome", "silver"])


def gen_headlight(on):
    els = []
    els.append(box("housing", -5, 8.5, 5, 5, 14.5, 8.2, "silver_dark"))
    els.append(box("housing_lip", -4.2, 9.3, 8.2, 4.2, 13.7, 8.9, "black"))
    els.append(box("lens", -3.4, 10, 8.9, 3.4, 13, 9.6, "glow" if on else "glass"))
    write_model("headlight_on" if on else "headlight",
                "paper_102.json" if on else "paper_101.json", els,
                ["silver_dark", "black", "glow" if on else "glass"])


def gen_rear_light(brake):
    els = []
    els.append(box("housing", -3.5, 12.2, -9.4, 3.5, 15.2, -8.4, "black"))
    els.append(box("lens", -2.9, 12.9, -9.9, 2.9, 14.5, -9.4, "red_bright" if brake else "red_dim"))
    write_model("rear_light_brake" if brake else "rear_light",
                "paper_202.json" if brake else "paper_201.json", els,
                ["black", "red_bright" if brake else "red_dim"])


def gen_wheel(prefix, pack_filename):
    els = wheel_elements(prefix, "rubber", "chrome")
    write_model(f"{prefix}_wheel", pack_filename, els, ["rubber", "chrome", "silver_dark"])


KEY_DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
    "thirdperson_lefthand": {"rotation": [0, -90, 0], "translation": [0, 3, 1], "scale": [0.6, 0.6, 0.6]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 4, 2], "scale": [0.5, 0.5, 0.5]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 4, 2], "scale": [0.5, 0.5, 0.5]},
    "ground": {"translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [25, -35, 0], "translation": [0, 2.5, 0], "scale": [0.7, 0.7, 0.7]},
    "head": {"translation": [0, 14, 4]},
}


def gen_key():
    els = []
    # key bow (hollow ring)
    els.append(box("bow_top", -2.5, 3, -1, 2.5, 5.5, 1, "gold"))
    els.append(box("bow_left", -3.5, 0, -1, -1, 3, 1, "gold"))
    els.append(box("bow_right", 1, 0, -1, 3.5, 3, 1, "gold"))
    els.append(box("bow_bottom", -2.5, -1.5, -1, 2.5, 0.5, 1, "gold"))
    # shaft + teeth
    els.append(box("shaft", -0.8, -7, -0.8, 0.8, -1.5, 0.8, "gold"))
    els.append(box("tooth_1", 0.8, -6.5, -0.8, 2.8, -5, 0.8, "gold"))
    els.append(box("tooth_2", 0.8, -4.2, -0.8, 2.4, -2.7, 0.8, "gold"))
    write_model("key", "nether_star_1001.json", els, ["gold"], display=KEY_DISPLAY)


def gen_pack_mcmeta():
    doc = {
        "pack": {
            "pack_format": 88,
            "min_format": 88,
            "max_format": 88,
            "description": "Wave Motorcycle - 3D model and textures",
        }
    }
    with open(os.path.join(ROOT, "pack.mcmeta"), "w") as f:
        json.dump(doc, f, indent=2)
    print("pack.mcmeta")


def main():
    # clean stale output (previous versions used the wavemotorcycle namespace)
    stale = os.path.join(ROOT, "assets", "wavemotorcycle")
    if os.path.isdir(stale):
        import shutil
        shutil.rmtree(stale)
        print("removed stale assets/wavemotorcycle")
    gen_textures()
    gen_body()
    gen_front_assembly()
    gen_handlebar()
    gen_headlight(False)
    gen_headlight(True)
    gen_rear_light(False)
    gen_rear_light(True)
    gen_wheel("front", "paper_301.json")
    gen_wheel("rear", "paper_302.json")
    gen_key()
    gen_pack_mcmeta()
    print("done.")


if __name__ == "__main__":
    main()
