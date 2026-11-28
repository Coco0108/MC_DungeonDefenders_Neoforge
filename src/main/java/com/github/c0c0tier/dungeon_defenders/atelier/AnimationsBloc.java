package com.github.c0c0tier.dungeon_defenders.atelier;

import java.util.Map;

import org.jspecify.annotations.Nullable;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Les animations d'un bloc animé de l'Atelier ({@code anime} dans sa fiche, voir {@link FicheBloc.Anime}), partagées par
 * tous les block entities qui le dessinent : le bloc de décor lui-même ({@link BlocAnimeEntity}) et les blocs du jeu
 * qui empruntent son modèle (la forge, le mannequin, les deux cristaux). Repris de {@code objet/BlocAnimeEntity.java} de
 * l'Atelier, sorti dans un composant parce que les blocs du jeu ont déjà leur propre block entity.
 *
 * <p>Un seul contrôleur : au repos il boucle {@code idle}, ou {@code opened} quand le bloc est ouvert ; les autres rôles
 * sont déclenchés une fois par le serveur ({@link #jouer}), et GeckoLib les envoie à tous les joueurs autour. L'état
 * ouvert est enregistré dans le block entity et synchronisé, pour qu'un joueur qui arrive le voie ouvert.
 *
 * <p>Sans fiche animée (l'Atelier n'a rien déployé), aucun rôle : rien ne se joue, et le bloc garde son modèle fixe
 * ({@link #dessineParGeckoLib}).
 */
public final class AnimationsBloc {
    public static final String CONTROLLER = "bloc";
    private static final int TRANSITION_TICKS = 4;
    private static final String OUVERT = "ouvert";

    private final String blocAtelier;
    private final Map<String, String> roles;
    private boolean ouvert;
    private @Nullable String dernierRole;

    /** Les animations du bloc de l'Atelier {@code blocAtelier} ({@code dd_forge_v2}…), d'après sa fiche. */
    public AnimationsBloc(String blocAtelier) {
        this.blocAtelier = blocAtelier;
        this.roles = Atelier.anime(blocAtelier).map(FicheBloc.Anime::rolesAnimation).orElse(Map.of());
    }

    /** Vrai si le bloc posé est dessiné par GeckoLib (sa forme de rendu est alors invisible), faux pour le modèle fixe. */
    public static boolean dessineParGeckoLib(String blocAtelier) {
        return Atelier.anime(blocAtelier).isPresent();
    }

    public String blocAtelier() {
        return blocAtelier;
    }

    public boolean a(String role) {
        return roles.containsKey(role);
    }

    public boolean ouvert() {
        return ouvert;
    }

    /**
     * Joue un rôle une fois, pour tous les joueurs autour (à appeler côté serveur). {@code open} laisse le bloc ouvert
     * ({@code opened} boucle), {@code close} le referme. Renvoie faux si la fiche n'a pas ce rôle (rien ne se joue).
     */
    public boolean jouer(BlockEntity bloc, String role) {
        if (!roles.containsKey(role) || !(bloc instanceof GeoBlockEntity geo)) {
            return false;
        }
        if (role.equals("open") || role.equals("close")) {
            ouvert = role.equals("open");
            bloc.setChanged();
            if (bloc.getLevel() != null) {
                bloc.getLevel().sendBlockUpdated(bloc.getBlockPos(), bloc.getBlockState(), bloc.getBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        geo.triggerAnim(CONTROLLER, role);
        dernierRole = role;
        return true;
    }

    /** Le dernier rôle joué par {@link #jouer} côté serveur, null si aucun (gametests). */
    public @Nullable String dernierRole() {
        return dernierRole;
    }

    /** Remet le bloc fermé sans rien jouer (un écran ne peut pas rester ouvert d'une session à l'autre). */
    public void fermerSansAnimation() {
        ouvert = false;
    }

    private @Nullable RawAnimation boucle(String role) {
        String name = roles.get(role);
        return name == null ? null : RawAnimation.begin().thenLoop(name);
    }

    /** Le contrôleur du bloc, à ajouter dans {@code registerControllers} de son block entity. */
    public <T extends GeoAnimatable> void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        RawAnimation idle = boucle("idle");
        RawAnimation opened = boucle("opened");
        // Retour à la boucle de repos une fois un rôle joué (voir ReturningAnimationController).
        AnimationController<T> controller = new ReturningAnimationController<>(CONTROLLER, TRANSITION_TICKS, test -> {
            RawAnimation loop = ouvert && opened != null ? opened : idle;
            return loop == null ? PlayState.STOP : test.setAndContinue(loop);
        });
        // death reste sur sa dernière image : le bloc est retiré juste avant la fin (voir EterniaCrystalBlockEntity).
        roles.forEach((role, name) -> controller.triggerableAnim(role,
                role.equals("death") ? RawAnimation.begin().thenPlayAndHold(name) : RawAnimation.begin().thenPlay(name)));
        controllers.add(controller);
    }

    public void save(ValueOutput output) {
        output.putBoolean(OUVERT, ouvert);
    }

    public void load(ValueInput input) {
        ouvert = input.getBooleanOr(OUVERT, false);
    }
}
