package com.github.c0c0tier.dungeon_defenders.client;

import net.neoforged.neoforge.common.ModConfigSpec;

// Préférences d'affichage HUD, propres à chaque joueur — distinct de Config.java (COMMON,
// valeurs de gameplay comme les PV du Cristal d'Eternia, lues aussi côté serveur). Type CLIENT :
// un seul fichier local (config/dungeon_defenders-client.toml), jamais envoyé au serveur ni lu
// par lui, jamais synchronisé entre joueurs — masquer un élément de son propre HUD ne doit rien
// changer pour les autres joueurs de la partie. Enregistré dans DungeonDefendersModClient
// (classe strictement cliente), jamais dans DungeonDefendersMod (chargée aussi sur un serveur
// dédié, qui n'a pas de HUD à configurer).
//
// Patron pour ajouter une nouvelle option d'affichage facultatif (voir aussi
// doc/04-guide-ajout-contenu.md) :
//   1. Un champ ici : BUILDER.comment("...").define("nomOption", valeurParDefaut) ;
//   2. Une clé de lang dungeon_defenders.configuration.<nomOption> (libellé affiché) ;
//   3. Dans le GuiLayer concerné, un if (!ClientDisplayConfig.MA_VALEUR.get()) { return; } tout
//      en haut de render(), après les gardes existantes (minecraft.level == null, hideGui...).
public class ClientDisplayConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_SCORE_GAIN_POPUP = BUILDER
            .comment("Afficher le \"+X\" flottant en bas à droite de l'écran à chaque gain de score.")
            .define("showScoreGainPopup", true);

    // Masqué par défaut (contrairement aux autres options, activées par défaut) : c'est bien
    // l'absence de contour qui est le comportement voulu, l'option n'existe que pour le
    // remettre — voir BlockOutlineClientEvents.
    public static final ModConfigSpec.BooleanValue SHOW_TOWER_BLOCK_OUTLINE = BUILDER
            .comment("Afficher le contour noir de sélection quand on vise une tour ou un cristal.")
            .define("showTowerBlockOutline", false);

    public static final ModConfigSpec SPEC = BUILDER.build();
}
