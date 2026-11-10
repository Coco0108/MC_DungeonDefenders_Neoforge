#!/usr/bin/env python3
"""Génère la structure d'une deuxième map de test, plus ambitieuse que `test_arena` :
« Sanctuaire en Ruines », trois couloirs de monstres convergeant vers une plateforme centrale
surélevée où se trouve le Cristal d'Eternia, avec une petite cour d'arrivée pour les joueurs côté
sud. Concept librement inspiré de la structure générale des toutes premières maps de la
campagne du jeu de référence (couloirs multiples + salle centrale surélevée) — pas une
reproduction d'une map précise, ni de ses dimensions ou de ses textures réelles (voir la
discussion sur la provenance des assets, doc/05-etat-et-problemes-connus.md).

Même écrivain NBT que generer-map-de-test.py (copié tel quel, déjà vérifié) ; seule la section
« Construction de la map » diffère.
"""
import gzip
import struct

DATA_VERSION = 4790  # relevé sur gametest/empty.nbt, écrit par cette version de Minecraft

# --- Écriture NBT (identique à generer-map-de-test.py) --------------------------------------

TAG_END, TAG_BYTE, TAG_INT, TAG_FLOAT, TAG_STRING, TAG_LIST, TAG_COMPOUND, TAG_INT_ARRAY = 0, 1, 3, 5, 8, 9, 10, 11


class Int(int):
    """Marqueur : entier NBT (TAG_Int) plutôt que déduit du type Python."""


class Float(float):
    pass


class IntArray(list):
    """Marqueur : TAG_Int_Array. NON utilisé pour "size"/"pos" (voir StructureTemplate#save,
    qui les écrit en TAG_List d'entiers)."""


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


# --- Construction de la map -------------------------------------------------------------------

WIDTH = DEPTH = 63          # impair : un centre franc
CENTER = 31
FLOOR_Y = 0                  # sol le plus bas de la structure
BASE = FLOOR_Y + 1           # hauteur de marche des couloirs/de la cour
RAMP_RISE = 4                # dénivelé de chaque rampe (4 marches d'1 bloc)
PLAZA_BASE = BASE + RAMP_RISE  # hauteur de marche de la plateforme centrale
WALL_HEIGHT = 8              # hauteur des murs d'enceinte au-dessus de FLOOR_Y

LANE_HALF_WIDTH = 3          # couloir large de 7 (CENTER-3..CENTER+3)
PLAZA_HALF = 9                # plateforme centrale large de 19 (CENTER-9..CENTER+9)
PLAZA_MIN = CENTER - PLAZA_HALF   # 22
PLAZA_MAX = CENTER + PLAZA_HALF   # 40

SPAWNER_ROW = 6               # même marge que test_arena entre le spawner et le mur d'enceinte
CRYSTAL_HEIGHT = 3            # hitbox connue du Cristal d'Eternia (mur/plaza doivent la dépasser)

HEIGHT = FLOOR_Y + WALL_HEIGHT + 1

palette, palette_index = [], {}


def state(name, properties=None):
    key = (name, tuple(sorted((properties or {}).items())))
    if key not in palette_index:
        entry = {"Name": name}
        if properties:
            entry["Properties"] = dict(properties)
        palette_index[key] = len(palette)
        palette.append(entry)
    return palette_index[key]


blocks_by_pos = {}   # (x,y,z) -> bloc ; une position posée deux fois garde la DERNIÈRE (même
                      # sémantique que StructureTemplate#placeInWorld, qui rejoue la liste dans
                      # l'ordre) — utile ici pour percer volontairement les murs avec les
                      # lampes, posées après. dict plutôt que liste : jamais deux entrées pour
                      # la même position dans le fichier final.
overwrite_count = 0   # juste pour la sortie de contrôle en fin de script


def put(x, y, z, index, nbt=None):
    global overwrite_count
    pos = (x, y, z)
    if not (0 <= x < WIDTH and 0 <= y < HEIGHT and 0 <= z < DEPTH):
        raise AssertionError(f"Position hors structure : {pos} (taille {WIDTH}x{HEIGHT}x{DEPTH})")
    if pos in blocks_by_pos:
        overwrite_count += 1
    block = {"state": index, "pos": [x, y, z]}
    if nbt is not None:
        block["nbt"] = nbt
    blocks_by_pos[pos] = block


def fill_column(x, z, y_from, y_to, top_index, core_index):
    """Remplit une colonne de y_from à y_to (inclus), le dessus dans top_index, le reste (masqué)
    dans core_index — pour qu'une marche/plateforme surélevée ne soit jamais creuse en dessous."""
    for y in range(y_from, y_to):
        put(x, y, z, core_index)
    put(x, y_to, z, top_index)


FLOOR = state("minecraft:stone_bricks")
WALL = state("minecraft:polished_andesite")
LIGHT = state("minecraft:sea_lantern")
LANE = state("minecraft:smooth_stone")
PLAZA_TOP = state("minecraft:polished_blackstone_bricks")
CORE = state("minecraft:stone")
PILLAR = state("minecraft:mossy_stone_bricks")
PILLAR_CRACKED = state("minecraft:cracked_stone_bricks")
TORCH = state("minecraft:torch")
NO_BUILD = state("dungeon_defenders:no_build_zone")

# Sol plein sur toute l'emprise : la base sous absolument tout le reste (couloirs, rampes,
# plateforme, cour), comme test_arena.
for x in range(WIDTH):
    for z in range(DEPTH):
        put(x, FLOOR_Y, z, FLOOR)

# Murs d'enceinte : le monde est vide, sans eux on tombe dans le néant.
for y in range(FLOOR_Y + 1, FLOOR_Y + 1 + WALL_HEIGHT):
    for x in range(WIDTH):
        put(x, y, 0, WALL)
        put(x, y, DEPTH - 1, WALL)
    for z in range(1, DEPTH - 1):
        put(0, y, z, WALL)
        put(WIDTH - 1, y, z, WALL)

# --- Plateforme centrale (podium plein, pas creux) -------------------------------------------
for x in range(PLAZA_MIN, PLAZA_MAX + 1):
    for z in range(PLAZA_MIN, PLAZA_MAX + 1):
        fill_column(x, z, BASE, PLAZA_BASE, PLAZA_TOP, CORE)

# --- Les trois couloirs de monstres (nord, est, ouest) + leur rampe --------------------------
# Même géométrie pour les trois, écrite explicitement par direction plutôt que factorisée en une
# fonction générique à rotations : plus de lignes, mais chaque couloir se relit et se vérifie
# indépendamment sans avoir à dérouler une transformation de coordonnées dans la tête.

lane_ends = {}  # direction -> position du spawner, réutilisée plus bas pour spawner/no-build
# Convention de hauteur (comme test_arena) : LANE/PLAZA_TOP posés par fill_column sont le bloc
# SOLIDE du sol, pas la case où on se tient. Tout ce qui doit "être posé sur le sol" (spawner,
# cristal, marqueurs...) va donc un bloc AU-DESSUS de la hauteur de sol correspondante — d'où
# les "+ 1" un peu partout ci-dessous plutôt que BASE/PLAZA_BASE directement.

# Nord : x autour de CENTER, z décroissant depuis la plateforme.
for x in range(CENTER - LANE_HALF_WIDTH, CENTER + LANE_HALF_WIDTH + 1):
    for z in range(1, PLAZA_MIN - RAMP_RISE):
        put(x, BASE, z, LANE)
    for i, z in enumerate(range(PLAZA_MIN - RAMP_RISE, PLAZA_MIN)):
        fill_column(x, z, BASE, BASE + i + 1, LANE, CORE)
lane_ends["north"] = (CENTER, BASE + 1, SPAWNER_ROW)

# Est : z autour de CENTER, x croissant depuis la plateforme.
for z in range(CENTER - LANE_HALF_WIDTH, CENTER + LANE_HALF_WIDTH + 1):
    for x in range(PLAZA_MAX + RAMP_RISE + 1, WIDTH - 1):
        put(x, BASE, z, LANE)
    for i, x in enumerate(range(PLAZA_MAX + RAMP_RISE, PLAZA_MAX, -1)):
        fill_column(x, z, BASE, BASE + i + 1, LANE, CORE)
lane_ends["east"] = (WIDTH - 1 - SPAWNER_ROW, BASE + 1, CENTER)

# Ouest : z autour de CENTER, x décroissant depuis la plateforme.
for z in range(CENTER - LANE_HALF_WIDTH, CENTER + LANE_HALF_WIDTH + 1):
    for x in range(1, PLAZA_MIN - RAMP_RISE):
        put(x, BASE, z, LANE)
    for i, x in enumerate(range(PLAZA_MIN - RAMP_RISE, PLAZA_MIN)):
        fill_column(x, z, BASE, BASE + i + 1, LANE, CORE)
lane_ends["west"] = (SPAWNER_ROW, BASE + 1, CENTER)

# --- Rampe sud, pour les joueurs uniquement (pas de couloir de monstres, pas de spawner) ------
for x in range(CENTER - LANE_HALF_WIDTH, CENTER + LANE_HALF_WIDTH + 1):
    for i, z in enumerate(range(PLAZA_MAX + 1, PLAZA_MAX + 1 + RAMP_RISE)):
        fill_column(x, z, BASE, PLAZA_BASE - i - 1 if PLAZA_BASE - i - 1 >= BASE else BASE, LANE, CORE)

# --- Cour d'arrivée des joueurs (sud), sol simple au niveau BASE ------------------------------
for x in range(1, WIDTH - 1):
    for z in range(PLAZA_MAX + RAMP_RISE + 1, DEPTH - 1):
        put(x, BASE, z, LANE)

# --- Décor : piliers en ruine le long des trois couloirs de monstres --------------------------
PILLAR_OFFSET = LANE_HALF_WIDTH + 1  # juste à l'extérieur du couloir large de 7


def pillar(x, z, height, cracked_top):
    # Les piliers sont dans la bande hors-couloir, jamais recouverte par LANE/CORE — son sol
    # reste le bloc plein FLOOR posé à FLOOR_Y (0) tout au début, donc le pilier commence à BASE
    # (1), pas BASE + 1 comme sur une case de couloir/plateforme (voir la remarque sur la
    # convention de hauteur plus haut : ici il n'y a pas de "bloc solide de couloir" à sauter).
    for y in range(BASE, BASE + height):
        put(x, y, z, PILLAR)
    top_index = PILLAR_CRACKED if cracked_top else PILLAR
    put(x, BASE + height, z, top_index)
    if not cracked_top:
        put(x, BASE + height + 1, z, TORCH)


for i, z in enumerate(range(SPAWNER_ROW + 2, PLAZA_MIN - RAMP_RISE, 6)):
    height = 3 if i % 2 == 0 else 2
    pillar(CENTER - PILLAR_OFFSET, z, height, i % 3 == 2)
    pillar(CENTER + PILLAR_OFFSET, z, height, i % 3 == 2)

for i, x in enumerate(range(PLAZA_MAX + RAMP_RISE + 2, WIDTH - 1 - SPAWNER_ROW - 2, 6)):
    height = 3 if i % 2 == 0 else 2
    pillar(x, CENTER - PILLAR_OFFSET, height, i % 3 == 2)
    pillar(x, CENTER + PILLAR_OFFSET, height, i % 3 == 2)

for i, x in enumerate(range(SPAWNER_ROW + 2, PLAZA_MIN - RAMP_RISE, 6)):
    height = 3 if i % 2 == 0 else 2
    pillar(x, CENTER - PILLAR_OFFSET, height, i % 3 == 2)
    pillar(x, CENTER + PILLAR_OFFSET, height, i % 3 == 2)

# Quelques lampes encastrées dans les murs (mêmes emplacements que test_arena, quatre côtés).
for z in range(6, DEPTH - 6, 14):
    put(0, FLOOR_Y + 3, z, LIGHT)
    put(WIDTH - 1, FLOOR_Y + 3, z, LIGHT)
for x in range(6, WIDTH - 6, 14):
    put(x, FLOOR_Y + 3, 0, LIGHT)
    put(x, FLOOR_Y + 3, DEPTH - 1, LIGHT)

# --- Le Cristal d'Eternia, au centre de la plateforme ------------------------------------------
put(CENTER, PLAZA_BASE + 1, CENTER, state("dungeon_defenders:eternia_crystal"),
    {"id": "dungeon_defenders:eternia_crystal", "CrystalHealth": 100})

# --- Les trois spawners, un par couloir, compositions différentes -----------------------------
def spawner(pos, zombies, skeletons):
    x, y, z = pos
    put(x, y, z, state("dungeon_defenders:spawner"), {
        "id": "dungeon_defenders:spawner",
        "IntervalTicks": 40,
        "SpawnRadius": 2,
        "WaveStart": 1,
        "WaveEnd": 5,
        "LastCombatSessionHandled": 0,
        "Entries": [
            {"Enemy": 0, "BaseCount": zombies, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": zombies},
            {"Enemy": 1, "BaseCount": skeletons, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": skeletons},
        ] if skeletons > 0 else [
            {"Enemy": 0, "BaseCount": zombies, "Spawned": 0, "Accumulator": 0, "EffectiveTotal": zombies},
        ],
    })


# Nord : rush de corps-à-corps (que des zombies) — le couloir le plus direct vers la plateforme.
spawner(lane_ends["north"], zombies=10, skeletons=0)
# Est : pression à distance (surtout des squelettes) — récompense les tours qui coupent la ligne
# de vue tôt dans le couloir.
spawner(lane_ends["east"], zombies=3, skeletons=7)
# Ouest : mixte, le couloir "généraliste".
spawner(lane_ends["west"], zombies=6, skeletons=4)

# --- Zone interdite autour de chaque spawner, comme test_arena --------------------------------
def no_build_ring(center_pos, axis):
    cx, cy, cz = center_pos
    for dx in range(-3, 4):
        for dz in range(-3, 4):
            x, z = cx + dx, cz + dz
            if (x, z) == (cx, cz):
                continue
            put(x, cy, z, NO_BUILD)


no_build_ring(lane_ends["north"], "z")
no_build_ring(lane_ends["east"], "x")
no_build_ring(lane_ends["west"], "x")

# --- Arrivée des joueurs et coffre de mana, sur la plateforme, à côté du cristal ---------------
put(CENTER, PLAZA_BASE + 1, CENTER + 4, state("dungeon_defenders:player_spawn"))
put(CENTER + 4, PLAZA_BASE + 1, CENTER + 4, state("dungeon_defenders:mana_chest", {"opened": "false"}),
    {"id": "dungeon_defenders:mana_chest", "ManaAmount": 50, "LastOpenedWave": 0})

# --- Configuration de la map --------------------------------------------------------------------
put(PLAZA_MIN + 1, PLAZA_BASE + 1, PLAZA_MIN + 1, state("dungeon_defenders:map_config"), {
    "id": "dungeon_defenders:map_config",
    "MapName": "Sanctuaire en Ruines",
    "MapOrder": 1,
    "WaveCount": 5,
    "ScoreMultiplier": Float(1.2),
    "FormatVersion": 1,
})

blocks = list(blocks_by_pos.values())

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
print(f"{target}: {len(blocks)} blocs ({overwrite_count} positions ecrasees intentionnellement), "
      f"{len(palette)} etats, {WIDTH}x{HEIGHT}x{DEPTH}")
