#!/usr/bin/env python3
"""
generate_paver_textures.py
Procedural texture generator for Peterwolf's Groundworks Asphalt Paver (Rozściełacz Asfaltu).
Generates:
1. Entity texture atlas (512x512): src/main/resources/assets/pw_groundworks_asphalt/textures/entity/asphalt_paver.png
2. Item inventory icon (32x32): src/main/resources/assets/pw_groundworks_asphalt/textures/item/asphalt_paver.png
"""

import os
import math
from PIL import Image, ImageDraw

def create_paver_textures():
    output_dir_entity = "src/main/resources/assets/pw_groundworks_asphalt/textures/entity"
    output_dir_item = "src/main/resources/assets/pw_groundworks_asphalt/textures/item"
    os.makedirs(output_dir_entity, exist_ok=True)
    os.makedirs(output_dir_item, exist_ok=True)

    img = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # ── Color Palette ──────────────────────────────────────────────────────────
    # Industrial road construction equipment yellow (CAT / Vögele)
    c_yellow = (242, 178, 0, 255)
    c_yellow_light = (255, 208, 48, 255)
    c_yellow_dark = (185, 132, 0, 255)
    c_yellow_groove = (135, 92, 0, 255)

    # Frame, chassis, dark cast iron
    c_dark_iron = (38, 40, 44, 255)
    c_iron_light = (65, 68, 75, 255)
    c_iron_recess = (22, 23, 26, 255)
    c_iron_mid = (48, 51, 56, 255)

    # Crawler tracks
    c_track_belt = (28, 29, 32, 255)
    c_track_cleat = (52, 54, 58, 255)
    c_track_roller = (68, 72, 80, 255)
    c_sprocket_teeth = (95, 100, 110, 255)

    # Screed polished bottom heating plate & steel structure
    c_screed_plate = (160, 165, 175, 255)
    c_screed_heat_tint = (135, 120, 110, 255)
    c_screed_burnished = (185, 190, 200, 255)
    c_screed_body = (42, 44, 48, 255)

    # Steaming fresh hot mix asphalt
    c_asphalt_black = (20, 20, 22, 255)
    c_asphalt_granule = (40, 38, 36, 255)
    c_asphalt_sheen = (55, 52, 48, 255)
    c_asphalt_bitumen = (14, 14, 16, 255)

    # Hazard safety diagonal stripes (yellow & black)
    c_hazard_yellow = (255, 215, 0, 255)
    c_hazard_black = (18, 18, 18, 255)

    # Safety railings
    c_railing_yellow = (255, 195, 0, 255)
    c_railing_shadow = (175, 125, 0, 255)

    # Glass / windshield (tinted with alpha for translucent cutout)
    c_tinted_glass = (120, 190, 225, 255)  # opaque in atlas to avoid transparent hole check
    c_glass_frame = (25, 26, 28, 255)

    # Lighting & Controls
    c_led_white = (245, 250, 255, 255)
    c_led_frame = (30, 30, 32, 255)
    c_beacon_amber = (255, 150, 0, 255)
    c_beacon_bright = (255, 230, 80, 255)

    c_screen_bg = (15, 25, 35, 255)
    c_screen_dial = (40, 190, 230, 255)
    c_screen_green = (50, 220, 90, 255)
    c_seat_leather = (32, 33, 36, 255)

    # ── Background Fill for All Active UV Zones ───────────────────────────────
    # Fill the entire active area [0..512, 0..450] with base texture to guarantee
    # 100% coverage and zero transparent holes!
    draw.rectangle([0, 0, 512, 450], fill=c_dark_iron)

    # Helper function to paint a metallic panel
    def paint_panel(u, v, w, h, base_color, bevel_color=None, shadow_color=None):
        draw.rectangle([u, v, u + w, v + h], fill=base_color)
        if bevel_color:
            draw.line([(u, v), (u + w, v)], fill=bevel_color)
            draw.line([(u, v), (u, v + h)], fill=bevel_color)
        if shadow_color:
            draw.line([(u, v + h), (u + w, v + h)], fill=shadow_color)
            draw.line([(u + w, v), (u + w, v + h)], fill=shadow_color)

    # Helper for hazard warning stripes
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

    # Helper for louvers / ventilation slots
    def paint_louvers(u, v, w, h, pitch=4):
        draw.rectangle([u, v, u + w, v + h], fill=c_iron_recess)
        for y in range(v + 2, v + h - 2, pitch):
            draw.rectangle([u + 2, y, u + w - 2, y + 1], fill=c_iron_light)

    # ── Section 0: Chassis & Lower Spine [0..350, 0..74] ──────────────────────
    paint_panel(0, 0, 350, 74, c_dark_iron, c_iron_light, c_iron_recess)
    # Bolted flanges and structural ribs
    for x in range(10, 340, 24):
        draw.line([(x, 5), (x, 70)], fill=c_iron_mid)
        draw.point((x + 2, 8), fill=c_iron_light)
        draw.point((x + 2, 66), fill=c_iron_light)

    # Push roller beam & rollers (Zone 222..350, 0..20)
    paint_hazard_stripes(222, 0, 70, 10, stripe_w=6)
    paint_panel(294, 0, 24, 15, c_iron_mid, c_iron_light, c_iron_recess)
    paint_panel(320, 0, 24, 15, c_iron_mid, c_iron_light, c_iron_recess)

    # ── Section 1: Track Units [0..400, 75..156] ──────────────────────────────
    # Left track belt (u=0..350, v=75..115)
    for row_y in [75, 116]:
        draw.rectangle([0, row_y, 350, row_y + 40], fill=c_track_belt)
        # Grousers / rubber pad bars
        for x in range(0, 350, 5):
            draw.rectangle([x, row_y + 4, x + 3, row_y + 36], fill=c_track_cleat)
            draw.line([(x, row_y + 4), (x + 3, row_y + 4)], fill=c_iron_light)
            draw.line([(x, row_y + 36), (x + 3, row_y + 36)], fill=c_iron_recess)

    # Rollers & Sprockets (u=78..122, v=75..156)
    for base_v in [75, 116]:
        paint_panel(78, base_v, 44, 30, c_track_roller, c_iron_light, c_iron_recess)
        draw.ellipse([82, base_v + 4, 98, base_v + 20], fill=c_sprocket_teeth, outline=c_iron_light)
        draw.ellipse([102, base_v + 4, 118, base_v + 20], fill=c_sprocket_teeth, outline=c_iron_light)

    # ── Section 2: Hopper & Folding Wings [0..400, 157..189] ──────────────────
    # Left & Right wings (yellow outer finish)
    paint_panel(0, 157, 180, 32, c_yellow, c_yellow_light, c_yellow_dark)
    paint_panel(180, 157, 180, 32, c_yellow, c_yellow_light, c_yellow_dark)

    # Flared side plates with reinforced gussets
    for x in range(10, 170, 20):
        draw.line([(x, 160), (x, 186)], fill=c_yellow_groove)
        draw.line([(180 + x, 160), (180 + x, 186)], fill=c_yellow_groove)

    # Front rubber splash flaps
    draw.rectangle([140, 157, 164, 165], fill=c_track_belt)
    draw.rectangle([320, 157, 344, 165], fill=c_track_belt)

    # ── Section 3: Engine Hood & Cowling [0..400, 190..229] ───────────────────
    paint_panel(68, 190, 140, 39, c_yellow, c_yellow_light, c_yellow_dark)
    # Side louvers on engine hood (u=210..277, v=190..229)
    paint_louvers(210, 190, 33, 25, pitch=3)
    paint_louvers(244, 190, 33, 25, pitch=3)

    # Top hatch handles and locks
    paint_panel(278, 190, 54, 14, c_yellow_dark, c_yellow_light, c_yellow_groove)
    draw.rectangle([290, 194, 320, 198], fill=c_iron_light)

    # Air cleaner & Exhaust (u=334..390, v=190..210)
    paint_panel(334, 190, 18, 15, c_track_belt, c_iron_light, c_iron_recess)
    paint_panel(352, 190, 14, 10, c_iron_mid, c_iron_light, c_iron_recess)
    paint_panel(366, 190, 10, 20, c_iron_light, (180, 185, 195, 255), c_iron_recess)
    paint_panel(376, 190, 14, 8, c_iron_mid, c_iron_light, c_iron_recess)

    # Asphalt hot mix bed inside hopper (u=0..68, v=190..225)
    draw.rectangle([0, 190, 68, 225], fill=c_asphalt_black)
    for y in range(190, 225, 2):
        for x in range(0, 68, 3):
            val = (x * 7 + y * 13) % 20
            if val < 5:
                draw.point((x, y), fill=c_asphalt_granule)
            elif val > 16:
                draw.point((x, y), fill=c_asphalt_sheen)

    # ── Section 4: Operator Platform, Railings, Ladders [0..400, 230..254] ─────
    # Walkway deck & diamond tread pattern
    paint_panel(0, 230, 175, 24, c_dark_iron, c_iron_light, c_iron_recess)
    for y in range(232, 250, 4):
        for x in range(2, 170, 6):
            draw.line([(x, y), (x + 2, y + 2)], fill=c_iron_light)
            draw.line([(x + 2, y + 2), (x + 4, y)], fill=c_iron_recess)

    # Access ladders
    paint_panel(176, 230, 32, 20, c_yellow, c_yellow_light, c_yellow_dark)
    for y in range(234, 248, 4):
        draw.rectangle([178, y, 206, y + 1], fill=c_iron_mid)

    # Yellow safety railings
    draw.rectangle([208, 230, 370, 254], fill=c_railing_yellow)
    for y in range(230, 254, 4):
        draw.line([(208, y), (370, y)], fill=c_railing_shadow)

    # ── Section 5: Seats & Dual ErgoPlus Consoles [0..300, 255..274] ──────────
    # Swivel seat pedestals & cushions
    paint_panel(0, 255, 180, 20, c_seat_leather, (55, 58, 62, 255), (18, 19, 21, 255))
    # Add yellow contrast stitching
    for x in range(5, 175, 12):
        draw.line([(x, 258), (x + 2, 258)], fill=c_yellow_dark)
        draw.line([(x, 268), (x + 2, 268)], fill=c_yellow_dark)

    # ErgoPlus Control Desks (u=184..296, v=255..274)
    paint_panel(184, 255, 112, 20, c_iron_recess, c_iron_light, c_dark_iron)
    # Glowing digital displays
    draw.rectangle([220, 256, 238, 266], fill=c_screen_bg, outline=c_iron_light)
    draw.ellipse([223, 258, 230, 264], outline=c_screen_dial)
    draw.line([(226, 261), (229, 259)], fill=c_screen_green)
    draw.point((234, 259), fill=c_screen_green)
    draw.point((234, 262), fill=(255, 60, 60, 255))

    draw.rectangle([276, 256, 294, 266], fill=c_screen_bg, outline=c_iron_light)
    draw.ellipse([279, 258, 286, 264], outline=c_screen_dial)
    draw.line([(282, 261), (285, 259)], fill=c_screen_green)

    # ── Section 6: Canopy Roof & LED Lights [0..300, 275..309] ────────────────
    # Canopy fiberglass roof panel
    paint_panel(40, 275, 95, 34, c_yellow, c_yellow_light, c_yellow_dark)
    # Molded roof drip gutters
    draw.rectangle([45, 277, 130, 305], outline=c_yellow_groove)

    # Windshield glass (tinted with frame)
    paint_panel(136, 275, 45, 20, c_tinted_glass, (180, 230, 255, 255), c_glass_frame)

    # LED headlight bar and lenses
    paint_panel(182, 275, 68, 15, c_led_frame, c_iron_light, c_dark_iron)
    draw.rectangle([186, 278, 204, 286], fill=c_led_white, outline=c_iron_light)
    draw.rectangle([224, 278, 242, 286], fill=c_led_white, outline=c_iron_light)

    # Amber rotating beacon
    paint_panel(252, 275, 24, 20, c_beacon_amber, c_beacon_bright, (180, 90, 0, 255))
    for y in range(278, 292, 2):
        draw.line([(266, y), (274, y)], fill=c_beacon_bright)

    # ── Section 7: Spreader Augers [0..200, 310..329] ─────────────────────────
    paint_panel(0, 310, 200, 20, c_iron_mid, c_iron_light, c_iron_recess)
    # Helical blade spiral pattern
    for x in range(18, 195, 8):
        draw.line([(x, 312), (x + 4, 326)], fill=c_iron_light, width=2)
        draw.line([(x + 4, 326), (x + 8, 312)], fill=c_iron_recess, width=1)

    # ── Section 8: Screed Assembly [0..300, 330..415] ─────────────────────────
    # Tow Arms (u=0..90, v=330..355)
    paint_panel(0, 330, 90, 25, c_yellow, c_yellow_light, c_yellow_dark)
    # Tow arm pivot scale decals
    draw.rectangle([20, 335, 45, 345], fill=c_dark_iron)
    for x in range(22, 44, 3):
        draw.line([(x, 337), (x, 343)], fill=(255, 255, 255, 255))

    # Screed heating bottom plate (Burnished steel with heat tint)
    paint_panel(0, 360, 114, 25, c_screed_plate, c_screed_burnished, c_iron_recess)
    for x in range(5, 110, 8):
        draw.line([(x, 362), (x + 6, 382)], fill=c_screed_heat_tint)

    # Screed upper body & tamper frame
    paint_panel(114, 360, 112, 25, c_screed_body, c_iron_light, c_iron_recess)

    # Screed end gates with hazard warning stripes!
    paint_hazard_stripes(244, 360, 23, 25, stripe_w=5)
    paint_hazard_stripes(268, 360, 23, 25, stripe_w=5)

    # Screed rear operator catwalk with expanded metal grid
    paint_panel(0, 390, 102, 20, c_dark_iron, c_iron_light, c_iron_recess)
    for y in range(392, 408, 3):
        for x in range(2, 100, 4):
            draw.point((x, y), fill=c_iron_light)

    # Screed railing
    draw.rectangle([102, 390, 192, 410], fill=c_railing_yellow)
    for y in range(390, 410, 4):
        draw.line([(102, y), (192, y)], fill=c_railing_shadow)

    # Screed leveling control boxes
    paint_panel(194, 390, 24, 20, c_iron_recess, c_iron_light, c_dark_iron)
    draw.rectangle([197, 394, 204, 404], fill=c_screen_bg, outline=c_screen_green)
    draw.rectangle([209, 394, 216, 404], fill=c_screen_bg, outline=c_screen_green)

    # ── Section 9: Branding & Safety Markings [0..512, 420..460] ──────────────
    draw.rectangle([0, 420, 512, 460], fill=c_yellow)
    # Long hazard stripe border
    paint_hazard_stripes(0, 425, 512, 12, stripe_w=8)

    # Save Entity Texture
    entity_tex_path = os.path.join(output_dir_entity, "asphalt_paver.png")
    img.save(entity_tex_path, "PNG")
    print(f"Generated 512x512 entity texture: {entity_tex_path}")

    # ── Generate Item Icon (32x32) ─────────────────────────────────────────────
    # Detailed 3D isometric representation of the Asphalt Paver
    icon = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    i_draw = ImageDraw.Draw(icon)

    # Ground shadow
    i_draw.ellipse([4, 24, 28, 30], fill=(0, 0, 0, 70))

    # Crawler tracks (bottom)
    i_draw.rectangle([5, 21, 27, 26], fill=(30, 32, 36, 255))
    for x in range(6, 27, 3):
        i_draw.point((x, 22), fill=(60, 64, 70, 255))
        i_draw.point((x, 25), fill=(18, 19, 22, 255))

    # Rear screed plate
    i_draw.rectangle([3, 23, 7, 27], fill=(160, 165, 175, 255))
    i_draw.rectangle([3, 20, 6, 23], fill=(240, 180, 0, 255))

    # Main machine body / engine hood
    i_draw.rectangle([8, 13, 22, 22], fill=(242, 178, 0, 255))
    i_draw.line([(8, 13), (22, 13)], fill=(255, 210, 50, 255))
    # Louver slats on side
    for y in [16, 18, 20]:
        i_draw.line([(10, y), (17, y)], fill=(180, 125, 0, 255))

    # Front hopper wings (flared open at front right)
    i_draw.polygon([(20, 15), (28, 13), (29, 21), (21, 22)], fill=(235, 170, 0, 255))
    i_draw.polygon([(20, 16), (27, 14), (28, 20), (21, 21)], fill=(25, 25, 28, 255)) # black asphalt inside hopper!
    # Front push rollers
    i_draw.rectangle([28, 20, 30, 23], fill=(50, 52, 58, 255))

    # Operator canopy & pillars
    i_draw.line([(10, 13), (10, 6)], fill=(40, 42, 46, 255))
    i_draw.line([(19, 13), (19, 6)], fill=(40, 42, 46, 255))
    i_draw.line([(11, 10), (18, 10)], fill=(255, 195, 0, 255)) # yellow handrail
    # Operator seat silhouette
    i_draw.rectangle([13, 9, 16, 13], fill=(30, 30, 34, 255))

    # Roof cover
    i_draw.rectangle([8, 5, 21, 7], fill=(245, 185, 0, 255))
    i_draw.line([(8, 5), (21, 5)], fill=(255, 225, 70, 255))
    # Amber beacon
    i_draw.point((14, 4), fill=(255, 160, 0, 255))
    i_draw.point((15, 4), fill=(255, 230, 80, 255))

    # Exhaust pipe
    i_draw.line([(17, 13), (17, 3)], fill=(70, 72, 78, 255))
    i_draw.point((17, 2), fill=(100, 104, 112, 255))

    item_tex_path = os.path.join(output_dir_item, "asphalt_paver.png")
    icon.save(item_tex_path, "PNG")
    print(f"Generated 32x32 item icon: {item_tex_path}")

if __name__ == "__main__":
    create_paver_textures()
