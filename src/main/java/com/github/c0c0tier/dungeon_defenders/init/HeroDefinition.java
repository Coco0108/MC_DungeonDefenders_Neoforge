package com.github.c0c0tier.dungeon_defenders.init;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Locale;

// Catalogue des héros jouables — même rôle que TowerDefinition pour les tours : un membre par
// héros, commun au client (écran de choix, filtrage de la roue) et au serveur (validation de
// PlaceTowerPayload).
//
// **Pourquoi cet enum arrive avant les compétences et les armes** : décidé avec le joueur
// (2026-09-03), on construit UN héros complet plutôt que les tours des quatre. La classe de base
// une fois, les différences ensuite — exactement le principe déjà appliqué aux tours
// (AbstractTowerBlockEntity porte le commun, chaque tour concrète ne fixe que ses stats).
// Ajouter un héros deviendra une entrée ici plus ses tours, sans toucher au reste.
//
// Un seul membre pour l'instant, comme ManaCrystalType à ses débuts : l'écran de choix n'a donc
// qu'une option. C'est volontaire — ça valide toute la plomberie (sélection, persistance,
// synchro, filtrage) sur un cas simple, avant que trois héros de plus ne s'y ajoutent.
public enum HeroDefinition {

    SQUIRE("squire", List.of(
            TowerDefinition.SPIKE_BLOCKADE,
            TowerDefinition.BOUNCER_BLOCKADE,
            TowerDefinition.SLICE_N_DICE_BLOCKADE,
            TowerDefinition.HARPOON_TURRET,
            TowerDefinition.BOWLING_BALL_TURRET,
            TowerDefinition.MORTAR_TURRET));

    /** Héros attribué à un joueur qui n'a jamais choisi — voir ModAttachments.HERO. */
    public static final HeroDefinition DEFAULT = SQUIRE;

    private final String id;
    private final List<TowerDefinition> towers;

    HeroDefinition(String id, List<TowerDefinition> towers) {
        this.id = id;
        this.towers = towers;
    }

    public String id() {
        return this.id;
    }

    /** Les tours que ce héros peut poser, dans l'ordre où la roue les présente. */
    public List<TowerDefinition> towers() {
        return this.towers;
    }

    public Component displayName() {
        return Component.translatable("dungeon_defenders.hero." + this.id);
    }

    public Component description() {
        return Component.translatable("dungeon_defenders.hero." + this.id + ".description");
    }

    /** Icône de l'écran de choix : la première tour du héros, faute de portrait dédié. */
    public Item icon() {
        return this.towers.getFirst().icon();
    }

    public String translationKey() {
        return "dungeon_defenders.hero." + this.name().toLowerCase(Locale.ROOT);
    }

    /** Le héros de ce joueur, lu depuis l'attachment. */
    public static HeroDefinition of(Player player) {
        return values()[player.getData(ModAttachments.HERO)];
    }

    /** @return vrai si ce héros peut poser cette tour — l'autorité vit côté serveur. */
    public boolean canPlace(TowerDefinition tower) {
        return this.towers.contains(tower);
    }
}
