#!/usr/bin/env python3
"""Génère une map de test à distance modérée où les monstres doivent faire un détour.

Objectif précis (2026-09-12), en complément de generer-map-ecart-ia.py (qui teste une longue
distance en ligne droite) : vérifier que SeekEterniaCrystalGoal, en s'appuyant sur le vrai
pathfinder Minecraft, sait aussi contourner un obstacle plutôt que de foncer bêtement tout droit
- ici, DEUX obstacles combinés sur une distance modérée (~27 blocs en ligne droite) :

1. Un mur plein (5 blocs de haut) qui barre presque toute la largeur de la salle, avec un
   passage étroit sur la gauche seulement -> détour latéral.
2. Une falaise (3 blocs de haut, jamais franchissable d'un seul pas) qui barre la suite du
   couloir pour qui n'a pas déjà rejoint la droite, où une rampe en escalier (terrassée, 1 bloc
   de plus par rangée - même technique que generer-map-sanctuaire.py) permet de monter ->
   détour + changement de hauteur.

Même écriture NBT que les scripts précédents (structure `.nbt` gzippée, format relu dans
StructureTemplate.java).
"""
import gzip
import struct

DATA_VERSION = 4790

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

WIDTH, DEPTH = 13, 34   # salle modérée, pas un long couloir (voir generer-map-ecart-ia.py)
HEIGHT = 11             # assez haut pour la salle basse (murs jusqu'à y=9) ET la plateforme haute
CENTER = WIDTH // 2      # = 6

# Zone "mur" (détour latéral) : passage laissé ouvert seulement à x < WALL_GAP_X.
WALL_Z = (7, 8)
WALL_GAP_X = 4
WALL_TOP_Y = 5

# Zone "rampe" (détour + hauteur) : seules les colonnes x >= RAMP_X0 montent ; les autres
# heurtent une falaise infranchissable à CLIFF_Z et doivent avoir rejoint la rampe avant.
RAMP_X0 = 9
RAMP_Z0 = 16       # première rangée qui monte (ground_top passe de 0 à 1)
CLIFF_Z = 20       # rangée de falaise (bloque x < RAMP_X0)
CLIFF_TOP_Y = 3    # falaise de 3 blocs (y=1..3) : aucun mob ne franchit ça d'un seul pas
HIGH_Z0 = 21       # à partir de cette rangée, toute la largeur est à hauteur haute

CRYSTAL_Z = 30
PLAYER_SPAWN_Z = 26
SPAWNER_Z = 3

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


blocks = []


def put(x, y, z, index, nbt=None):
    block = {"state": index, "pos": [x, y, z]}
    if nbt is not None:
        block["nbt"] = nbt
    blocks.append(block)


FLOOR = state("minecraft:stone_bricks")
WALL = state("minecraft:polished_andesite")
CLIFF = state("minecraft:polished_andesite")
HIGH_FLOOR = state("minecraft:smooth_quartz")   # distinct visuellement : "on est monté"
LIGHT = state("minecraft:sea_lantern")
LANE = state("minecraft:smooth_stone")


def ground_top(x, z):
    """Hauteur (y) du dernier bloc solide du sol pour cette colonne — le mob se tient à
    ground_top + 1. Encode entièrement le tracé attendu : bas partout, sauf la rampe (colonnes
    x >= RAMP_X0, à partir de RAMP_Z0) qui monte d'1 bloc par rangée, et la salle haute
    (z >= HIGH_Z0) qui reste en permanence au niveau haut, quelle que soit la colonne."""
    if z >= HIGH_Z0:
        return CLIFF_TOP_Y
    if x >= RAMP_X0 and z >= RAMP_Z0:
        return min(CLIFF_TOP_Y, z - RAMP_Z0 + 1)
    return 0


# Sol : un bloc solide à chaque y de 0 jusqu'à ground_top(x, z) inclus, pour chaque colonne.
for x in range(WIDTH):
    for z in range(DEPTH):
        top = ground_top(x, z)
        is_high = top == CLIFF_TOP_Y and z >= RAMP_Z0
        for y in range(0, top + 1):
            put(x, y, z, HIGH_FLOOR if (is_high and y == top) else FLOOR)

# Bande centrale visible sur la partie basse : purement cosmétique, pour lire le tracé attendu
# d'un coup d'œil (le passage à gauche, puis le détour vers la droite pour la rampe).
for z in range(0, HIGH_Z0):
    top = ground_top(CENTER, z) if CENTER < RAMP_X0 else 0
    put(CENTER, top, z, LANE)

# Obstacle 1 : le mur (détour latéral). Passage ouvert uniquement à x < WALL_GAP_X.
for z in WALL_Z:
    for x in range(WALL_GAP_X, WIDTH):
        for y in range(1, WALL_TOP_Y + 1):
            put(x, y, z, WALL)

# Obstacle 2 : la falaise (détour + hauteur). Bloque toute colonne qui n'a pas déjà grimpé par
# la rampe (x >= RAMP_X0) avant d'atteindre CLIFF_Z.
for x in range(0, RAMP_X0):
    for y in range(1, CLIFF_TOP_Y + 1):
        put(x, y, CLIFF_Z, CLIFF)

# Périmètre : les deux longs côtés et les deux extrémités, assez haut pour couvrir la salle
# basse ET la plateforme haute (le mob ne doit jamais pouvoir sortir par-dessus, quelle que soit
# la zone).
for z in range(DEPTH):
    for x in (0, WIDTH - 1):
        for y in range(1, 10):
            put(x, y, z, WALL)
for x in range(WIDTH):
    for z in (0, DEPTH - 1):
        for y in range(1, 10):
            put(x, y, z, WALL)

# Lampes, uniquement dans la salle basse (la salle haute a déjà son propre sol clair).
for z in range(3, HIGH_Z0, 8):
    put(0, 3, z, LIGHT)
    put(WIDTH - 1, 3, z, LIGHT)

BASE_LOW = ground_top(CENTER, SPAWNER_Z) + 1     # = 1
BASE_HIGH = CLIFF_TOP_Y + 1                       # = 4

# Cristal d'Eternia, sur la plateforme haute.
put(CENTER, BASE_HIGH, CRYSTAL_Z, state("dungeon_defenders:eternia_crystal"),
    {"id": "dungeon_defenders:eternia_crystal", "CrystalHealth": 100})

# Point d'arrivée des joueurs, sur la plateforme, près du cristal.
put(CENTER, BASE_HIGH, PLAYER_SPAWN_Z, state("dungeon_defenders:player_spawn"))

# Coffre de mana, à côté.
put(CENTER + 2, BASE_HIGH, PLAYER_SPAWN_Z, state("dungeon_defenders:mana_chest", {"opened": "false"}),
    {"id": "dungeon_defenders:mana_chest", "ManaAmount": 50, "LastOpenedWave": 0})

# Spawner, dans la salle basse, avant les deux obstacles. ~27 blocs du cristal en ligne droite
# (distance modérée, contrairement au couloir de 74 blocs de generer-map-ecart-ia.py) : ce
# n'est pas la distance qui est testée ici, mais la capacité à contourner.
put(CENTER, BASE_LOW, SPAWNER_Z, state("dungeon_defenders:spawner"), {
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

# Configuration de la map.
put(1, BASE_LOW, 1, state("dungeon_defenders:map_config"), {
    "id": "dungeon_defenders:map_config",
    "MapName": "Detour - IA",
    "MapOrder": 2,
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

straight_line = ((CENTER - CENTER) ** 2 + (BASE_HIGH - BASE_LOW) ** 2 + (CRYSTAL_Z - SPAWNER_Z) ** 2) ** 0.5
print(f"{target}: {len(blocks)} blocs, {len(palette)} etats, {WIDTH}x{HEIGHT}x{DEPTH}, "
      f"distance spawner->cristal (ligne droite) = {straight_line:.1f} blocs")
