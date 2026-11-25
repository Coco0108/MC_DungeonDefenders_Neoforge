package com.github.c0c0tier.dungeon_defenders.atelier;

import com.geckolib.animatable.GeoAnimatable;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.model.GeoModel;
import com.geckolib.renderer.base.GeoRenderState;

/**
 * An animation controller that goes back to its state handler (idle/walk) once a triggered animation has played.
 *
 * <p>In GeckoLib 5.5, a triggered animation that ends leaves the controller "finished" but still marked as triggered:
 * the state handler is never called again, so the monster stays in its bind pose, without any animation, after its
 * first attack. Here, a finished triggered animation is cleared before the usual update, which then calls the state
 * handler and blends from the last frame of that animation into idle or walk.
 *
 * <p>Repris tel quel de l'Atelier (entity/ReturningAnimationController.java).
 */
final class ReturningAnimationController<T extends GeoAnimatable> extends AnimationController<T> {
    ReturningAnimationController(String name, int transitionTicks, AnimationStateHandler<T> stateHandler) {
        super(name, transitionTicks, stateHandler);
    }

    @Override
    protected boolean checkControllerState(T animatable, GeoRenderState renderState, AnimatableManager<T> manager,
            GeoModel<T> geoModel) {
        if (triggeredAnimTime != NOT_TRIGGERED && timelineTime == FINISHED_ANIMATING && timeline != null) {
            triggeredAnimTime = NOT_TRIGGERED;
            // Back on the last frame, so that the next animation starts with a transition from it.
            timelineTime = timeline.lastAnimationEndTime();
        }
        return super.checkControllerState(animatable, renderState, manager, geoModel);
    }
}
