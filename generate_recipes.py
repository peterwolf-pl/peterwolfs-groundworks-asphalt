import os
import json

recipe_dir = "src/main/resources/data/pw_groundworks_asphalt/recipe"
os.makedirs(recipe_dir, exist_ok=True)

def write_json(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)

write_json(os.path.join(recipe_dir, "asphalt_mix.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        "SGS",
        "GCG",
        "BSB"
    ],
    "key": {
        "S": "minecraft:sand",
        "G": "minecraft:gravel",
        "C": "minecraft:coal",
        "B": "minecraft:cobblestone"
    },
    "result": {
        "id": "pw_groundworks_asphalt:asphalt_mix",
        "count": 8
    }
})

write_json(os.path.join(recipe_dir, "fresh_asphalt.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "building",
    "pattern": [
        "MM",
        "MM"
    ],
    "key": {
        "M": "pw_groundworks_asphalt:asphalt_mix"
    },
    "result": {
        "id": "pw_groundworks_asphalt:fresh_asphalt",
        "count": 4
    }
})

write_json(os.path.join(recipe_dir, "compacted_asphalt_smelting.json"), {
    "type": "minecraft:smelting",
    "category": "blocks",
    "ingredient": "pw_groundworks_asphalt:fresh_asphalt",
    "result": {
        "id": "pw_groundworks_asphalt:compacted_asphalt"
    },
    "experience": 0.1,
    "cookingtime": 200
})

write_json(os.path.join(recipe_dir, "asphalt_slab.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "building",
    "pattern": [
        "AAA"
    ],
    "key": {
        "A": "pw_groundworks_asphalt:compacted_asphalt"
    },
    "result": {
        "id": "pw_groundworks_asphalt:asphalt_slab",
        "count": 6
    }
})

write_json(os.path.join(recipe_dir, "asphalt_white_line.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "building",
    "pattern": [
        "AAA",
        "AWA",
        "AAA"
    ],
    "key": {
        "A": "pw_groundworks_asphalt:compacted_asphalt",
        "W": "minecraft:white_dye"
    },
    "result": {
        "id": "pw_groundworks_asphalt:asphalt_white_line",
        "count": 8
    }
})

write_json(os.path.join(recipe_dir, "asphalt_yellow_line.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "building",
    "pattern": [
        "AAA",
        "AYA",
        "AAA"
    ],
    "key": {
        "A": "pw_groundworks_asphalt:compacted_asphalt",
        "Y": "minecraft:yellow_dye"
    },
    "result": {
        "id": "pw_groundworks_asphalt:asphalt_yellow_line",
        "count": 8
    }
})

write_json(os.path.join(recipe_dir, "road_roller.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " Y ",
        "IRI",
        "SSS"
    ],
    "key": {
        "Y": "minecraft:yellow_concrete",
        "I": "minecraft:iron_block",
        "R": "minecraft:redstone_block",
        "S": "minecraft:smooth_stone"
    },
    "result": {
        "id": "pw_groundworks_asphalt:road_roller",
        "count": 1
    }
})

write_json(os.path.join(recipe_dir, "asphalt_paver.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        "HYH",
        "IRI",
        "BIB"
    ],
    "key": {
        "H": "minecraft:hopper",
        "Y": "minecraft:yellow_concrete",
        "I": "minecraft:iron_block",
        "R": "minecraft:redstone_block",
        "B": "minecraft:iron_bars"
    },
    "result": {
        "id": "pw_groundworks_asphalt:asphalt_paver",
        "count": 1
    }
})

write_json(os.path.join(recipe_dir, "cat_grader.json"), {
    "type": "minecraft:crafting_shaped",
    "category": "misc",
    "pattern": [
        " Y ",
        "IRI",
        "WBW"
    ],
    "key": {
        "Y": "minecraft:yellow_concrete",
        "I": "minecraft:iron_block",
        "R": "minecraft:redstone_block",
        "W": "minecraft:black_wool",
        "B": "minecraft:iron_bars"
    },
    "result": {
        "id": "pw_groundworks_asphalt:cat_grader",
        "count": 1
    }
})

print("Recipes created successfully!")
