package net.kdt.pojavlaunch.utils;

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.AdapterView;
import android.widget.ScrollView;

import androidx.recyclerview.widget.RecyclerView;

public final class JellyAnimations {

    private JellyAnimations() {
    }

    /** Subtle jelly entrance: fade + tiny elastic scale, without a large bounce. */
    public static void animateScreen(View root) {
        if (root == null) return;

        root.animate().cancel();
        root.setAlpha(0f);
        root.setScaleX(0.985f);
        root.setScaleY(0.985f);
        root.setTranslationY(5f);

        root.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1.006f)
                .scaleY(1.006f)
                .setDuration(220L)
                .setInterpolator(new DecelerateInterpolator(1.6f))
                .withEndAction(() -> root.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(90L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();

        if (root instanceof ViewGroup) {
            animateChildren((ViewGroup) root);
        }
    }

    /** Subtle dialog jelly: quick fade and a very small elastic settle. */
    public static void animateDialog(View dialogRoot) {
        if (dialogRoot == null) return;

        dialogRoot.animate().cancel();
        dialogRoot.setAlpha(0f);
        dialogRoot.setScaleX(0.96f);
        dialogRoot.setScaleY(0.96f);
        dialogRoot.setTranslationY(6f);

        dialogRoot.animate()
                .alpha(1f)
                .translationY(0f)
                .scaleX(1.012f)
                .scaleY(1.012f)
                .setDuration(190L)
                .setInterpolator(new OvershootInterpolator(1.05f))
                .withEndAction(() -> dialogRoot.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(80L)
                        .setInterpolator(new DecelerateInterpolator())
                        .start())
                .start();

        if (dialogRoot instanceof ViewGroup) {
            animateChildren((ViewGroup) dialogRoot);
        }
    }

    private static void animateChildren(ViewGroup parent) {
        // Preference screens are backed by RecyclerView. Animating/repositioning
        // its recycled children can leave preference rows invisible or off-screen.
        if (parent instanceof AdapterView || parent instanceof RecyclerView) return;

        long index = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;
            if (child.getParent() instanceof AdapterView) continue;

            child.animate().cancel();
            child.setAlpha(0f);
            child.setTranslationY(7f);
            child.setScaleX(0.99f);
            child.setScaleY(0.99f);

            long delay = Math.min(index * 18L, 90L);

            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .scaleX(1.003f)
                    .scaleY(1.003f)
                    .setStartDelay(delay)
                    .setDuration(210L)
                    .setInterpolator(new DecelerateInterpolator(1.5f))
                    .withEndAction(() -> child.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(70L)
                            .setInterpolator(new DecelerateInterpolator())
                            .start());

            if (child instanceof ViewGroup && !(child instanceof ScrollView) && !(child instanceof RecyclerView)) {
                animateChildren((ViewGroup) child);
            }
            index++;
        }
    }
}
