import os
import random
from PIL import Image

random.seed(42)

block_dir = "src/main/resources/assets/pw_groundworks_asphalt/textures/block"
item_dir = "src/main/resources/assets/pw_groundworks_asphalt/textures/item"
os.makedirs(block_dir, exist_ok=True)
os.makedirs(item_dir, exist_ok=True)

# 1. Fresh Asphalt: rough, uncompacted hot asphalt with dark bitumen and mineral aggregate flecks
fresh_img = Image.new("RGBA", (16, 16))
fresh_base = [(30, 31, 34), (24, 25, 27), (36, 37, 40), (18, 19, 21), (42, 43, 46)]
for y in range(16):
    for x in range(16):
        c = random.choice(fresh_base)
        # occasional lighter stone chip fleck
        if random.random() < 0.12:
            c = (55, 57, 62)
        elif random.random() < 0.05:
            c = (68, 70, 75)
        fresh_img.putpixel((x, y), (*c, 255))
fresh_img.save(os.path.join(block_dir, "fresh_asphalt.png"))

# 2. Compacted Asphalt: smooth, rolled, dense roadway pavement
compact_img = Image.new("RGBA", (16, 16))
compact_base = [(46, 48, 52), (42, 44, 48), (50, 52, 56), (38, 40, 44), (48, 50, 54)]
for y in range(16):
    for x in range(16):
        c = random.choice(compact_base)
        if random.random() < 0.08:
            c = (58, 60, 64)
        compact_img.putpixel((x, y), (*c, 255))
compact_img.save(os.path.join(block_dir, "compacted_asphalt.png"))

# 3. Asphalt White Line: compacted asphalt with a clean vertical road marking stripe in center (cols 6-9)
white_img = compact_img.copy()
white_colors = [(240, 240, 245), (225, 228, 232), (210, 212, 218), (250, 250, 255)]
for y in range(16):
    for x in range(6, 10):
        c = random.choice(white_colors)
        if x in (6, 9) and random.random() < 0.25:
            # slightly textured worn edge
            c = (150, 155, 165)
        white_img.putpixel((x, y), (*c, 255))
white_img.save(os.path.join(block_dir, "asphalt_white_line.png"))

# 4. Asphalt Yellow Line: compacted asphalt with a bright amber road marking stripe in center (cols 6-9)
yellow_img = compact_img.copy()
yellow_colors = [(245, 185, 30), (235, 175, 25), (255, 195, 45), (220, 160, 20)]
for y in range(16):
    for x in range(6, 10):
        c = random.choice(yellow_colors)
        if x in (6, 9) and random.random() < 0.25:
            c = (170, 130, 25)
        yellow_img.putpixel((x, y), (*c, 255))
yellow_img.save(os.path.join(block_dir, "asphalt_yellow_line.png"))

# 5. Asphalt Mix (Item): a pile / lump of hot asphalt mineral mix
mix_img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
# create a clump shape around center
for y in range(16):
    for x in range(16):
        # distance from bottom center (8, 11)
        dx = x - 8
        dy = (y - 11) * 1.5
        dist = (dx*dx + dy*dy)**0.5
        if dist < 5.5:
            c = random.choice([(25, 26, 28), (35, 36, 39), (20, 21, 23), (48, 50, 54)])
            if dist < 2.0 and random.random() < 0.3:
                c = (70, 72, 78) # glossy highlight
            mix_img.putpixel((x, y), (*c, 255))
        elif dist < 6.8 and random.random() < 0.4:
            c = random.choice([(22, 23, 25), (32, 33, 36)])
            mix_img.putpixel((x, y), (*c, 255))
mix_img.save(os.path.join(item_dir, "asphalt_mix.png"))

print("Generated all block and item textures successfully!")
