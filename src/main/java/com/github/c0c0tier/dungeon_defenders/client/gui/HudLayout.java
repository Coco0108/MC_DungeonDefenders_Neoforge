package com.github.c0c0tier.dungeon_defenders.client.gui;

// Constantes de mise en page partagées par le groupe de jauges en bas à gauche (mana, vie,
// expérience) : centralisées ici pour que les trois overlays restent alignés au pixel près
// sans dupliquer les mêmes valeurs magiques dans chaque classe. Le groupe haut droit
// (vague/ennemis/phase) n'en a pas besoin, ses overlays se chaînent directement entre eux
// (voir WaveOverlay.ROW_Y/ROW_HEIGHT).
final class HudLayout {
    static final int MARGIN = 4;
    // Losanges mana (gauche) / vie (droite) — voir DiamondGauge. Chaque losange occupe un
    // carré de 2*DIAMOND_RADIUS de côté.
    static final int DIAMOND_RADIUS = 24;
    static final int DIAMOND_GAP = 10;
    // Espace vertical entre la pointe basse des losanges et le haut de la barre d'expérience.
    static final int ROW_GAP = 4;
    // Hauteur de la barre d'expérience (horizontale, tout en bas).
    static final int EXPERIENCE_BAR_HEIGHT = 6;

    private HudLayout() {
    }
}
