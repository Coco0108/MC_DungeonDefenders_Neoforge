package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.ability.BloodRageAbility;
import com.github.c0c0tier.dungeon_defenders.ability.BurstAbility;
import com.github.c0c0tier.dungeon_defenders.ability.ChannelAbility;
import com.github.c0c0tier.dungeon_defenders.ability.CircularSliceAbility;
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

    // Circular Slice et Blood Rage : les deux sorts de l'Écuyer confirmés dans le jeu de
    // référence (2026-09-03, voir doc/02-gameplay.md) — pas des sorts inventés. Heal et Repair
    // ne sont PAS ici : génériques à tout héros, voir HealAbility/RepairAbility.
    SQUIRE("squire", List.of(
            TowerDefinition.SPIKE_BLOCKADE,
            TowerDefinition.BOUNCER_BLOCKADE,
            TowerDefinition.SLICE_N_DICE_BLOCKADE,
            TowerDefinition.HARPOON_TURRET,
            TowerDefinition.BOWLING_BALL_TURRET,
            TowerDefinition.MORTAR_TURRET),
            CircularSliceAbility.INSTANCE, BloodRageAbility.INSTANCE);

    /** Héros attribué à un joueur qui n'a jamais choisi — voir ModAttachments.HERO. */
    public static final HeroDefinition DEFAULT = SQUIRE;

    private final String id;
    private final List<TowerDefinition> towers;
    private final BurstAbility spell1;
    private final ChannelAbility spell2;

    HeroDefinition(String id, List<TowerDefinition> towers, BurstAbility spell1, ChannelAbility spell2) {
        this.id = id;
        this.towers = towers;
        this.spell1 = spell1;
        this.spell2 = spell2;
    }

    public String id() {
        return this.id;
    }

    /** Les tours que ce héros peut poser, dans l'ordre où la roue les présente. */
    public List<TowerDefinition> towers() {
        return this.towers;
    }

    /** Sort de l'emplacement SPELL_1 — toujours une compétence en salve, voir BurstAbility. */
    public BurstAbility spell1() {
        return this.spell1;
    }

    /** Sort de l'emplacement SPELL_2 — toujours une compétence maintenue, voir ChannelAbility. */
    public ChannelAbility spell2() {
        return this.spell2;
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
