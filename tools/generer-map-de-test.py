#!/usr/bin/env python3
"""Génère la structure de la map de test du mod Dungeon Defenders.

Écrit un `.nbt` de structure Minecraft (NBT gzippé) sans passer par le jeu. Le format a été
relu depuis `StructureTemplate.java` et vérifié sur la structure `gametest/empty.nbt` déjà
présente dans le dépôt :

    { DataVersion: int, size: [3 ints], palette: [{Name, Properties?}], blocks: [{state,pos,nbt?}], entities: [] }
"""
import gzip
import struct

DATA_VERSION = 4790  # relevé sur gametest/empty.nbt, écrit par cette version de Minecraft

# --- Écriture NBT -------------------------------------------------------------------------

TAG_END, TAG_BYTE, TAG_INT, TAG_FLOAT, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 0, 1, 3, 5, 8, 9, 10, 11


class Int(int):
    """Marqueur : entier NBT (TAG_Int) plutôt que déduit du type Python."""


class Float(float):
    pass


class IntArray(list):
    """Marqueur : TAG_Int_Array. NON utilisé pour "size"/"pos" — vérifié dans
    StructureTemplate#save, qui les écrit en TAG_List d'entiers (newIntegerList) et les relit
    avec getListOrEmpty. Un TAG_Int_Array y serait lu comme une liste vide : structure de taille
    0 et tous les blocs empilés à l'origine, sans le moindre message d'erreur."""


def tag_of(value):
    if isinstance(value, IntArray):
        return TAG_INT_ARRAY
    if isinstance(value, bool):
        return TAG_BYTE
    if isinstance(value, Float):
        return TAG_FLOAT
    if isinstance(value, int):
        return TAG_INT
    if isinstance(value, str):
        return TAG_STRING
    if isinstance(value, list):
        return TAG_LIST
    if isinstance(value, dict):
        return TAG_COMPOUND
    raise TypeError(type(value))


def write_string(out, text):
    encoded = text.encode("utf-8")
    out += struct.pack(">H", len(encoded)) + encoded
    return out


def write_payload(out, value):
    tag = tag_of(value)
    if tag == TAG_BYTE:
        return out + struct.pack(">b", 1 if value else 0)
    if tag == TAG_INT:
        return out + struct.pack(">i", value)
    if tag == TAG_FLOAT:
        return out + struct.pack(">f", value)
    if tag == TAG_STRING:
        return write_string(out, value)
    if tag == TAG_INT_ARRAY:
        out += struct.pack(">i", len(value))
        for item in value:
            out += struct.pack(">i", item)
        return out
    if tag == TAG_LIST:
        # Une liste NBT est homogène ; une liste vide se note TAG_End.
        item_tag = tag_of(value[0]) if value else TAG_END
        out += struct.pack(">bi", item_tag, len(value))
        for item in value:
            out = write_payload(out, item)
        return out
    if tag == TAG_COMPOUND:
        for name, item in value.items():
            out += struct.pack(">b", tag_of(item))
            out = write_string(out, name)
            out = write_payload(out, item)
        return out + struct.pack(">b", TAG_END)
    raise TypeError(tag)


def write_nbt(root, name=""):
    out = struct.pack(">b", TAG_COMPOUND)
    out = write_string(out, name)
    return write_payload(out, root)


# --- Construction de la map ---------------------------------------------------------------

WIDTH, DEPTH = 49, 49          # impair : un centre franc
FLOOR_Y, WALL_HEIGHT = 0, 5    # le sol est la couche la plus basse de la structure
HEIGHT = FLOOR_Y + WALL_HEIGHT + 1

palette, palette_index = [], {}


def state(name, properties=None):
    """Ajoute (ou retrouve) un état de bloc dans la palette, et renvoie son index."""
    key = (name, tuple(sorted((properties or {}).items())))
    if key not in palette_index:
        entry = {"Name": name}
        if properties:
            entry["Properties"] = dict(properties)
        palette_index[key] = len(palette)
        palette.append(entry)
    return palette_index[key]


blocks = []


def put(x, y, z, index, nbt=None):
    block = {"state": index, "pos": [x, y, z]}
    if nbt is not None:
        block["nbt"] = nbt
    blocks.append(block)


FLOOR = state("minecraft:stone_bricks")
WALL = state("minecraft:polished_andesite")
LIGHT = state("minecraft:sea_lantern")
LANE = state("minecraft:smooth_stone")

# Sol plein.
for x in range(WIDTH):
    for z in range(DEPTH):
        put(x, FLOOR_Y, z, FLOOR)

# Un couloir visible entre le spawner et le cristal : purement cosmétique, mais ça rend le
# chemin des monstres lisible d'un coup d'œil quand on teste.
for z in range(4, DEPTH - 4):
    for x in range(WIDTH // 2 - 3, WIDTH // 2 + 4):
        put(x, FLOOR_Y, z, LANE)

# Murs d'enceinte : le monde est vide, sans eux on tombe dans le néant.
for y in range(FLOOR_Y + 1, FLOOR_Y + 1 + WALL_HEIGHT):
    for x in range(WIDTH):
        put(x, y, 0, WALL)
        put(x, y, DEPTH - 1, WALL)
    for z in range(1, DEPTH - 1):
        put(0, y, z, WALL)
        put(WIDTH - 1, y, z, WALL)

# Quelques lampes encastrées dans les murs : le biome est le vide, il fait nuit la moitié du
# temps et rien d'autre n'éclaire.
for z in range(6, DEPTH - 6, 12):
    put(0, FLOOR_Y + 3, z, LIGHT)
    put(WIDTH - 1, FLOOR_Y + 3, z, LIGHT)

BASE = FLOOR_Y + 1
CENTER = WIDTH // 2

# Cristal d'Eternia : l'objectif. Sa hitbox fait 3 blocs de haut, les murs en font 5.
put(CENTER, BASE, DEPTH - 8, state("dungeon_defenders:eternia_crystal"),
    {"id": "dungeon_defenders:eternia_crystal", "CrystalHealth": 100})

# Spawner à l'autre bout, configuré : 8 zombies et 4 squelettes, vagues 1 à 3.
put(CENTER, BASE, 6, state("dungeon_defenders:spawner"), {
    "id": "dungeon_defenders:spawner",
    "IntervalTicks": 40,
    "SpawnRadius": 2,
    "WaveStart": 1,
    "WaveEnd": 3,
    "LastCombatSessionHandled": 0,
    "Entries": [
        {"Enemy": 0, "BaseCount": 8, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": 8},
        {"Enemy": 1, "BaseCount": 4, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": 4},
    ],
})

# Point d'arrivée des joueurs, près du cristal (consommé au démarrage).
put(CENTER, BASE, DEPTH - 12, state("dungeon_defenders:player_spawn"))

# Coffre de mana, à portée de la zone de défense.
put(CENTER + 5, BASE, DEPTH - 12, state("dungeon_defenders:mana_chest", {"opened": "false"}),
    {"id": "dungeon_defenders:mana_chest", "ManaAmount": 50, "LastOpenedWave": 0})

# Configuration de la map : 3 vagues (et pas 5) exprès, pour vérifier en jeu que le nombre de
# vagues vient bien de la map et non de la constante globale.
put(1, BASE, 1, state("dungeon_defenders:map_config"), {
    "id": "dungeon_defenders:map_config",
    "MapName": "Arene de test",
    "MapOrder": 0,
    "WaveCount": 3,
    "ScoreMultiplier": Float(1.0),
    "FormatVersion": 1,
})

# Zone interdite à la pose : tout autour du spawner, pour qu'on ne puisse pas l'étouffer sous
# des tours — et pour tester le marqueur.
NO_BUILD = state("dungeon_defenders:no_build_zone")
for x in range(CENTER - 3, CENTER + 4):
    for z in range(3, 10):
        if (x, z) != (CENTER, 6):  # le spawner occupe déjà sa case
            put(x, BASE, z, NO_BUILD)

root = {
    "DataVersion": DATA_VERSION,
    "size": [WIDTH, HEIGHT, DEPTH],
    "palette": palette,
    "blocks": blocks,
    "entities": [],
}

import sys
target = sys.argv[1]
with gzip.open(target, "wb") as handle:
    handle.write(write_nbt(root))
print(f"{target}: {len(blocks)} blocs, {len(palette)} états, {WIDTH}x{HEIGHT}x{DEPTH}")
