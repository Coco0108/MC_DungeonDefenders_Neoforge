#!/usr/bin/env python3
"""Génère une map de test avec un grand écart entre le spawner et le Cristal d'Eternia.

Objectif précis (2026-09-12) : exercer SeekEterniaCrystalGoal (voir entity/ai/, doc/02-gameplay.md
"Le goal de longue distance") sur une distance largement supérieure au rayon de détection local
des goals de palier (16 blocs cristal / 8 blocs tour) et à l'ancien FOLLOW_RANGE vanilla d'un
zombie (35 blocs) — sans ce goal, un monstre spawné ici n'aurait aucune raison de se diriger vers
le cristal avant d'y "tomber" par hasard.

Même écriture NBT que tools/generer-map-de-test.py (structure `.nbt`, format relu depuis
StructureTemplate.java) : un couloir long et étroit plutôt qu'une arène, pour rendre le trajet
évident à observer en jeu. Écart réel spawner -> cristal : 74 blocs (voir SPAWNER_Z/CRYSTAL_Z).
"""
import gzip
import struct

DATA_VERSION = 4790  # relevé sur gametest/empty.nbt, écrit par cette version de Minecraft

# --- Écriture NBT -------------------------------------------------------------------------

TAG_END, TAG_BYTE, TAG_INT, TAG_FLOAT, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 0, 1, 3, 5, 8, 9, 10, 11


class Float(float):
    pass


def tag_of(value):
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

WIDTH, DEPTH = 9, 90           # couloir étroit et long, pas une arène
FLOOR_Y, WALL_HEIGHT = 0, 5
HEIGHT = FLOOR_Y + WALL_HEIGHT + 1
BASE = FLOOR_Y + 1
CENTER = WIDTH // 2

CRYSTAL_Z = 8     # extrémité sud : le cristal, proche de l'arrivée des joueurs
SPAWNER_Z = 82    # extrémité nord : le spawner, à 74 blocs du cristal en ligne droite

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

# Sol plein sur tout le couloir.
for x in range(WIDTH):
    for z in range(DEPTH):
        put(x, FLOOR_Y, z, FLOOR)

# Une bande centrale visible sur toute la longueur : purement cosmétique, pour que le trajet du
# spawner au cristal soit lisible d'un coup d'œil en jeu.
for z in range(DEPTH):
    put(CENTER, FLOOR_Y, z, LANE)

# Murs d'enceinte (le monde est vide, sans eux on tombe dans le néant) : les deux longs côtés
# sur toute la longueur, plus les deux extrémités.
for y in range(FLOOR_Y + 1, FLOOR_Y + 1 + WALL_HEIGHT):
    for z in range(DEPTH):
        put(0, y, z, WALL)
        put(WIDTH - 1, y, z, WALL)
    for x in range(WIDTH):
        put(x, y, 0, WALL)
        put(x, y, DEPTH - 1, WALL)

# Lampes encastrées régulièrement dans les murs latéraux, sur toute la longueur du couloir.
for z in range(4, DEPTH - 4, 12):
    put(0, FLOOR_Y + 3, z, LIGHT)
    put(WIDTH - 1, FLOOR_Y + 3, z, LIGHT)

# Cristal d'Eternia, près de l'extrémité sud.
put(CENTER, BASE, CRYSTAL_Z, state("dungeon_defenders:eternia_crystal"),
    {"id": "dungeon_defenders:eternia_crystal", "CrystalHealth": 100})

# Point d'arrivée des joueurs, juste devant le cristal (consommé au démarrage).
put(CENTER, BASE, CRYSTAL_Z + 4, state("dungeon_defenders:player_spawn"))

# Coffre de mana, à portée de la zone de défense.
put(CENTER + 2, BASE, CRYSTAL_Z + 4, state("dungeon_defenders:mana_chest", {"opened": "false"}),
    {"id": "dungeon_defenders:mana_chest", "ManaAmount": 50, "LastOpenedWave": 0})

# Spawner à l'autre extrémité, 74 blocs plus loin : largement au-delà du rayon de détection
# local (16 blocs cristal / 8 blocs tour) et de l'ancien FOLLOW_RANGE vanilla d'un zombie (35
# blocs), pour vérifier que SeekEterniaCrystalGoal fait bien converger le monstre sur toute la
# distance. Composition volontairement simple (uniquement des zombies) : ce test porte sur le
# déplacement, pas sur la composition des vagues.
put(CENTER, BASE, SPAWNER_Z, state("dungeon_defenders:spawner"), {
    "id": "dungeon_defenders:spawner",
    "IntervalTicks": 30,
    "SpawnRadius": 1,
    "WaveStart": 1,
    "WaveEnd": 2,
    "LastCombatSessionHandled": 0,
    "Entries": [
        {"Enemy": 0, "BaseCount": 10, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": 10},
    ],
})

# Configuration de la map : 2 vagues seulement, pour rejouer vite pendant les tests.
put(1, BASE, 1, state("dungeon_defenders:map_config"), {
    "id": "dungeon_defenders:map_config",
    "MapName": "Couloir - ecart IA",
    "MapOrder": 1,
    "WaveCount": 2,
    "ScoreMultiplier": Float(1.0),
    "FormatVersion": 1,
})

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
print(f"{target}: {len(blocks)} blocs, {len(palette)} etats, {WIDTH}x{HEIGHT}x{DEPTH}, "
      f"ecart spawner->cristal = {SPAWNER_Z - CRYSTAL_Z} blocs")
