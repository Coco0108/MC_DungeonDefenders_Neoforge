package com.github.c0c0tier.dungeon_defenders.init;

import com.github.c0c0tier.dungeon_defenders.DungeonDefendersMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

// Une map jouable, telle qu'elle est présentée dans l'écran de choix. Remplace l'ancien enum
// GameMap : les maps ne sont plus une liste fermée écrite en dur, mais découvertes à l'exécution
// parmi les structures disponibles (voir MapRegistry).
//
// **Ce qui identifie une map, c'est sa structure** : l'identifiant `<namespace>:map/<id>` d'un
// fichier `.nbt`. Le namespace fait office de "pack" (la campagne du mod, ou un pack tiers) —
// voir packId(). Le reste des champs vient du bloc de configuration posé DANS la map (voir
// MapConfigBlock), donc il voyage à l'intérieur du .nbt : copier le fichier suffit à emporter
// ses réglages.
public record MapDefinition(
        Identifier structureId,
        String displayName,
        int order,
        int waveCount,
        float scoreMultiplier) {

    /**
     * Version du format des données écrites par le bloc de configuration. Incrémenter en cas de
     * changement incompatible : les packs déjà publiés continueront d'être lus avec les valeurs
     * par défaut pour ce qu'ils ne connaissent pas, plutôt que de casser.
     */
    public static final int FORMAT_VERSION = 1;

    /** Préfixe de chemin qui distingue une structure de map d'une autre structure (la taverne). */
    public static final String STRUCTURE_PREFIX = "map/";

    public static final int DEFAULT_WAVE_COUNT = ModAttachments.MAX_WAVE;
    public static final float DEFAULT_SCORE_MULTIPLIER = 1.0F;
    public static final int DEFAULT_ORDER = 0;

    public static final StreamCodec<RegistryFriendlyByteBuf, MapDefinition> STREAM_CODEC =
            StreamCodec.composite(
                    Identifier.STREAM_CODEC, MapDefinition::structureId,
                    ByteBufCodecs.STRING_UTF8, MapDefinition::displayName,
                    ByteBufCodecs.VAR_INT, MapDefinition::order,
                    ByteBufCodecs.VAR_INT, MapDefinition::waveCount,
                    ByteBufCodecs.FLOAT, MapDefinition::scoreMultiplier,
                    MapDefinition::new);

    /** @return vrai si cet identifiant ressemble à une structure de map (`<ns>:map/<id>`). */
    public static boolean isMapStructure(Identifier id) {
        return id.getPath().startsWith(STRUCTURE_PREFIX) && id.getPath().length() > STRUCTURE_PREFIX.length();
    }

    /**
     * Le "pack" auquel appartient cette map, c'est-à-dire son namespace : {@code dungeon_defenders}
     * pour la campagne, celui du pack pour une extension. Aucune donnée à déclarer — le namespace
     * suffit à regrouper les maps d'un même jar ou datapack.
     */
    public String packId() {
        return this.structureId.getNamespace();
    }

    public boolean isCampaign() {
        return DungeonDefendersMod.MODID.equals(this.packId());
    }

    /** L'identifiant court de la map, sans le préfixe `map/`. */
    public String mapId() {
        return this.structureId.getPath().substring(STRUCTURE_PREFIX.length());
    }

    /**
     * Nom du pack affiché dans la colonne de gauche. Un pack peut fournir la traduction
     * {@code dungeon_defenders.map_pack.<namespace>} dans son propre fichier de langue ; sinon on
     * retombe sur le namespace brut plutôt que d'afficher une clé de traduction crue.
     */
    public Component packDisplayName() {
        return Component.translatableWithFallback("dungeon_defenders.map_pack." + this.packId(), this.packId());
    }

    /**
     * Nom de la map. Saisi librement dans le bloc de configuration (pas de clé de langue : une
     * map créée en jeu n'a aucun moyen d'en enregistrer une), avec repli sur son identifiant tant
     * qu'il n'a pas été renseigné.
     */
    public Component mapDisplayName() {
        return this.displayName.isBlank() ? Component.literal(this.mapId()) : Component.literal(this.displayName);
    }

    /**
     * Image d'aperçu, cherchée dans les ressources du pack auquel appartient la map. Absente,
     * le jeu affichera la texture "manquante" habituelle — c'est le cas de toute map créée en
     * jeu tant que la capture d'aperçu n'existe pas (phase 2).
     */
    public Identifier previewTexture() {
        return Identifier.fromNamespaceAndPath(this.packId(), "textures/gui/maps/" + this.mapId() + ".png");
    }
}
