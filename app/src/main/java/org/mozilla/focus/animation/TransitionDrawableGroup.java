package org.mozilla.focus.animation;

import android.graphics.drawable.TransitionDrawable;

import java.util.Collection;
import java.util.Collections;

/**
 * A class to allow {@link TransitionDrawable}'s animations to play together: similar to {@link android.animation.AnimatorSet}.
 */
public class TransitionDrawableGroup {
    private final Collection<TransitionDrawable> transitionDrawables;

    public TransitionDrawableGroup(final Collection<TransitionDrawable> c) {
        transitionDrawables = Collections.unmodifiableCollection(c);
    }

    public void startTransition(final int durationMillis) {
        // In theory, there are no guarantees these will play together.
        // In practice, I haven't noticed any problems.
        for (final TransitionDrawable transitionDrawable : transitionDrawables) {
            transitionDrawable.startTransition(durationMillis);
        }
    }

    public void resetTransition() {
        for (final TransitionDrawable transitionDrawable : transitionDrawables) {
            transitionDrawable.resetTransition();
        }
    }
}
