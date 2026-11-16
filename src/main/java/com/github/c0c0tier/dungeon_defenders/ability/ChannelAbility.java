package com.github.c0c0tier.dungeon_defenders.ability;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

// Compétence maintenue : le joueur appuie et tient une touche, le mana se vide en continu tant
// qu'elle reste active — comme Heal Self, Repair Defense et Blood Rage dans le jeu de
// référence (à l'inverse de Circular Slice, qui est une salve instantanée, voir BurstAbility).
//
// Le serveur est seul juge de quand ça s'arrête (canContinue), à chaque tick, indépendamment
// de ce que fait le client : mana épuisé, cible invalide, joueur à PV pleins... Le client ne
// fait qu'annoncer "je commence"/"j'arrête" (voir PlayerAbilityChannels), il n'est jamais
// l'autorité sur la durée réelle de la canalisation.
public interface ChannelAbility extends HeroAbility {

    int manaCostPerTick();

    /** Vrai pour Repair (cible une tour) ; faux pour Heal et Blood Rage (sur soi, pas de cible). */
    default boolean requiresTarget() {
        return false;
    }

    /**
     * @return faux si la canalisation doit s'arrêter à ce tick (mana épuisé, PV pleins, cible
     *         invalide/hors de portée...). Appelé avant chaque {@link #applyTick}, y compris au
     *         tout premier tick — une compétence qui ne peut jamais démarrer se contente donc de
     *         renvoyer faux immédiatement, sans code de démarrage séparé.
     */
    boolean canContinue(ServerPlayer player, @Nullable BlockPos target);

    /**
     * Un tick d'effet (débit du mana, soin/réparation/buff). Appelé seulement si
     * {@link #canContinue} vient de renvoyer vrai.
     *
     * <p>Ne synchronise PAS le mana vers le client : c'est l'appelant (le tick loop de
     * {@code ModEvents.onPlayerTick}) qui le fait, à intervalle régulier plutôt qu'à chaque
     * appel — synchroniser à chaque tick pendant toute une canalisation serait vingt paquets
     * par seconde pour rien.
     */
    void applyTick(ServerPlayer player, @Nullable BlockPos target);
}
