import os
import json

base = "src/main/resources/assets/pw_groundworks_asphalt"
models_block = os.path.join(base, "models/block")
models_item = os.path.join(base, "models/item")
items_dir = os.path.join(base, "items")
blockstates_dir = os.path.join(base, "blockstates")

for d in [models_block, models_item, items_dir, blockstates_dir]:
    os.makedirs(d, exist_ok=True)

def write_json(path, data):
    with open(path, "w") as f:
        json.dump(data, f, indent=2)

# --- Block Models ---
write_json(os.path.join(models_block, "fresh_asphalt.json"), {
    "parent": "minecraft:block/cube_all",
    "textures": {
        "all": "pw_groundworks_asphalt:block/fresh_asphalt"
    }
})

write_json(os.path.join(models_block, "compacted_asphalt.json"), {
    "parent": "minecraft:block/cube_all",
    "textures": {
        "all": "pw_groundworks_asphalt:block/compacted_asphalt"
    }
})

write_json(os.path.join(models_block, "asphalt_slab.json"), {
    "parent": "minecraft:block/slab",
    "textures": {
        "bottom": "pw_groundworks_asphalt:block/compacted_asphalt",
        "top": "pw_groundworks_asphalt:block/compacted_asphalt",
        "side": "pw_groundworks_asphalt:block/compacted_asphalt"
    }
})

write_json(os.path.join(models_block, "asphalt_slab_top.json"), {
    "parent": "minecraft:block/slab_top",
    "textures": {
        "bottom": "pw_groundworks_asphalt:block/compacted_asphalt",
        "top": "pw_groundworks_asphalt:block/compacted_asphalt",
        "side": "pw_groundworks_asphalt:block/compacted_asphalt"
    }
})

write_json(os.path.join(models_block, "asphalt_white_line.json"), {
    "parent": "minecraft:block/cube",
    "textures": {
        "particle": "pw_groundworks_asphalt:block/compacted_asphalt",
        "down": "pw_groundworks_asphalt:block/compacted_asphalt",
        "up": "pw_groundworks_asphalt:block/asphalt_white_line",
        "north": "pw_groundworks_asphalt:block/compacted_asphalt",
        "south": "pw_groundworks_asphalt:block/compacted_asphalt",
        "west": "pw_groundworks_asphalt:block/compacted_asphalt",
        "east": "pw_groundworks_asphalt:block/compacted_asphalt"
    }
})

write_json(os.path.join(models_block, "asphalt_yellow_line.json"), {
    "parent": "minecraft:block/cube",
    "textures": {
        "particle": "pw_groundworks_asphalt:block/compacted_asphalt",
        "down": "pw_groundworks_asphalt:block/compacted_asphalt",
        "up": "pw_groundworks_asphalt:block/asphalt_yellow_line",
        "north": "pw_groundworks_asphalt:block/compacted_asphalt",
        "south": "pw_groundworks_asphalt:block/compacted_asphalt",
        "west": "pw_groundworks_asphalt:block/compacted_asphalt",
        "east": "pw_groundworks_asphalt:block/compacted_asphalt"
    }
})

# --- Blockstates ---
write_json(os.path.join(blockstates_dir, "fresh_asphalt.json"), {
    "variants": {
        "": { "model": "pw_groundworks_asphalt:block/fresh_asphalt" }
    }
})

write_json(os.path.join(blockstates_dir, "compacted_asphalt.json"), {
    "variants": {
        "": { "model": "pw_groundworks_asphalt:block/compacted_asphalt" }
    }
})

write_json(os.path.join(blockstates_dir, "asphalt_slab.json"), {
    "variants": {
        "type=bottom": { "model": "pw_groundworks_asphalt:block/asphalt_slab" },
        "type=top": { "model": "pw_groundworks_asphalt:block/asphalt_slab_top" },
        "type=double": { "model": "pw_groundworks_asphalt:block/compacted_asphalt" }
    }
})

write_json(os.path.join(blockstates_dir, "asphalt_white_line.json"), {
    "variants": {
        "facing=north": { "model": "pw_groundworks_asphalt:block/asphalt_white_line" },
        "facing=south": { "model": "pw_groundworks_asphalt:block/asphalt_white_line" },
        "facing=east": { "model": "pw_groundworks_asphalt:block/asphalt_white_line", "y": 90 },
        "facing=west": { "model": "pw_groundworks_asphalt:block/asphalt_white_line", "y": 90 }
    }
})

write_json(os.path.join(blockstates_dir, "asphalt_yellow_line.json"), {
    "variants": {
        "facing=north": { "model": "pw_groundworks_asphalt:block/asphalt_yellow_line" },
        "facing=south": { "model": "pw_groundworks_asphalt:block/asphalt_yellow_line" },
        "facing=east": { "model": "pw_groundworks_asphalt:block/asphalt_yellow_line", "y": 90 },
        "facing=west": { "model": "pw_groundworks_asphalt:block/asphalt_yellow_line", "y": 90 }
    }
})

# --- Items & Item Models ---
block_items = [
    "fresh_asphalt",
    "compacted_asphalt",
    "asphalt_slab",
    "asphalt_white_line",
    "asphalt_yellow_line"
]

for item in block_items:
    write_json(os.path.join(models_item, f"{item}.json"), {
        "parent": f"pw_groundworks_asphalt:block/{item}"
    })
    write_json(os.path.join(items_dir, f"{item}.json"), {
        "model": {
            "type": "minecraft:model",
            "model": f"pw_groundworks_asphalt:item/{item}"
        }
    })

generated_items = [
    "asphalt_mix",
    "cat_grader",
    "asphalt_paver",
    "road_roller"
]

for item in generated_items:
    write_json(os.path.join(models_item, f"{item}.json"), {
        "parent": "minecraft:item/generated",
        "textures": {
            "layer0": f"pw_groundworks_asphalt:item/{item}"
        }
    })
    write_json(os.path.join(items_dir, f"{item}.json"), {
        "model": {
            "type": "minecraft:model",
            "model": f"pw_groundworks_asphalt:item/{item}"
        }
    })

print("JSON models, items, blockstates generated successfully!")
