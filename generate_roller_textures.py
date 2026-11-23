#!/usr/bin/env python3
"""
Procedural 512x512 Texture Generator for 16-Ton Heavy Road Roller (Walec Drogowy).
Generates:
  1. src/main/resources/assets/pw_groundworks_asphalt/textures/entity/road_roller.png (512x512)
  2. src/main/resources/assets/pw_groundworks_asphalt/textures/item/road_roller.png (32x32 or 64x64)
"""

import os
import math
import random
from PIL import Image, ImageDraw, ImageFilter
import build_full_roller

def generate_textures():
    parts = build_full_roller.create_model_and_textures()
    all_boxes = [b for p in parts.values() for b in p.boxes]

    # Use the same shelf packer as generate_model_java.py
    all_boxes.sort(key=lambda b: (b.req_h, b.req_w), reverse=True)

    atlas_w = 512
    atlas_h = 512
    margin = 2

    current_x = margin
    current_y = margin
    shelf_h = 0

    for b in all_boxes:
        w = b.req_w + margin
        h = b.req_h + margin
        if current_x + w > atlas_w:
            current_x = margin
            current_y += shelf_h + margin
            shelf_h = 0
        b.u = current_x
        b.v = current_y
        current_x += w
        if h > shelf_h:
            shelf_h = h

    # Create 512x512 RGBA image
    img = Image.new("RGBA", (atlas_w, atlas_h), (25, 27, 30, 255))
    draw = ImageDraw.Draw(img)

    # Helper functions for procedural painting
    def draw_metal(x0, y0, x1, y1, base_rgb, noise_amp=8, bevel=True):
        for y in range(y0, y1):
            for x in range(x0, x1):
                n = int((math.sin(x * 12.9898 + y * 78.233) * 43758.5453) % 1.0 * noise_amp)
                r = max(0, min(255, base_rgb[0] + n))
                g = max(0, min(255, base_rgb[1] + n))
                b_val = max(0, min(255, base_rgb[2] + n))
                img.putpixel((x, y), (r, g, b_val, 255))

        if bevel and (x1 - x0 > 2) and (y1 - y0 > 2):
            # Highlight top and left edges
            for x in range(x0, x1):
                pr, pg, pb, _ = img.getpixel((x, y0))
                img.putpixel((x, y0), (min(255, pr + 40), min(255, pg + 40), min(255, pb + 40), 255))
            for y in range(y0, y1):
                pr, pg, pb, _ = img.getpixel((x0, y))
                img.putpixel((x0, y), (min(255, pr + 30), min(255, pg + 30), min(255, pb + 30), 255))
            # Shadow bottom and right edges
            for x in range(x0, x1):
                pr, pg, pb, _ = img.getpixel((x, y1 - 1))
                img.putpixel((x, y1 - 1), (max(0, pr - 40), max(0, pg - 40), max(0, pb - 40), 255))
            for y in range(y0, y1):
                pr, pg, pb, _ = img.getpixel((x1 - 1, y))
                img.putpixel((x1 - 1, y), (max(0, pr - 30), max(0, pg - 30), max(0, pb - 30), 255))

    def draw_drum_steel(x0, y0, x1, y1):
        # Polished cylindrical machined steel with horizontal lathe turn lines & specular highlight
        for y in range(y0, y1):
            t = (y - y0) / max(1, (y1 - y0 - 1))
            spec = math.pow(math.sin(t * math.pi), 3.0) * 60.0
            for x in range(x0, x1):
                lathe = (x % 3) * 6 - 3
                r = int(min(255, max(0, 175 + spec + lathe)))
                g = int(min(255, max(0, 185 + spec + lathe)))
                b_val = int(min(255, max(0, 195 + spec + lathe)))
                img.putpixel((x, y), (r, g, b_val, 255))

    def draw_drum_disc(x0, y0, x1, y1):
        cx = (x0 + x1) / 2.0
        cy = (y0 + y1) / 2.0
        max_r = max(1.0, min(x1 - x0, y1 - y0) / 2.0)
        for y in range(y0, y1):
            for x in range(x0, x1):
                dist = math.sqrt((x - cx) ** 2 + (y - cy) ** 2) / max_r
                if dist > 0.95:
                    c = (60, 65, 70)  # rim weld ring
                elif dist > 0.85:
                    c = (140, 145, 150)
                elif 0.35 < dist < 0.45:
                    c = (70, 75, 80)  # bolt circle
                elif dist < 0.3:
                    c = (50, 52, 55)  # center axle cap
                else:
                    c = (120, 125, 130)
                img.putpixel((x, y), (c[0], c[1], c[2], 255))

    def draw_tire_rubber(x0, y0, x1, y1):
        # Deep charcoal rubber with chevron / V-tread pattern
        for y in range(y0, y1):
            for x in range(x0, x1):
                # Chevron pattern: V shape |x - mid| + y
                mid = (x0 + x1) / 2.0
                diag = int(abs(x - mid) * 1.5 + y * 0.8) % 8
                if diag < 3:
                    c = (18, 18, 20)  # deep groove
                else:
                    c = (38, 40, 42)  # tread block
                img.putpixel((x, y), (c[0], c[1], c[2], 255))

    def draw_grille(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if (x % 3 == 0) or (y % 3 == 0):
                    img.putpixel((x, y), (15, 15, 15, 255))
                else:
                    img.putpixel((x, y), (45, 48, 52, 255))

    def draw_glass(x0, y0, x1, y1):
        # Tinted sky-blue/grey panoramic glass with reflection streak
        for y in range(y0, y1):
            t_y = (y - y0) / max(1, (y1 - y0 - 1))
            for x in range(x0, x1):
                t_x = (x - x0) / max(1, (x1 - x0 - 1))
                # Border ceramic frit
                if x == x0 or x == x1 - 1 or y == y0 or y == y1 - 1:
                    img.putpixel((x, y), (25, 28, 32, 255))
                else:
                    diag = t_x * 0.7 + t_y * 0.3
                    alpha = 65 if (0.4 < diag < 0.6) else 38
                    r = 220 if (0.4 < diag < 0.6) else 140
                    g = 245 if (0.4 < diag < 0.6) else 210
                    b_val = 255 if (0.4 < diag < 0.6) else 245
                    img.putpixel((x, y), (r, g, b_val, alpha))

    def draw_beacon(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                fresnel = 25 if (y % 2 == 0) else 0
                img.putpixel((x, y), (min(255, 240 + fresnel), 130, 15, 255))

    def draw_led_light(x0, y0, x1, y1):
        for y in range(y0, y1):
            for x in range(x0, x1):
                if x == x0 or x == x1 - 1 or y == y0 or y == y1 - 1:
                    img.putpixel((x, y), (30, 30, 30, 255))
                else:
                    grid = 30 if ((x + y) % 3 == 0) else 0
                    img.putpixel((x, y), (210 + grid, 230 + grid, 255, 255))

    def draw_tail_light(x0, y0, x1, y1):
        mid = (x0 + x1) // 2
        for y in range(y0, y1):
            for x in range(x0, x1):
                if x < mid:
                    img.putpixel((x, y), (200, 20, 20, 255))  # red brake
                else:
                    img.putpixel((x, y), (240, 140, 20, 255))  # amber turn signal

    # Paint every box in the atlas!
    for b in all_boxes:
        x0 = b.u
        y0 = b.v
        x1 = min(atlas_w, x0 + b.req_w)
        y1 = min(atlas_h, y0 + b.req_h)

        mat = b.mat
        if mat == "yellow_body":
            draw_metal(x0, y0, x1, y1, (222, 162, 10), noise_amp=6, bevel=True)
        elif mat == "yellow_safety":
            draw_metal(x0, y0, x1, y1, (245, 195, 0), noise_amp=4, bevel=True)
        elif mat == "chassis_dark":
            draw_metal(x0, y0, x1, y1, (38, 42, 46), noise_amp=8, bevel=True)
        elif mat == "drum_steel":
            draw_drum_steel(x0, y0, x1, y1)
        elif mat == "drum_disc":
            draw_drum_disc(x0, y0, x1, y1)
        elif mat == "tire_rubber":
            draw_tire_rubber(x0, y0, x1, y1)
        elif mat == "wheel_rim":
            draw_metal(x0, y0, x1, y1, (220, 160, 10), noise_amp=6, bevel=True)
        elif mat == "cab_glass":
            draw_glass(x0, y0, x1, y1)
        elif mat == "black_grille":
            draw_grille(x0, y0, x1, y1)
        elif mat == "black_plastic":
            draw_metal(x0, y0, x1, y1, (30, 32, 34), noise_amp=6, bevel=False)
        elif mat == "black_rubber":
            draw_metal(x0, y0, x1, y1, (25, 25, 26), noise_amp=4, bevel=False)
        elif mat == "seat_black":
            draw_metal(x0, y0, x1, y1, (24, 25, 28), noise_amp=5, bevel=True)
        elif mat == "steel_dark":
            draw_metal(x0, y0, x1, y1, (65, 70, 75), noise_amp=8, bevel=True)
        elif mat == "steel_polished":
            draw_metal(x0, y0, x1, y1, (225, 230, 235), noise_amp=4, bevel=True)
        elif mat == "amber_beacon":
            draw_beacon(x0, y0, x1, y1)
        elif mat == "led_light":
            draw_led_light(x0, y0, x1, y1)
        elif mat == "tail_light":
            draw_tail_light(x0, y0, x1, y1)
        elif mat == "mirror_glass":
            draw_metal(x0, y0, x1, y1, (200, 215, 225), noise_amp=3, bevel=True)
        else:
            draw_metal(x0, y0, x1, y1, (100, 100, 100), noise_amp=5, bevel=True)

    # Save entity texture
    out_dir = "src/main/resources/assets/pw_groundworks_asphalt/textures/entity"
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "road_roller.png")
    img.save(out_path, "PNG")
    print(f"Generated Entity Texture: {out_path} (512x512)")

    # Generate 64x64 item icon
    item_dir = "src/main/resources/assets/pw_groundworks_asphalt/textures/item"
    os.makedirs(item_dir, exist_ok=True)
    item_path = os.path.join(item_dir, "road_roller.png")

    icon = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    idraw = ImageDraw.Draw(icon)

    # Isometric/Side view of Road Roller:
    # 1. Front drum (steel cylinder at left)
    idraw.ellipse([4, 28, 26, 54], fill=(180, 190, 200, 255), outline=(90, 100, 110, 255), width=2)
    idraw.ellipse([11, 37, 19, 45], fill=(50, 55, 60, 255))  # hub cap

    # 2. Yoke frame (yellow beam)
    idraw.polygon([(15, 41), (30, 41), (34, 34), (16, 34)], fill=(225, 165, 10, 255), outline=(150, 100, 0, 255))

    # 3. Rear pneumatic wheel
    idraw.ellipse([40, 30, 60, 54], fill=(30, 32, 34, 255), outline=(15, 15, 15, 255), width=2)
    idraw.ellipse([46, 37, 54, 47], fill=(220, 160, 10, 255))  # yellow rim

    # 4. Engine hood (rear yellow sloped)
    idraw.polygon([(32, 35), (34, 24), (58, 27), (58, 38), (42, 38)], fill=(225, 165, 10, 255), outline=(150, 100, 0, 255))
    # Grille
    idraw.rectangle([48, 29, 56, 34], fill=(40, 40, 40, 255))
    # Exhaust pipe
    idraw.rectangle([46, 17, 48, 25], fill=(60, 65, 70, 255))

    # 5. Panoramic cab (center high)
    idraw.polygon([(26, 34), (28, 14), (40, 14), (42, 34)], fill=(75, 110, 140, 230), outline=(30, 35, 40, 255), width=1)
    # Cab roof
    idraw.rectangle([25, 11, 42, 14], fill=(225, 165, 10, 255), outline=(150, 100, 0, 255))
    # Amber beacon
    idraw.rectangle([32, 8, 35, 11], fill=(255, 140, 0, 255))

    icon.save(item_path, "PNG")
    print(f"Generated Item Icon: {item_path} (64x64)")

if __name__ == "__main__":
    generate_textures()
