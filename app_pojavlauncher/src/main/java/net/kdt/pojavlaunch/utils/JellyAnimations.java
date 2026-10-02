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

    /** Shared jelly-style entrance for full screens. */
    public static void animateScreen(View root) {
        if (root == null) return;

        root.setAlpha(0f);
        root.setScaleX(0.965f);
        root.setScaleY(0.965f);
        root.setTranslationY(12f);

        root.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1.015f)
                .scaleY(1.015f)
                .setDuration(420L)
                .setInterpolator(new OvershootInterpolator(1.25f))
                .withEndAction(() -> root.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220L)
                        .setInterpolator(new OvershootInterpolator(2.6f))
                        .start())
                .start();

        if (root instanceof ViewGroup) {
            animateChildren((ViewGroup) root, 0);
        }
    }

    /** Shared jelly-style entrance for dialogs/popups. */
    public static void animateDialog(View dialogRoot) {
        if (dialogRoot == null) return;

        dialogRoot.setAlpha(0f);
        dialogRoot.setScaleX(0.82f);
        dialogRoot.setScaleY(0.82f);
        dialogRoot.setTranslationY(24f);

        dialogRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1.08f)
                .scaleY(1.08f)
                .setDuration(360L)
                .setInterpolator(new OvershootInterpolator(1.65f))
                .withEndAction(() -> dialogRoot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(240L)
                        .setInterpolator(new OvershootInterpolator(3.0f))
                        .start())
                .start();

        if (dialogRoot instanceof ViewGroup) {
            animateChildren((ViewGroup) dialogRoot, 0);
        }
    }

    private static void animateChildren(ViewGroup parent, int depth) {
        if (parent instanceof AdapterView) {
            return;
        }

        long index = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;

            // Scroll containers animate as a single surface; their visible content is
            // deliberately not treated as individual list rows.
            if (child instanceof ScrollView) {
                animateSurface(child, Math.min(index * 35L, 300L));
                index++;
                continue;
            }

            if (child.getParent() instanceof android.widget.AdapterView) continue;

            child.setAlpha(0f);
            child.setTranslationY(24f);
            child.setScaleX(0.90f);
            child.setScaleY(0.90f);

            long delay = Math.min(index * 35L, 300L);

            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.055f)
                    .scaleY(1.055f)
                    .setStartDelay(delay)
                    .setDuration(460L)
                    .setInterpolator(new OvershootInterpolator(1.7f))
                    .withEndAction(() -> child.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(190L)
                            .setInterpolator(new OvershootInterpolator(2.8f))
                            .start())
                    .start();

            if (child instanceof ViewGroup && !(child instanceof ScrollView)) {
                animateChildren((ViewGroup) child, depth + 1);
            }

            index++;
        }
    }

    private static void animateSurface(View view, long delay) {
        view.setAlpha(0f);
        view.setTranslationY(20f);
        view.setScaleX(0.94f);
        view.setScaleY(0.94f);

        view.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1.035f)
                .scaleY(1.035f)
                .setStartDelay(delay)
                .setDuration(440L)
                .setInterpolator(new OvershootInterpolator(1.5f))
                .withEndAction(() -> view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(180L)
                        .setInterpolator(new OvershootInterpolator(2.5f))
                        .start())
                .start();
    }
}
