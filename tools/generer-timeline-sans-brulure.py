#!/usr/bin/env python3
"""Régénère data/minecraft/timeline/day.json : la timeline "day" vanilla, SANS la piste
`minecraft:gameplay/monsters_burn`.

Pourquoi : depuis que l'heure du monde est figée (WorldTime, midi par défaut), zombies et
squelettes brûleraient en permanence sur une map à ciel ouvert. Le jeu décide ça par l'attribut
d'environnement MONSTERS_BURN, que cette piste met à vrai pendant la journée (modificateur "or" :
impossible de le forcer à faux depuis la dimension ou le biome). Remplacer le fichier vanilla par
une copie sans cette piste est la seule façon purement "données" de l'éteindre, et elle vaut
aussi pour les mondes déjà créés.

À relancer à chaque changement de version de Minecraft, pour reprendre les autres pistes (couleur
du ciel, angle du soleil...) telles que le jeu les livre :

    python3 tools/generer-timeline-sans-brulure.py <minecraft.jar> src/main/resources/data/minecraft/timeline/day.json
"""
import json
import sys
import zipfile

SOURCE = "data/minecraft/timeline/day.json"
TRACK = "minecraft:gameplay/monsters_burn"

jar_path, target = sys.argv[1], sys.argv[2]
with zipfile.ZipFile(jar_path) as jar:
    timeline = json.loads(jar.read(SOURCE))

if TRACK not in timeline["tracks"]:
    sys.exit(f"Piste {TRACK} absente de {SOURCE} : le format a changé, à revoir à la main.")
del timeline["tracks"][TRACK]

with open(target, "w", encoding="utf-8") as handle:
    json.dump(timeline, handle, indent=2)
    handle.write("\n")
print(f"{target} : {len(timeline['tracks'])} pistes conservées, {TRACK} retirée")
