package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Locale;

// Liste fermée des ennemis utilisables dans un SpawnerBlockEntity. Il n'existe pas de tag
// vanilla générique "tout ce qui est hostile" (vérifié) : cet enum sert à la fois de source
// de vérité pour le réseau (transmis par ordinal, même approche que GamePhase/GameDifficulty)
// et de liste pour le bouton "cycler le type" du GUI de config (SpawnerConfigScreen).
//
// Ajouter un ennemi ici (et uniquement ici) suffit à le rendre choisissable dans tous les
// spawners, sans autre changement au GUI ni au réseau — c'est le but de cet enum plutôt
// qu'un EntityType<?> brut par entrée.
public enum SpawnableEnemy {
    ZOMBIE(EntityType.ZOMBIE, Items.ZOMBIE_SPAWN_EGG),
    SKELETON(EntityType.SKELETON, Items.SKELETON_SPAWN_EGG);

    private final EntityType<? extends Monster> entityType;
    private final Item spawnEggItem;

    SpawnableEnemy(EntityType<? extends Monster> entityType, Item spawnEggItem) {
        this.entityType = entityType;
        this.spawnEggItem = spawnEggItem;
    }

    public EntityType<? extends Monster> entityType() {
        return this.entityType;
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
