package com.github.c0c0tier.dungeon_defenders.ability;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

// Compétence générique, la même pour tout héros (emplacement HEAL) — pas de champ dans
// HeroDefinition, donc un singleton plutôt qu'une instance par héros.
//
// 1 mana = 1 PV, chaque tick serveur (20/s) tant que la touche est maintenue — valeur de test,
// pas encore équilibrée, comme les coûts de pose des tours. Le jeu de référence lie plutôt le
// débit à une statistique "Casting Rate" qui n'existe pas dans ce mod ; en attendant, le ratio
// est fixe.
//
// **Interrompue par un coup reçu**, en plus de PV pleins/mana épuisé (voir canContinue) : c'est
// ModEvents qui écoute LivingDamageEvent.Post et coupe la canalisation — comportement confirmé
// pour Heal Self dans le jeu de référence, PAS extrapolé à Repair ni Blood Rage (aucune source
// ne le documente pour elles).
public final class HealAbility implements ChannelAbility {

    public static final HealAbility INSTANCE = new HealAbility();

    private static final int MANA_COST_PER_TICK = 1;
    private static final float HEAL_PER_TICK = 1.0F;

    private HealAbility() {
    }

    @Override
    public Component displayName() {
        return Component.translatable("dungeon_defenders.ability.heal");
    }

    @Override
    public Item icon() {
        // Association de soin la plus lisible en vanilla — placeholder, comme les autres
        // textures réutilisées dans le mod.
        return Items.GOLDEN_APPLE;
    }

    @Override
    public int manaCostPerTick() {
        return MANA_COST_PER_TICK;
    }

    @Override
    public boolean canContinue(ServerPlayer player, @Nullable BlockPos target) {
        return player.getHealth() < player.getMaxHealth()
                && player.getData(ModAttachments.MANA) >= MANA_COST_PER_TICK;
    }

    @Override
    public void applyTick(ServerPlayer player, @Nullable BlockPos target) {
        int currentMana = player.getData(ModAttachments.MANA);
        player.setData(ModAttachments.MANA, currentMana - MANA_COST_PER_TICK);
        player.heal(HEAL_PER_TICK);
    }
}
