package com.github.c0c0tier.dungeon_defenders.entity;

import com.github.c0c0tier.dungeon_defenders.init.ModAttachments;
import com.github.c0c0tier.dungeon_defenders.init.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

// Cristal de mana : drop d'un monstre à sa mort (voir ModEvents.onMonsterDeath), ramassé en
// marchant dessus — jamais un item d'inventaire, comme l'expérience vanilla. Premier vrai
// Entity custom du mod.
//
// extends ExperienceOrb : pas une simple inspiration, une vraie sous-classe. ExperienceOrb a
// déjà tout ce qu'il faut pour ce comportement (flotte, gravité, se magnétise vers le joueur le
// plus proche, fusionne avec les cristaux voisins, disparaît après un temps) — seul le point
// d'entrée du ramassage, playerTouch(Player) (public, pas final), a besoin d'être réécrit pour
// donner du mana plutôt que de l'XP vanilla.
//
// Point de sécurité : ExperienceOrb fusionne automatiquement les orbes proches de même valeur
// (scanForMerges/tryMergeToExisting, private, non surchargeables) — sans précaution, un cristal
// de mana pourrait fusionner avec une VRAIE orbe d'XP vanilla si elles ont la même valeur
// numérique, corrompant le ramassage. Réglé à la racine par ModEvents.onExperienceDrop, qui
// annule LivingExperienceDropEvent pour tout Monster : plus aucune vraie orbe d'XP ne peut
// exister dans une partie de ce mod.
public class ManaCrystalEntity extends ExperienceOrb {

    public ManaCrystalEntity(Level level, double x, double y, double z, int manaValue) {
        super(ModEntities.MANA_CRYSTAL.get(), level);
        this.setPos(x, y, z);
        if (!level.isClientSide()) {
            this.setYRot(this.random.nextFloat() * 360.0F);
            this.setDeltaMovement(
                    (this.random.nextDouble() * 0.2D - 0.1D) * 2.0D,
                    this.random.nextDouble() * 0.2D * 2.0D,
                    (this.random.nextDouble() * 0.2D - 0.1D) * 2.0D);
        }
        this.setValue(manaValue);
    }

    // Requis par EntityType.EntityFactory (chargement depuis le NBT, etc.) — voir
    // init/ModEntities.java.
    public ManaCrystalEntity(EntityType<? extends ExperienceOrb> type, Level level) {
        super(type, level);
    }

    @Override
    public void playerTouch(Player player) {
        if (!(player instanceof ServerPlayer) || player.takeXpDelay != 0) {
            return;
        }

        player.takeXpDelay = 2;
        // Anime le ramassage (son + particule) — même mécanisme que l'XP vanilla, purement
        // visuel, sans rapport avec la valeur en mana donnée ci-dessous.
        player.take(this, 1);

        int currentMana = player.getData(ModAttachments.MANA);
        int newMana = Math.min(ModAttachments.MAX_MANA, currentMana + this.getValue());
        player.setData(ModAttachments.MANA, newMana);
        player.syncData(ModAttachments.MANA);

        this.discard();
    }
}
