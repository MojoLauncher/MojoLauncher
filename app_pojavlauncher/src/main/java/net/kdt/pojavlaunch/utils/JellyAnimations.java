package net.kdt.pojavlaunch.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ScrollView;

public final class JellyAnimations {

    private JellyAnimations() {
    }

    /**
     * Plays a lightweight jelly-style entrance animation for a screen.
     * This is intentionally UI-only and does not alter screen logic.
     */
    public static void animateScreen(View root) {
        if (root == null) return;

        root.setAlpha(0f);
        root.animate()
                .alpha(1f)
                .setDuration(260L)
                .setInterpolator(new DecelerateInterpolator())
                .start();

        if (root instanceof ViewGroup) {
            animateChildren((ViewGroup) root, 0);
        }
    }

    private static void animateChildren(ViewGroup parent, int depth) {
        if (parent instanceof ScrollView || parent instanceof AdapterView) {
            return;
        }

        long index = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;

            child.setAlpha(0f);
            child.setTranslationY(18f);
            child.setScaleX(0.94f);
            child.setScaleY(0.94f);

            long delay = Math.min(index * 28L, 280L);

            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.035f)
                    .scaleY(1.035f)
                    .setStartDelay(delay)
                    .setDuration(420L)
                    .setInterpolator(new OvershootInterpolator(1.45f))
                    .withEndAction(() -> child.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150L)
                            .setInterpolator(new OvershootInterpolator(2.0f))
                            .start())
                    .start();

            if (child instanceof ViewGroup) {
                animateChildren((ViewGroup) child, depth + 1);
            }

            index++;
        }
    }
}
