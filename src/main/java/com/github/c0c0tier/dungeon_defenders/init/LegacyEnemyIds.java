package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.function.Consumer;

// Traduction de l'ANCIEN format des spawners (ennemi sauvegardé par ordinal de l'ex-enum
// SpawnableEnemy) vers le nouveau (identifiant d'ennemi data-driven, voir EnemyRegistry).
//
// **Cette table est figée pour toujours.** Elle ne décrit pas les ennemis qui existent
// aujourd'hui (ça, c'est le rôle des JSON), mais ce que voulaient dire les nombres écrits dans
// les fichiers AVANT la migration : 0 = zombie, 1 = squelette, l'ordre de l'enum au moment où
// il a été supprimé (vérifié identique sur toutes les branches le 2026-09-29, voir
// doc/data-driven/ennemis.md, "Gel de l'enum"). La modifier ferait relire de vieilles maps avec
// le mauvais monstre, sans erreur — exactement le problème que la migration supprime.
public final class LegacyEnemyIds {

    public static final Identifier ZOMBIE = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "zombie");
    public static final Identifier SKELETON = Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "skeleton");

    private static final Map<Integer, Identifier> BY_ORDINAL = Map.of(
            0, ZOMBIE,
            1, SKELETON);

    private static final String SPAWNER_ID = DungeonDefendersMod.MODID + ":spawner";

    private LegacyEnemyIds() {
    }

    /**
     * @return l'identifiant correspondant à un ancien ordinal. Un ordinal hors table (fichier
     *         corrompu ou bricolé) donne {@code dungeon_defenders:legacy_unknown_<n>} : il sera
     *         ensuite traité comme n'importe quel ennemi inconnu (log, rien ne spawne), sans
     *         jamais faire planter le chargement.
     */
    public static Identifier fromOrdinal(int ordinal) {
        Identifier known = BY_ORDINAL.get(ordinal);
        if (known != null) {
            return known;
        }
        return Identifier.fromNamespaceAndPath(DungeonDefendersMod.MODID, "legacy_unknown_" + Math.abs(ordinal));
    }

    /**
     * Convertit en place, dans le NBT brut d'une structure (`blocks[].nbt`), chaque champ
     * {@code "Enemy"} numérique des spawners en identifiant. Sert à {@code /dd_export}, qui
     * publie des fichiers : ils doivent sortir au nouveau format même si la map a été
     * sauvegardée avant la migration. Tout le reste du NBT est laissé intact.
     *
     * @param onConversion appelé une fois par ennemi converti, avec un message lisible
     * @return le nombre d'ennemis convertis
     */
    public static int convertStructure(CompoundTag structure, Consumer<String> onConversion) {
        ListTag blocks = structure.getList("blocks").orElse(null);
        if (blocks == null) {
            return 0;
        }

        int converted = 0;
        for (int i = 0; i < blocks.size(); i++) {
            CompoundTag block = blocks.getCompound(i).orElse(null);
            if (block == null) {
                continue;
            }
            CompoundTag nbt = block.getCompound("nbt").orElse(null);
            if (nbt == null || !SPAWNER_ID.equals(nbt.getString("id").orElse(""))) {
                continue;
            }
            ListTag entries = nbt.getList("Entries").orElse(null);
            if (entries == null) {
                continue;
            }
            for (int j = 0; j < entries.size(); j++) {
                CompoundTag entry = entries.getCompound(j).orElse(null);
                if (entry == null || !(entry.get("Enemy") instanceof NumericTag ordinalTag)) {
                    continue;
                }
                int ordinal = ordinalTag.intValue();
                Identifier id = fromOrdinal(ordinal);
                entry.put("Enemy", StringTag.valueOf(id.toString()));
                converted++;
                onConversion.accept("spawner " + block.get("pos") + " (relatif) : ennemi n°" + ordinal
                        + " converti en " + id);
            }
        }
        return converted;
    }
}
