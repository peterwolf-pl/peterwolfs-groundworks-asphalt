#!/usr/bin/env python3
"""
generate_grader_textures.py
Procedural texture generator for Peterwolf's Groundworks Cat Motor Grader (Równiarka Drogowa).
Generates:
1. Entity texture atlas (512x512): src/main/resources/assets/pw_groundworks_asphalt/textures/entity/cat_grader.png
2. Item inventory icon (32x32): src/main/resources/assets/pw_groundworks_asphalt/textures/item/cat_grader.png
"""

import os
import math
from PIL import Image, ImageDraw

def create_grader_textures():
    output_dir_entity = "src/main/resources/assets/pw_groundworks_asphalt/textures/entity"
    output_dir_item = "src/main/resources/assets/pw_groundworks_asphalt/textures/item"
    os.makedirs(output_dir_entity, exist_ok=True)
    os.makedirs(output_dir_item, exist_ok=True)

    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Color Palette ──────────────────────────────────────────────────────────
    # Official Caterpillar Equipment Highway Yellow
    c_cat_yellow = (244, 180, 26, 255)
    c_cat_yellow_light = (255, 205, 55, 255)
    c_cat_yellow_dark = (195, 138, 12, 255)
    c_cat_yellow_shadow = (145, 98, 8, 255)

    # Heavy structural chassis, casting & dark steel
    c_dark_iron = (36, 38, 42, 255)
    c_iron_light = (64, 68, 76, 255)
    c_iron_recess = (20, 22, 25, 255)
    c_iron_mid = (48, 52, 58, 255)

    # Hydraulic cylinders & chrome rods
    c_chrome_highlight = (240, 245, 252, 255)
    c_chrome_mid = (190, 200, 215, 255)
    c_chrome_shadow = (140, 150, 165, 255)
    c_hydraulic_barrel = (28, 30, 34, 255)

    # Heavy earthmover tire rubber
    c_tire_tread = (24, 25, 27, 255)
    c_tire_sidewall = (34, 35, 38, 255)
    c_tire_lug = (46, 48, 52, 255)
    c_tire_groove = (16, 17, 19, 255)

    # Moldboard & blade wear surfaces
    c_moldboard_face = (130, 135, 145, 255)
    c_cutting_edge_bright = (215, 222, 230, 255)
    c_cutting_edge_steel = (165, 172, 182, 255)
    c_soil_stain = (95, 82, 70, 255)

    # Circle ring gear & teeth
    c_circle_plate = (42, 45, 50, 255)
    c_gear_teeth = (85, 90, 100, 255)
    c_gear_recess = (22, 24, 28, 255)

    # Ripper & scarifier teeth
    c_carbide_tooth = (220, 205, 150, 255)
    c_shank_steel = (55, 58, 65, 255)

    # Safety Hazard stripes (diagonal yellow & black)
    c_hazard_yellow = (255, 215, 0, 255)
    c_hazard_black = (18, 18, 18, 255)

    # Panoramic cab & interior
    c_tinted_glass = (140, 210, 245, 38)
    c_glass_frame = (28, 29, 32, 255)
    c_seat_charcoal = (32, 33, 36, 255)
    c_seat_cushion = (45, 47, 52, 255)
    c_console_dash = (22, 24, 26, 255)
    c_instrument_glow = (50, 210, 240, 255)

    # Lights & Warning Beacon
    c_led_white = (248, 252, 255, 255)
    c_taillight_red = (220, 30, 30, 255)
    c_beacon_amber = (255, 155, 10, 255)
    c_beacon_bright = (255, 235, 90, 255)

    # ── 1. Base Fill for 100% Solid Atlas Coverage (Zero Transparent Holes) ───
    draw.rectangle([0, 0, 512, 512], fill=c_dark_iron)

    # ── Helper Painting Functions ─────────────────────────────────────────────
    def paint_panel(u, v, w, h, base_color, bevel_color=None, shadow_color=None):
        draw.rectangle([u, v, u + w, v + h], fill=base_color)
        if bevel_color:
            draw.line([(u, v), (u + w, v)], fill=bevel_color)
            draw.line([(u, v), (u, v + h)], fill=bevel_color)
        if shadow_color:
            draw.line([(u, v + h), (u + w, v + h)], fill=shadow_color)
            draw.line([(u + w, v), (u + w, v + h)], fill=shadow_color)

    def paint_hazard_stripes(u, v, w, h, stripe_w=8):
        draw.rectangle([u, v, u + w, v + h], fill=c_hazard_yellow)
        for offset in range(-h, w + h, stripe_w * 2):
            points = [
                (u + max(0, offset), v),
                (u + min(w, offset + stripe_w), v),
                (u + min(w, offset + stripe_w + h), v + h),
                (u + max(0, offset + h), v + h)
            ]
            draw.polygon(points, fill=c_hazard_black)

    def paint_louvers(u, v, w, h, pitch=4):
        draw.rectangle([u, v, u + w, v + h], fill=c_iron_recess)
        for y in range(v + 2, v + h - 2, pitch):
            draw.rectangle([u + 2, y, u + w - 2, y + 1], fill=c_iron_light)

    def paint_tread_pattern(u, v, w, h):
        draw.rectangle([u, v, u + w, v + h], fill=c_tire_tread)
        # Chevron / diamond lugs
        for y in range(v + 2, v + h - 2, 6):
            draw.line([(u + 2, y), (u + w // 2, y + 2)], fill=c_tire_lug, width=2)
            draw.line([(u + w // 2, y + 2), (u + w - 2, y)], fill=c_tire_lug, width=2)
            draw.line([(u + 2, y + 3), (u + w - 2, y + 3)], fill=c_tire_groove)

    # ── ZONE A: Wheels, Heavy Tires, Rims & Planetary Hubs [u: 0..200, v: 0..160]
    # Tire Carcass segments (0..144, 0..32)
    for offs in [0, 36, 72, 108]:
        paint_tread_pattern(offs, 0, 34, 30)

    # Deep steel dished rim in CAT yellow (0, 32, 30, 26)
    paint_panel(0, 32, 28, 24, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Wheel rim flange & 10 lug nuts
    cx, cy = 14, 44
    draw.ellipse([cx - 8, cy - 8, cx + 8, cy + 8], outline=c_cat_yellow_shadow, width=2)
    for ang in range(0, 360, 36):
        rad = math.radians(ang)
        lx = int(cx + 6 * math.cos(rad))
        ly = int(cy + 6 * math.sin(rad))
        draw.point((lx, ly), fill=c_iron_light)

    # Heavy planetary final drive hub (32, 32, 18, 14)
    paint_panel(32, 32, 18, 14, c_dark_iron, c_iron_light, c_iron_recess)
    draw.ellipse([36, 34, 46, 44], fill=c_iron_mid, outline=c_iron_light)

    # Outer hub cap (52, 32, 12, 10)
    paint_panel(52, 32, 12, 10, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)

    # Tandem Rocker Arm beam (0, 60, 70, 38)
    paint_panel(0, 60, 70, 38, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Heavy cast rocker inspection cover & pivot hub
    draw.rectangle([15, 68, 55, 90], fill=c_dark_iron, outline=c_iron_light)
    draw.ellipse([30, 74, 40, 84], fill=c_iron_mid, outline=c_cat_yellow)
    # Tandem spindle hub (72, 60, 22, 14)
    paint_panel(72, 60, 22, 14, c_dark_iron, c_iron_light, c_iron_recess)

    # ── ZONE B: Front Axle, Steering & Push Block [u: 200..360, v: 0..160] ─────
    # Front bolster casting (200, 0, 46, 20)
    paint_panel(200, 0, 46, 20, c_dark_iron, c_iron_light, c_iron_recess)
    # Oscillating center axle beam (248, 0, 72, 12)
    paint_panel(248, 0, 72, 12, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Steering knuckles (200, 22, 22, 16) & wheel lean brackets (224, 22, 18, 13)
    paint_panel(200, 22, 22, 16, c_dark_iron, c_iron_light, c_iron_recess)
    paint_panel(224, 22, 18, 13, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)

    # Front push block / counterweight (244, 22, 58, 20)
    paint_panel(244, 22, 58, 20, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Front hazard warning chevron panel
    paint_hazard_stripes(248, 25, 50, 14, stripe_w=6)
    # Front tow hitch clevis pin (304, 22, 18, 10)
    paint_panel(304, 22, 18, 10, c_dark_iron, c_iron_light, c_iron_recess)

    # ── ZONE C: Arched Backbone Gooseneck Frame [u: 0..200, v: 160..280] ──────
    # Gooseneck front sloped segment (0, 160, 54, 26)
    paint_panel(0, 160, 54, 26, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Gooseneck high apex arch (56, 160, 58, 29)
    paint_panel(56, 160, 58, 29, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Gooseneck rear segment to cab (116, 160, 56, 27)
    paint_panel(116, 160, 56, 27, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Bottom reinforcement web flange (0, 192, 112, 50)
    paint_panel(0, 192, 112, 50, c_cat_yellow_dark, c_cat_yellow, c_cat_yellow_shadow)
    # Heavy hydraulic lines running along gooseneck
    for y in [170, 174, 178]:
        draw.line([(4, y), (170, y)], fill=c_iron_recess, width=2)
        draw.line([(4, y), (170, y)], fill=c_iron_mid, width=1)
    # Front drawbar ball hitch socket (114, 192, 26, 13)
    paint_panel(114, 192, 26, 13, c_dark_iron, c_iron_light, c_iron_recess)
    # High blade lift cylinder trunnion mounts (142, 192, 50, 13)
    paint_panel(142, 192, 50, 13, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Side-shift anchor bracket (0, 244, 26, 12)
    paint_panel(0, 244, 26, 12, c_dark_iron, c_iron_light, c_iron_recess)

    # ── ZONE D: Drawbar, Circle Ring & Moldboard Blade [u: 200..380, v: 160..280]
    # Drawbar front tongue (200, 160, 46, 22)
    paint_panel(200, 160, 46, 22, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Drawbar tubular A-frame legs (248, 160, 58, 30) & (308, 160, 58, 30)
    paint_panel(248, 160, 58, 30, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    paint_panel(308, 160, 58, 30, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)

    # Circle ring gear plate (200, 192, 106, 31)
    paint_panel(200, 192, 106, 31, c_circle_plate, c_iron_light, c_iron_recess)
    # Internal ring gear teeth pattern
    for x in range(205, 300, 4):
        draw.rectangle([x, 200, x + 2, 215], fill=c_gear_teeth)
        draw.line([(x + 1, 200), (x + 1, 215)], fill=c_chrome_highlight)
    # Center boss (308, 192, 50, 16) & Pinion motor (308, 210, 22, 13)
    paint_panel(308, 192, 50, 16, c_dark_iron, c_iron_light, c_iron_recess)
    paint_panel(308, 210, 22, 13, c_dark_iron, c_iron_light, c_iron_recess)

    # Moldboard blade curved front face (200, 226, 102, 14)
    paint_panel(200, 226, 102, 14, c_moldboard_face, c_cutting_edge_bright, c_iron_recess)
    # Horizontal curvature and earth grading scratch marks
    for y in [228, 231, 234]:
        draw.line([(202, y), (300, y)], fill=c_cutting_edge_steel)
        draw.line([(210, y + 1), (280, y + 1)], fill=c_soil_stain)

    # Lower hardened cutting edge wear lip (200, 242, 102, 6)
    draw.rectangle([200, 242, 302, 248], fill=c_cutting_edge_bright)
    # Reversible cutting edge plow bolts
    for x in range(204, 300, 8):
        draw.ellipse([x - 1, 244, x + 1, 246], fill=c_iron_recess)

    # Upper rolled dirt spill curl lip (200, 250, 102, 6)
    paint_panel(200, 250, 102, 6, c_cat_yellow_dark, c_cat_yellow, c_cat_yellow_shadow)

    # Rear vertical stiffener ribs & slide rails (200, 258, 24, 14) & (304, 226, 70, 6)
    paint_panel(200, 258, 24, 14, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    paint_panel(304, 226, 70, 6, c_dark_iron, c_iron_light, c_iron_recess)

    # Hydraulic blade-lift cylinders barrels (304, 234, 18, 22)
    paint_panel(304, 234, 18, 22, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Chrome hydraulic piston rods (324, 234, 10, 18)
    draw.rectangle([324, 234, 334, 252], fill=c_chrome_mid)
    draw.line([(326, 234), (326, 252)], fill=c_chrome_highlight, width=2)
    draw.line([(332, 234), (332, 252)], fill=c_chrome_shadow, width=1)
    # Side-shift cylinder (336, 234, 40, 8)
    paint_panel(336, 234, 40, 8, c_dark_iron, c_iron_light, c_iron_recess)

    # ── ZONE E: Operator Cab ROPS & Panoramic Interior [u: 0..260, v: 280..420]
    # Elevated cab floor & lower platform sill (0, 280, 90, 26)
    paint_panel(0, 280, 90, 26, c_dark_iron, c_iron_light, c_iron_recess)
    # Lower observation floor glass (92, 280, 46, 7)
    draw.rectangle([92, 280, 138, 287], fill=c_tinted_glass, outline=c_glass_frame)

    # Lower side door sills (0, 308, 42, 28)
    paint_panel(0, 308, 42, 28, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # ROPS structural corner pillars (44, 308, 10, 20)
    paint_panel(44, 308, 10, 20, c_dark_iron, c_iron_light, c_iron_recess)
    # Upper window headers (56, 308, 90, 25)
    paint_panel(56, 308, 90, 25, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Tinted panoramic safety glass enclosure (148, 308, 82, 34)
    draw.rectangle([148, 308, 230, 342], fill=c_tinted_glass, outline=c_glass_frame)
    # Subtle glass reflection glint
    draw.line([(152, 338), (175, 312)], fill=(220, 245, 255, 65), width=2)
    draw.line([(180, 338), (195, 320)], fill=(220, 245, 255, 65), width=1)

    # Sloped Cat equipment cab roof canopy (0, 338, 98, 27)
    paint_panel(0, 338, 98, 27, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Roof HVAC Air Conditioning unit pod (100, 338, 46, 15)
    paint_panel(100, 338, 46, 15, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    paint_louvers(104, 342, 38, 8, pitch=3)

    # Operator luxury suspension seat (0, 368, 38, 22)
    paint_panel(0, 368, 38, 22, c_seat_charcoal, c_seat_cushion, c_iron_recess)
    # Steering console & electro-hydraulic joystick pods (40, 368, 30, 14)
    paint_panel(40, 368, 30, 14, c_console_dash, c_iron_light, c_iron_recess)
    draw.rectangle([45, 372, 55, 378], fill=c_instrument_glow)
    draw.rectangle([58, 372, 65, 378], fill=c_instrument_glow)
    # Steering wheel & column (72, 368, 18, 12)
    paint_panel(72, 368, 18, 12, c_dark_iron, c_iron_light, c_iron_recess)

    # Left & right external walkways with anti-slip perforated tread (0, 392, 58, 25)
    paint_panel(0, 392, 58, 25, c_dark_iron, c_iron_light, c_iron_recess)
    for px in range(4, 54, 4):
        for py in range(396, 412, 4):
            draw.point((px, py), fill=c_iron_light)

    # Safety boarding ladders (60, 392, 12, 15)
    paint_panel(60, 392, 12, 15, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)

    # ── ZONE F: Rear Engine Compartment & Cat Diesel Hood [u: 260..512, v: 280..420]
    # Rear heavy box frame (260, 280, 118, 50)
    paint_panel(260, 280, 118, 50, c_dark_iron, c_iron_light, c_iron_recess)
    # Engine hood top sloping cover in CAT yellow (380, 280, 106, 42)
    paint_panel(380, 280, 106, 42, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Hood access latch handles
    draw.rectangle([400, 290, 410, 294], fill=c_dark_iron, outline=c_iron_light)
    draw.rectangle([450, 290, 460, 294], fill=c_dark_iron, outline=c_iron_light)

    # Engine hood side panels with cooling louvers (260, 332, 78, 50)
    paint_panel(260, 332, 78, 50, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    paint_louvers(270, 342, 60, 30, pitch=4)

    # Engine front bulkhead / firewall (340, 332, 38, 16)
    paint_panel(340, 332, 38, 16, c_dark_iron, c_iron_light, c_iron_recess)

    # Rear radiator housing & screen (380, 332, 50, 22)
    paint_panel(380, 332, 50, 22, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    paint_louvers(386, 336, 38, 14, pitch=3)

    # Heavy cast iron rear counterweight bumper (432, 332, 58, 16)
    paint_panel(432, 332, 58, 16, c_dark_iron, c_iron_light, c_iron_recess)
    paint_hazard_stripes(436, 336, 50, 8, stripe_w=6)

    # Hydraulic oil tank & battery compartments (260, 384, 48, 28)
    paint_panel(260, 384, 48, 28, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)

    # Vertical exhaust pipe (310, 384, 10, 20) & Muffler body (322, 384, 18, 16)
    draw.rectangle([310, 384, 320, 404], fill=c_dark_iron)
    paint_panel(322, 384, 18, 16, c_dark_iron, c_iron_light, c_iron_recess)
    # Perforated heat shield wrap around muffler
    for mx in range(324, 338, 3):
        for my in range(386, 398, 3):
            draw.point((mx, my), fill=c_iron_light)

    # Exhaust rain flapper cap (342, 384, 14, 6)
    paint_panel(342, 384, 14, 6, c_chrome_mid, c_chrome_highlight, c_chrome_shadow)

    # Cyclone air pre-cleaner canister (358, 384, 22, 19)
    paint_panel(358, 384, 22, 19, c_dark_iron, c_iron_light, c_iron_recess)
    draw.ellipse([362, 386, 376, 398], fill=c_iron_recess, outline=c_cat_yellow)

    # ── ZONE G: Rear Ripper / Scarifier [u: 380..512, v: 0..160] ──────────────
    # Ripper mounting brackets (380, 0, 22, 18)
    paint_panel(380, 0, 22, 18, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Parallel linkage arms (404, 0, 34, 19)
    paint_panel(404, 0, 34, 19, c_cat_yellow, c_cat_yellow_light, c_cat_yellow_dark)
    # Ripper cross tool beam (440, 0, 70, 10)
    paint_panel(440, 0, 70, 10, c_dark_iron, c_iron_light, c_iron_recess)
    # Ripper curved forged steel shanks (380, 20, 22, 24)
    paint_panel(380, 20, 22, 24, c_shank_steel, c_iron_light, c_iron_recess)
    # Replaceable carbide teeth points (404, 20, 18, 11)
    paint_panel(404, 20, 18, 11, c_carbide_tooth, (255, 240, 180, 255), (170, 150, 90, 255))
    # Ripper hydraulic rams & rods (424, 20, 14, 15) & (440, 20, 10, 12)
    paint_panel(424, 20, 14, 15, c_dark_iron, c_iron_light, c_iron_recess)
    draw.rectangle([440, 20, 450, 32], fill=c_chrome_mid)

    # ── ZONE H: Lights, Beacons & Safety Decals [u: 0..260, v: 420..512] ──────
    # Cab roof LED floodlights (0, 420, 14, 6)
    draw.rectangle([0, 420, 14, 426], fill=c_led_white, outline=c_dark_iron)
    # Rear frame taillights (16, 420, 14, 6)
    draw.rectangle([16, 420, 30, 426], fill=c_taillight_red, outline=c_dark_iron)
    # Front headlights on bolster (32, 420, 14, 7)
    draw.rectangle([32, 420, 46, 427], fill=c_led_white, outline=c_dark_iron)

    # Safety amber beacon base & dome (48, 420, 18, 10)
    draw.rectangle([48, 420, 66, 430], fill=c_beacon_amber, outline=c_dark_iron)
    draw.line([(50, 422), (64, 422)], fill=c_beacon_bright)
    # Beacon spinning reflector (68, 420, 10, 7)
    draw.rectangle([68, 420, 78, 427], fill=c_beacon_bright, outline=c_beacon_amber)

    # Hazard safety chevron stripe decals (80, 420, 52, 9)
    paint_hazard_stripes(80, 420, 52, 9, stripe_w=5)

    # Caterpillar branding decals / logo area (134, 420, 64, 11)
    draw.rectangle([134, 420, 198, 431], fill=c_hazard_black)
    draw.rectangle([138, 422, 194, 429], fill=c_cat_yellow)
    # CAT triangle emblem silhouette
    draw.polygon([(142, 428), (146, 423), (150, 428)], fill=c_hazard_black)

    # Rearview mirrors on stalks (200, 420, 12, 12)
    draw.rectangle([200, 420, 212, 432], fill=c_tinted_glass, outline=c_dark_iron)

    # Save Entity Texture
    entity_path = os.path.join(output_dir_entity, "cat_grader.png")
    img.save(entity_path, "PNG")
    print(f"Generated entity texture: {entity_path} ({img.size[0]}x{img.size[1]})")

    # ── 2. Item Inventory Icon (32x32) ────────────────────────────────────────
    # Create an authentic isometric pixel-art icon for the Cat Motor Grader
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    i_draw = ImageDraw.Draw(icon)

    # Background shadow
    i_draw.ellipse([3, 24, 29, 30], fill=(15, 15, 18, 120))

    # Rear tandem wheels (4 wheels)
    i_draw.rectangle([4, 19, 8, 25], fill=c_tire_tread, outline=c_dark_iron)
    i_draw.rectangle([10, 19, 14, 25], fill=c_tire_tread, outline=c_dark_iron)
    i_draw.point((6, 22), fill=c_cat_yellow)
    i_draw.point((12, 22), fill=c_cat_yellow)

    # Front wheel
    i_draw.rectangle([24, 17, 28, 24], fill=c_tire_tread, outline=c_dark_iron)
    i_draw.point((26, 20), fill=c_cat_yellow)

    # Rear engine hood
    i_draw.rectangle([4, 13, 14, 19], fill=c_cat_yellow, outline=c_cat_yellow_dark)
    i_draw.line([(6, 15), (12, 15)], fill=c_iron_recess)  # louvers
    i_draw.line([(6, 17), (12, 17)], fill=c_iron_recess)

    # Exhaust pipe
    i_draw.line([(6, 9), (6, 13)], fill=c_dark_iron, width=1)
    i_draw.point((7, 9), fill=c_chrome_highlight)

    # ROPS Operator Cab
    i_draw.rectangle([12, 8, 18, 18], fill=c_cat_yellow, outline=c_cat_yellow_dark)
    i_draw.rectangle([14, 10, 17, 14], fill=c_tinted_glass, outline=c_glass_frame)  # windshield
    # Roof canopy & beacon
    i_draw.line([(11, 8), (19, 8)], fill=c_cat_yellow_light)
    i_draw.point((15, 7), fill=c_beacon_amber)

    # Arched Gooseneck Backbone Beam
    i_draw.line([(18, 13), (21, 11), (25, 15)], fill=c_cat_yellow, width=2)
    i_draw.line([(18, 12), (21, 10), (25, 14)], fill=c_cat_yellow_light, width=1)

    # Under-frame Moldboard / Blade
    i_draw.line([(16, 22), (22, 20)], fill=c_cutting_edge_bright, width=2)
    i_draw.line([(16, 23), (22, 21)], fill=c_moldboard_face, width=1)

    # Rear ripper teeth
    i_draw.line([(3, 20), (2, 22)], fill=c_carbide_tooth, width=1)

    item_path = os.path.join(output_dir_item, "cat_grader.png")
    icon.save(item_path, "PNG")
    print(f"Generated item icon: {item_path} ({icon.size[0]}x{icon.size[1]})")

if __name__ == "__main__":
    create_grader_textures()
