#!/usr/bin/env python3
"""Vérifie qu'aucune classe chargée par un serveur dédié ne NOMME une classe cliente.

Pourquoi cet outil existe : un serveur dédié n'embarque aucune classe cliente, et la JVM
résout ce qu'une classe mentionne au moment où elle la charge — pas seulement au moment où le
code s'exécute. Un `if (level.isClientSide()) { Minecraft.getInstance()... }` dans un bloc
suffit donc à faire échouer le chargement du mod entier, avec un NoClassDefFoundError sur une
classe cliente dès constructMods. C'est arrivé pour de vrai (TavernCrystalBlock, 2026-08-30,
voir doc/05-etat-et-problemes-connus.md).

Ni `./gradlew build` ni `./gradlew runServer` n'attrapent ça : l'environnement de dev contient
les classes des deux côtés. Il faut donc analyser le jar produit et refaire le raisonnement du
serveur — c'est ce que fait ce script, en parcourant le constant pool des .class.

Usage :

    ./gradlew build -x test
    python3 tools/verifier-dist.py build/libs/dungeon_defenders-0.0.1.jar

Sort 0 si le graphe serveur est propre, 1 sinon (avec la chaîne de références fautive).
"""
import collections
import struct
import sys
import zipfile

PKG = "com/github/c0c0tier/dungeon_defenders/"

# Classes qui vivent HORS du paquet `client/` mais qui sont malgré tout client-only : elles
# citent du code client, et seul DungeonDefendersModClient (jamais chargé sur un serveur
# dédié) les atteint. Elles ne sont donc pas des points de départ pour l'analyse.
#
# Si le script signale une classe légitimement client-only qui n'est pas dans cette liste,
# le bon réflexe est de la déplacer sous `client/` — l'ajouter ici est le second choix.
CLIENT_ONLY_OUTSIDE_CLIENT_PACKAGE = {
    "DungeonDefendersModClient",
    "block/entity/EterniaCrystalBlockEntityRenderer",
    "block/entity/EterniaCrystalRenderState",
    "block/entity/HealthBarRendering",
    "block/entity/HealthLerp",
    "block/entity/SpawnerBlockEntityRenderer",
    "block/entity/SpawnerRenderState",
    "block/entity/TowerHealthBarRenderer",
    "block/entity/TowerHealthBarRenderState",
    "entity/MobHealthBarRenderer",
}


def referenced_names(data):
    """Noms de classes et descripteurs cités dans le constant pool d'un .class."""
    if data[:4] != b"\xca\xfe\xba\xbe":
        raise ValueError("pas un fichier .class")
    count = struct.unpack(">H", data[8:10])[0]
    utf8, class_indexes, i, pos = {}, [], 1, 10
    while i < count:
        tag = data[pos]
        pos += 1
        if tag == 1:  # CONSTANT_Utf8
            length = struct.unpack(">H", data[pos:pos + 2])[0]
            pos += 2
            utf8[i] = data[pos:pos + length].decode("utf-8", "replace")
            pos += length
        elif tag == 7:  # CONSTANT_Class
            class_indexes.append(struct.unpack(">H", data[pos:pos + 2])[0])
            pos += 2
        elif tag == 15:  # MethodHandle
            pos += 3
        elif tag in (8, 16, 19, 20):  # String, MethodType, Module, Package
            pos += 2
        elif tag in (3, 4, 9, 10, 11, 12, 17, 18):  # Integer/Float/refs/NameAndType/Dynamic
            pos += 4
        elif tag in (5, 6):  # Long/Double occupent deux entrées
            pos += 8
            i += 1
        else:
            raise ValueError("tag de constant pool inconnu : %d" % tag)
        i += 1

    names = {utf8[j] for j in class_indexes if j in utf8}
    # Les descripteurs de champs/méthodes et les signatures génériques citent aussi des types
    # sans passer par CONSTANT_Class : on les ratisse au motif, sans chercher à les parser.
    names.update(s for s in utf8.values() if "net/minecraft/client/" in s)
    return names


def main(jar_path):
    classes = {}
    with zipfile.ZipFile(jar_path) as jar:
        for name in jar.namelist():
            if name.endswith(".class") and name.startswith(PKG):
                classes[name[:-len(".class")]] = referenced_names(jar.read(name))

    if not classes:
        print("Aucune classe du mod trouvée dans %s" % jar_path)
        return 1

    client_only = {PKG + short for short in CLIENT_ONLY_OUTSIDE_CLIENT_PACKAGE}

    def is_client_only(name):
        # Les classes internes et synthétiques (Foo$1, Foo$Bar) suivent le côté de leur
        # classe englobante : c'est le nom avant le premier '$' qui décide.
        return name.split("$", 1)[0] in client_only

    roots = sorted(
        c for c in classes
        if not c.startswith(PKG + "client/") and not is_client_only(c)
    )

    seen, queue, came_from = set(roots), collections.deque(roots), {}
    leaks = []
    while queue:
        current = queue.popleft()
        for ref in classes.get(current, ()):
            if "net/minecraft/client/" in ref:
                leaks.append((current, ref))
            elif ref in classes and ref not in seen:
                seen.add(ref)
                came_from[ref] = current
                queue.append(ref)

    print("Classes du mod chargeables côté serveur : %d / %d" % (len(seen), len(classes)))

    if not leaks:
        print("OK : aucune classe cliente nommée dans le graphe serveur.")
        return 0

    print("\nFUITE CLIENT -> SERVEUR (le mod ne chargera pas sur un serveur dédié) :")
    for owner, ref in sorted(set(leaks)):
        chain, node = [owner], owner
        while node in came_from:
            node = came_from[node]
            chain.append(node)
        print("  %s\n    cite %s\n    atteint via %s" % (owner, ref, " <- ".join(chain)))
    return 1


if __name__ == "__main__":
    if len(sys.argv) != 2:
        print("usage: %s <chemin/vers/le.jar>" % sys.argv[0])
        sys.exit(2)
    sys.exit(main(sys.argv[1]))
