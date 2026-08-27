package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;
import java.util.Optional;

// Liste fermée des ennemis utilisables dans un SpawnerBlockEntity. Il n'existe pas de tag
// vanilla générique "tout ce qui est hostile" (vérifié) : cet enum sert à la fois de source
// de vérité pour le réseau (transmis par ordinal, même approche que GamePhase/GameDifficulty)
// et de liste pour le bouton "cycler le type" du GUI de config (SpawnerConfigScreen). Sert
// aussi de barème d'XP/score (ModEvents.onMonsterDeath) — un ennemi plus dangereux (le
// squelette, à distance) rapporte plus qu'un zombie.
//
// Ajouter un ennemi ici (et uniquement ici) suffit à le rendre choisissable dans tous les
// spawners, sans autre changement au GUI ni au réseau — c'est le but de cet enum plutôt
// qu'un EntityType<?> brut par entrée.
public enum SpawnableEnemy {
    ZOMBIE(EntityType.ZOMBIE, Items.ZOMBIE_SPAWN_EGG, 10),
    SKELETON(EntityType.SKELETON, Items.SKELETON_SPAWN_EGG, 15);

    // Valeur donnée à chaque mort (voir ModEvents.onMonsterDeath) : à la fois en XP joueur
    // (partagée entre tous les joueurs présents) et en score de la carte. Valeurs de test, pas
    // encore équilibrées, comme les coûts de pose des tours.
    private static final int DEFAULT_XP_VALUE = 5;

    private final EntityType<? extends Monster> entityType;
    private final Item spawnEggItem;
    private final int xpValue;

    SpawnableEnemy(EntityType<? extends Monster> entityType, Item spawnEggItem, int xpValue) {
        this.entityType = entityType;
        this.spawnEggItem = spawnEggItem;
        this.xpValue = xpValue;
    }

    public EntityType<? extends Monster> entityType() {
        return this.entityType;
    }

    public int xpValue() {
        return this.xpValue;
    }

    // Utilisé par ModEvents.onMonsterDeath pour retrouver la valeur d'XP/score d'un monstre
    // tué à partir de son EntityType. DEFAULT_XP_VALUE en repli : ce mod ne fait apparaître que
    // des ennemis listés ici via le Spawner, mais reste défensif plutôt que de supposer une
    // correspondance garantie (ex. un futur monstre spawné autrement).
    public static int xpValueFor(EntityType<?> entityType) {
        return findFor(entityType).map(SpawnableEnemy::xpValue).orElse(DEFAULT_XP_VALUE);
    }

    private static Optional<SpawnableEnemy> findFor(EntityType<?> entityType) {
        for (SpawnableEnemy enemy : values()) {
            if (enemy.entityType == entityType) {
                return Optional.of(enemy);
            }
        }
        return Optional.empty();
    }

    // Icône réutilisée dans l'aperçu de composition du spawner (SpawnerBlockEntityRenderer) :
    // pas de sprite dédié par ennemi, l'œuf d'invocation vanilla correspondant sert de
    // placeholder reconnaissable sans dépendre d'un asset custom.
    public Item spawnEggItem() {
        return this.spawnEggItem;
    }

    public String translationKey() {
        return "dungeon_defenders.enemy." + name().toLowerCase(Locale.ROOT);
    }

    /** Valeur suivante dans l'enum, en bouclant — pour le bouton "cycler" du GUI. */
    public SpawnableEnemy next() {
        SpawnableEnemy[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
