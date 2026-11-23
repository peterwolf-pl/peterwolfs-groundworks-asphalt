#!/usr/bin/env python3
"""
Full Road Roller Model & Texture Generator Builder with Zero Coplanar Conflicts.
"""

import os
import sys
import math
from dataclasses import dataclass, field
from typing import List, Dict

@dataclass
class Box:
    part_name: str
    parent_var: str
    rx: float
    ry: float
    rz: float
    rdx: float
    rdy: float
    rdz: float
    mat: str
    desc: str
    u: int = 0
    v: int = 0

    @property
    def req_w(self):
        w = max(1, math.ceil(abs(self.rdx)))
        d = max(1, math.ceil(abs(self.rdz)))
        return 2 * (d + w)

    @property
    def req_h(self):
        h = max(1, math.ceil(abs(self.rdy)))
        d = max(1, math.ceil(abs(self.rdz)))
        return d + h

@dataclass
class Part:
    name: str
    parent_var: str
    var_name: str
    offset: tuple
    boxes: List[Box] = field(default_factory=list)

def create_model_and_textures():
    parts: Dict[str, Part] = {}

    def get_part(name: str, parent_var: str, var_name: str, offset: tuple) -> Part:
        if name not in parts:
            parts[name] = Part(name, parent_var, var_name, offset)
        return parts[name]

    def add_b(part_name: str, parent_var: str, var_name: str, offset: tuple,
              rx, ry, rz, rdx, rdy, rdz, mat, desc):
        p = get_part(part_name, parent_var, var_name, offset)
        b = Box(part_name, parent_var, round(rx, 3), round(ry, 3), round(rz, 3),
                round(rdx, 3), round(rdy, 3), round(rdz, 3), mat, desc)
        p.boxes.append(b)

    # ==========================================
    # 1. REAR CHASSIS & AXLE & COUNTERWEIGHT
    # ==========================================
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -11.0, -2.0, -46.0, 5.0, 8.0, 44.0, "chassis_dark", "Left chassis longitudinal beam")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          6.0, -2.0, -46.0, 5.0, 8.0, 44.0, "chassis_dark", "Right chassis longitudinal beam")

    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -6.0, -1.96, -45.96, 12.0, 7.92, 6.0, "chassis_dark", "Rear cross member")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -6.0, -1.96, -22.0, 12.0, 7.92, 6.0, "chassis_dark", "Mid cross member")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -6.0, -1.96, -5.96, 12.0, 7.92, 3.92, "chassis_dark", "Front cross member")

    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -15.0, -2.05, -51.0, 30.0, 9.1, 4.95, "yellow_body", "Rear heavy ballast counterweight bumper")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -14.0, 7.07, -49.5, 28.0, 2.9, 3.4, "chassis_dark", "Rear bumper lower bevel guard")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -2.5, 0.5, -53.0, 5.0, 4.0, 1.95, "steel_dark", "Rear tow hitch eye block")

    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -4.5, 5.0, -33.5, 9.0, 6.0, 7.0, "chassis_dark", "Rear central differential carrier")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -16.0, 6.2, -31.8, 11.45, 3.6, 3.6, "chassis_dark", "Left rear axle trumpet tube")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          4.55, 6.2, -31.8, 11.45, 3.6, 3.6, "chassis_dark", "Right rear axle trumpet tube")

    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -3.5, -3.0, -2.0, 7.0, 2.9, 5.0, "chassis_dark", "Hitch upper ear")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -3.5, 4.1, -2.0, 7.0, 2.9, 5.0, "chassis_dark", "Hitch lower ear")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -1.2, -3.5, 0.8, 2.4, 11.0, 2.0, "steel_polished", "Vertical kingpin pivot pin")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -10.5, 0.5, -4.5, 3.0, 4.0, 4.0, "chassis_dark", "Left steer cylinder rear anchor")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          7.5, 0.5, -4.5, 3.0, 4.0, 4.0, "chassis_dark", "Right steer cylinder rear anchor")

    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          -13.5, 0.0, -51.1, 4.0, 2.5, 0.8, "tail_light", "Left tail light cluster")
    add_b("rear_chassis", "root", "rearChassis", (0.0, 0.0, 0.0),
          9.5, 0.0, -51.1, 4.0, 2.5, 0.8, "tail_light", "Right tail light cluster")

    # ==========================================
    # 2. ENGINE COMPARTMENT & AERODYNAMIC HOOD
    # ==========================================
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -12.0, -14.0, -45.8, 24.0, 11.9, 27.6, "yellow_body", "Engine lower compartment housing")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -11.5, -18.5, -34.0, 23.0, 4.45, 15.75, "yellow_body", "Bonnet top front slope")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -11.45, -16.5, -45.5, 22.9, 2.45, 11.45, "yellow_body", "Bonnet top rear slope")

    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -12.08, -12.0, -42.0, 0.6, 8.0, 14.0, "black_grille", "Left radiator intake louver panel")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          11.48, -12.0, -42.0, 0.6, 8.0, 14.0, "black_grille", "Right radiator intake louver panel")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -6.0, -18.56, -28.0, 12.0, 0.5, 7.0, "black_grille", "Top engine hot air exhaust grille")

    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          8.0, -24.0, -25.5, 2.5, 5.4, 2.5, "steel_dark", "Exhaust muffler canister")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          8.5, -32.0, -25.0, 1.5, 7.95, 1.5, "steel_dark", "Vertical exhaust tailpipe")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          8.2, -32.8, -25.8, 2.1, 0.75, 2.6, "steel_dark", "Exhaust hinged rain cap")

    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -10.2, -27.0, -25.0, 1.4, 8.4, 1.5, "black_plastic", "Air intake snorkel tube")
    add_b("engine_hood", "rear_chassis", "engineHood", (0.0, 0.0, 0.0),
          -11.0, -30.5, -26.0, 3.0, 3.45, 3.5, "black_plastic", "Cyclone air pre-cleaner canister")

    # ==========================================
    # 3. OPERATOR CABIN (ROPS/FOPS) & INTERIOR
    # ==========================================
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.5, -13.5, -18.0, 27.0, 2.0, 20.0, "chassis_dark", "Cab subframe platform floor")

    # Access steps (staggered slightly to avoid coplanar planes with spine and handrails)
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -17.5, -8.0, -11.0, 3.95, 12.0, 6.0, "chassis_dark", "Left access staircase spine")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -18.0, -8.5, -12.0, 4.42, 1.0, 7.95, "black_grille", "Left top step tread")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -18.0, 0.0, -12.0, 4.42, 1.0, 7.95, "black_grille", "Left lower step tread")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -18.5, -18.0, -12.06, 0.7, 10.4, 1.5, "yellow_safety", "Left stair yellow safety handrail front post")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -18.5, -18.0, -5.44, 0.7, 10.4, 1.48, "yellow_safety", "Left stair yellow safety handrail rear post")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          13.55, -8.0, -11.0, 3.95, 12.0, 6.0, "chassis_dark", "Right access staircase spine")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          13.58, -8.5, -12.0, 4.42, 1.0, 7.95, "black_grille", "Right top step tread")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          13.58, 0.0, -12.0, 4.42, 1.0, 7.95, "black_grille", "Right lower step tread")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          17.8, -18.0, -12.06, 0.7, 10.4, 1.5, "yellow_safety", "Right stair yellow safety handrail front post")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          17.8, -18.0, -5.44, 0.7, 10.4, 1.48, "yellow_safety", "Right stair yellow safety handrail rear post")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.0, -16.0, 0.5, 26.0, 2.45, 1.5, "yellow_body", "Front cab lower sill")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.0, -16.0, -18.0, 26.0, 2.45, 1.5, "yellow_body", "Rear cab lower sill")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.0, -16.0, -16.45, 1.5, 2.45, 16.9, "yellow_body", "Left door sill")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.5, -16.0, -16.45, 1.5, 2.45, 16.9, "yellow_body", "Right door sill")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.0, -35.0, 0.2, 2.0, 18.95, 1.8, "chassis_dark", "Front-left ROPS A-pillar")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.0, -35.0, 0.2, 2.0, 18.95, 1.8, "chassis_dark", "Front-right ROPS A-pillar")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.0, -35.0, -18.0, 2.0, 18.95, 1.8, "chassis_dark", "Rear-left ROPS C-pillar")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.0, -35.0, -18.0, 2.0, 18.95, 1.8, "chassis_dark", "Rear-right ROPS C-pillar")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -13.05, -35.0, -8.5, 1.6, 18.95, 2.0, "chassis_dark", "Left B-pillar door post")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.45, -35.0, -8.5, 1.6, 18.95, 2.0, "chassis_dark", "Right B-pillar door post")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -10.95, -34.8, 0.8, 21.9, 18.75, 1.0, "cab_glass", "Front curved windshield glass")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -10.95, -34.8, -17.8, 21.9, 18.75, 1.0, "cab_glass", "Rear panoramic window glass")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -12.8, -34.8, -6.45, 1.0, 18.75, 6.6, "cab_glass", "Left door window glass")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -12.8, -34.8, -16.15, 1.0, 18.75, 7.6, "cab_glass", "Left rear quarter glass")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.8, -34.8, -6.45, 1.0, 18.75, 6.6, "cab_glass", "Right door window glass")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          11.8, -34.8, -16.15, 1.0, 18.75, 7.6, "cab_glass", "Right rear quarter glass")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -14.0, -38.0, -19.0, 28.0, 2.95, 22.0, "yellow_body", "Main cab aerodynamic roof cap")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -8.0, -41.5, -14.0, 16.0, 3.45, 12.0, "chassis_dark", "Roof HVAC climate control pod")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -2.0, -44.5, -9.0, 4.0, 2.95, 4.0, "amber_beacon", "Emergency amber rotating beacon")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -10.0, -37.5, 3.05, 6.0, 2.0, 1.15, "led_light", "Left front roof LED work light")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          4.0, -37.5, 3.05, 6.0, 2.0, 1.15, "led_light", "Right front roof LED work light")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -10.0, -37.5, -19.8, 6.0, 2.0, 0.75, "led_light", "Left rear roof LED work light")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          4.0, -37.5, -19.8, 6.0, 2.0, 0.75, "led_light", "Right rear roof LED work light")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -1.0, -26.0, 1.85, 8.0, 1.5, 0.6, "black_plastic", "Front windshield wiper arm and blade")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -16.5, -32.5, 0.5, 3.45, 1.5, 1.5, "chassis_dark", "Left mirror stalk")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -18.5, -35.0, 0.0, 1.95, 7.0, 2.5, "mirror_glass", "Left panoramic mirror head")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          13.05, -32.5, 0.5, 3.45, 1.5, 1.5, "chassis_dark", "Right mirror stalk")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          16.55, -35.0, 0.0, 1.95, 7.0, 2.5, "mirror_glass", "Right panoramic mirror head")

    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -3.5, -18.0, -11.0, 7.0, 4.45, 7.0, "chassis_dark", "Operator seat pedestal base")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -4.0, -20.5, -11.5, 8.0, 2.45, 8.0, "seat_black", "Operator ergonomic seat cushion")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -3.5, -30.0, -11.5, 7.0, 9.45, 2.5, "seat_black", "Operator seat high backrest")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -2.5, -33.5, -11.2, 5.0, 3.45, 1.9, "seat_black", "Operator seat headrest")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -5.0, -23.5, -9.0, 0.95, 1.5, 5.0, "seat_black", "Left armrest")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          4.05, -23.5, -9.0, 0.95, 1.5, 5.0, "seat_black", "Right armrest")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -2.5, -23.0, -2.0, 5.0, 9.48, 3.0, "chassis_dark", "Steering column pedestal")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -4.5, -25.5, -3.5, 9.0, 2.45, 4.5, "black_plastic", "Dashboard digital instrument display")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          -3.5, -26.5, -2.5, 7.0, 0.95, 3.0, "black_plastic", "Steering wheel")
    add_b("cab", "rear_chassis", "cab", (0.0, 0.0, 0.0),
          4.2, -22.5, -7.5, 2.3, 1.5, 4.5, "black_plastic", "Right joystick control console")

    # ==========================================
    # 4. FRONT ASSEMBLY & DRUM YOKE & SCRAPERS
    # ==========================================
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -3.0, 0.0, 0.5, 6.0, 4.0, 9.0, "chassis_dark", "Front hitch articulation tongue")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -2.5, -0.5, 1.0, 5.0, 5.0, 3.0, "chassis_dark", "Tongue pivot sleeve")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -8.5, 1.5, 1.0, 2.0, 2.0, 7.5, "chassis_dark", "Left steering cylinder barrel")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -7.8, 2.1, -2.5, 0.6, 0.8, 3.45, "steel_polished", "Left steering chrome piston rod")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          6.5, 1.5, 1.0, 2.0, 2.0, 7.5, "chassis_dark", "Right steering cylinder barrel")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          7.2, 2.1, -2.5, 0.6, 0.8, 3.45, "steel_polished", "Right steering chrome piston rod")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -18.0, 3.0, 9.5, 36.0, 6.0, 5.45, "yellow_body", "Yoke rear cross beam")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -8.0, -1.0, 10.0, 16.0, 3.95, 4.5, "yellow_body", "Yoke central reinforcement arch")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -19.5, 3.0, 14.95, 3.0, 9.0, 28.5, "yellow_body", "Left yoke longitudinal side arm")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -20.5, 5.0, 25.0, 0.95, 6.0, 6.0, "chassis_dark", "Left drum axle bearing housing")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -17.5, 4.05, 24.05, 1.25, 7.9, 7.9, "black_rubber", "Left drum rubber vibration isolator mounts")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          16.5, 3.0, 14.95, 3.0, 9.0, 28.5, "yellow_body", "Right yoke longitudinal side arm")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          19.55, 5.0, 25.0, 0.95, 6.0, 6.0, "chassis_dark", "Right drum axle bearing housing")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          16.25, 4.05, 24.05, 1.25, 7.9, 7.9, "black_rubber", "Right drum rubber vibration isolator mounts")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          18.5, 5.5, 25.5, 3.5, 5.0, 5.0, "chassis_dark", "Hydraulic vibration exciter motor")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -18.0, 3.0, 43.45, 36.0, 6.0, 5.6, "yellow_body", "Yoke front cross beam bumper")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -16.2, 10.5, 41.8, 32.4, 2.0, 0.5, "steel_dark", "Front mud scraper blade")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -16.8, 6.0, 41.8, 1.0, 5.0, 1.65, "chassis_dark", "Front mud scraper left spring bracket")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          15.8, 6.0, 41.8, 1.0, 5.0, 1.65, "chassis_dark", "Front mud scraper right spring bracket")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -16.2, 10.5, 15.48, 32.4, 2.0, 1.0, "steel_dark", "Rear mud scraper blade")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -16.8, 6.0, 15.0, 1.0, 5.0, 3.0, "chassis_dark", "Rear mud scraper left spring bracket")
    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          15.8, 6.0, 15.0, 1.0, 5.0, 3.0, "chassis_dark", "Rear mud scraper right spring bracket")

    add_b("front_assembly", "root", "frontAssembly", (0.0, 0.0, 0.0),
          -16.0, 2.0, 42.0, 32.0, 1.2, 1.2, "steel_dark", "Water spray manifold bar")

    # ==========================================
    # 5. FRONT VIBRATING STEEL DRUM (`drum`)
    # Offset: (0.0, 8.0, 28.0)
    # ==========================================
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -16.0, -13.5, -5.6, 32.0, 27.0, 11.2, "drum_steel", "Drum vertical cylinder core")
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -15.95, -5.65, -13.5, 31.9, 11.3, 27.0, "drum_steel", "Drum horizontal cylinder core")
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -15.9, -11.55, -9.55, 31.8, 23.1, 19.1, "drum_steel", "Drum diagonal 1 cylinder facet")
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -15.85, -9.6, -11.5, 31.7, 19.2, 23.0, "drum_steel", "Drum diagonal 2 cylinder facet")

    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -16.06, -12.5, -12.5, 0.5, 25.0, 25.0, "drum_disc", "Left welded heavy drum end plate")
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          15.56, -12.5, -12.5, 0.5, 25.0, 25.0, "drum_disc", "Right welded heavy drum end plate")

    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          -16.5, -3.5, -3.5, 0.44, 7.0, 7.0, "chassis_dark", "Left drum axle hub boss")
    add_b("drum", "front_assembly", "drum", (0.0, 8.0, 28.0),
          16.06, -3.5, -3.5, 0.44, 7.0, 7.0, "chassis_dark", "Right drum axle hub boss")

    # ==========================================
    # 6. REAR WHEELS & TIRES & CHEVRON TREADS
    # ==========================================
    # Left wheel: offset (-18.5F, 8.0F, -30.0F)
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -5.5, -14.0, -6.0, 11.0, 28.0, 12.0, "tire_rubber", "Left wheel vertical carcass")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -5.45, -6.05, -14.0, 10.9, 12.1, 28.0, "tire_rubber", "Left wheel horizontal carcass")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -5.4, -12.05, -10.05, 10.8, 24.1, 20.1, "tire_rubber", "Left wheel diagonal 1 carcass")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -5.35, -10.1, -12.0, 10.7, 20.2, 24.0, "tire_rubber", "Left wheel diagonal 2 carcass")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -5.56, -8.0, -8.0, 1.8, 16.0, 16.0, "wheel_rim", "Left wheel steel rim disc")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -7.5, -4.0, -4.0, 1.93, 8.0, 8.0, "chassis_dark", "Left wheel planetary hub")
    add_b("rear_left_wheel", "rear_chassis", "rearLeftWheel", (-18.5, 8.0, -30.0),
          -8.5, -2.0, -2.0, 0.98, 4.0, 4.0, "steel_dark", "Left wheel planetary hub center cap")

    # Right wheel: offset (18.5F, 8.0F, -30.0F)
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          -5.5, -14.0, -6.0, 11.0, 28.0, 12.0, "tire_rubber", "Right wheel vertical carcass")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          -5.45, -6.05, -14.0, 10.9, 12.1, 28.0, "tire_rubber", "Right wheel horizontal carcass")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          -5.4, -12.05, -10.05, 10.8, 24.1, 20.1, "tire_rubber", "Right wheel diagonal 1 carcass")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          -5.35, -10.1, -12.0, 10.7, 20.2, 24.0, "tire_rubber", "Right wheel diagonal 2 carcass")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          3.76, -8.0, -8.0, 1.8, 16.0, 16.0, "wheel_rim", "Right wheel steel rim disc")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          5.57, -4.0, -4.0, 1.93, 8.0, 8.0, "chassis_dark", "Right wheel planetary hub")
    add_b("rear_right_wheel", "rear_chassis", "rearRightWheel", (18.5, 8.0, -30.0),
          7.52, -2.0, -2.0, 0.98, 4.0, 4.0, "steel_dark", "Right wheel planetary hub center cap")

    return parts
