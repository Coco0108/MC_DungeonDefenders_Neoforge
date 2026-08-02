package com.github.c0c0tier.dungeon_defenders.block.entity;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class EterniaCrystalBlockEntity extends BlockEntity {

    public static final int DEFAULT_HEALTH = 100;
    private int crystalHealth = DEFAULT_HEALTH;

    // On stocke l'UUID du texte flottant pour pouvoir le retrouver
    private UUID textDisplayUUID = null;

    public EterniaCrystalBlockEntity(BlockPos pos, BlockState state) {
        // Correction ici : on passe directement par le Holder ou une référence paresseuse
        super(DungeonDefendersMod.ETERNIA_CRYSTAL_BE.get(), pos, state);
    }

    public int getCrystalHealth() {
        return this.crystalHealth;
    }

    public void setCrystalHealth(int health) {
        this.crystalHealth = health;
        setChanged();
        // updateTextDisplay();

        this.level.players().forEach(player ->
                player.sendSystemMessage(Component.literal("Le cristal a maintenant " + this.crystalHealth + " PV."))
        );

        if (this.crystalHealth <= 0 && this.level != null && !this.level.isClientSide()) {
            // On détruit le bloc et on supprime le texte flottant
            // removeTextDisplay();

            // 1. On détruit le bloc (le false empêche le drop de l'item)
            this.level.destroyBlock(this.worldPosition, false);

            // 2. On prévient TOUS les joueurs aux alentours (pas seulement celui qui a cliqué)
            this.level.players().forEach(player ->
                    player.sendSystemMessage(Component.literal("§c§lLe cristal a été détruit !! Game Over !"))
            );
        }
    }

    // --- LOGIQUE DU TEXTE FLOTTANT ---

    private void updateTextDisplay() {
        if (this.level == null || this.level.isClientSide()) return;
        ServerLevel serverLevel = (ServerLevel) this.level;

        // Message de PV coloré (Vert si haut, Orange si moyen, Rouge si bas)
        String color = this.crystalHealth > 50 ? "§a" : (this.crystalHealth > 20 ? "§6" : "§c");
        Component text = Component.literal("§b§lEternia Crystal\n" + color + this.crystalHealth + " / " + DEFAULT_HEALTH + " PV");

        if (this.textDisplayUUID != null) {
            // Si le texte existe déjà, on le cherche et on le met à jour
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.textDisplayUUID);
            if (entity instanceof Display.TextDisplay textDisplay) {
                textDisplay.setText(text);
                return;
            }
        }

        // Si le texte n'existe pas encore (ex: premier placement), on le crée
        // On le place au centre du bloc (X + 0.5, Z + 0.5) et 3.2 blocs plus haut (au-dessus de ta grande hitbox !)
        Display.TextDisplay textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, this.level);
        textDisplay.setPos(new Vec3(this.worldPosition.getX() + 0.5D, this.worldPosition.getY() + 3.2D, this.worldPosition.getZ() + 0.5D));
        textDisplay.setText(text);

        // Options d'affichage pratiques :
        textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER); // Tourne face au joueur
        textDisplay.setViewRange(30F); // Visible à 30 blocs à la ronde

        serverLevel.addFreshEntity(textDisplay);
        this.textDisplayUUID = textDisplay.getUUID();
    }

    private void removeTextDisplay() {
        if (this.level != null && !this.level.isClientSide() && this.textDisplayUUID != null) {
            ServerLevel serverLevel = (ServerLevel) this.level;
            net.minecraft.world.entity.Entity entity = serverLevel.getEntity(this.textDisplayUUID);
            if (entity != null) {
                entity.discard();
            }
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        // removeTextDisplay(); // Supprime le texte si le bloc est miné/supprimé par d'autres moyens
    }

    @Override
    public void onLoad() {
        super.onLoad();
        // Force la création/mise à jour du texte quand le chunk se charge
        // updateTextDisplay();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        // On écrit l'entier dans le flux de sortie de NeoForge
        output.putInt("CrystalHealth", this.crystalHealth);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // getIntOr renvoie la valeur sauvegardée, ou la valeur par défaut si absente
        this.crystalHealth = input.getIntOr("CrystalHealth", DEFAULT_HEALTH);
    }
}