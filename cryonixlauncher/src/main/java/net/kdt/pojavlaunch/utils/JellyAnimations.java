package net.kdt.pojavlaunch.utils;

import android.animation.TimeInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ScrollView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Shared CryonixLauncher motion system.
 *
 * The motion language is inspired by the short, tactile screen/press choreography
 * used by CS Launcher Plus, but implemented independently for CryonixLauncher.
 * This keeps the existing launcher animation API while adding the same family of
 * screen reveals, staggered cards, springy press feedback and slide transitions.
 */
public final class JellyAnimations {

    private static final TimeInterpolator EMPHASIZED_DECELERATE =
            new android.view.animation.PathInterpolator(0.05f, 0.7f, 0.1f, 1f);

    /** Small damped spring used for tactile release feedback. */
    private static final TimeInterpolator SOFT_JELLY = new TimeInterpolator() {
        @Override
        public float getInterpolation(float t) {
            if (t <= 0f) return 0f;
            if (t >= 1f) return 1f;
            double envelope = Math.exp(-7.0 * t);
            return (float) (1.0 - envelope * Math.cos(9.0 * t));
        }
    };

    private static final Map<View, Boolean> PRESS_ATTACHED = new WeakHashMap<>();

    private JellyAnimations() {
    }

    /**
     * Screen entrance: fade + emphasized deceleration + a tiny scale settle.
     * Kept deliberately subtle so it matches the CryonixLauncher video style.
     */
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
                .setDuration(260L)
                .setInterpolator(EMPHASIZED_DECELERATE)
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

        // Apply the same tactile response to newly attached clickable controls.
        root.post(() -> attachTouchFeedback(root));
    }

    /**
     * CS-style alias for a screen reveal. Existing callers can use the same
     * shared motion language without adding another animation framework.
     */
    public static void revealScreen(View root) {
        animateScreen(root);
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
                .setDuration(210L)
                .setInterpolator(SOFT_JELLY)
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

    /**
     * Shared press feedback: instant press-in, springy release.
     * Returning false from the listener keeps the view's click listener working.
     */
    public static void pressFeedback(View... views) {
        if (views == null) return;
        for (View view : views) attachPress(view);
    }

    /** Applies press feedback to every clickable descendant of a screen. */
    public static void attachTouchFeedback(View root) {
        if (root == null) return;
        walkClickable(root);
    }

    /** Small selection/emphasis spring. */
    public static void springScale(View view, float targetScale) {
        if (view == null) return;
        view.animate().cancel();
        view.animate()
                .scaleX(targetScale)
                .scaleY(targetScale)
                .setDuration(240L)
                .setInterpolator(SOFT_JELLY)
                .start();
    }

    /** Edge slide used for sidebars, cards and panels. */
    public static void slideIn(View view, int direction, long startDelay) {
        if (view == null) return;
        float distance = 28f * view.getResources().getDisplayMetrics().density;

        view.animate().cancel();
        view.setAlpha(0f);
        if (direction < 0) {
            view.setTranslationX(-distance);
        } else if (direction > 0) {
            view.setTranslationX(distance);
        } else {
            view.setTranslationY(distance);
        }

        view.animate()
                .alpha(1f)
                .translationX(0f)
                .translationY(0f)
                .setStartDelay(Math.max(0L, startDelay))
                .setDuration(300L)
                .setInterpolator(EMPHASIZED_DECELERATE)
                .start();
    }

    /** Staggered direct-child entrance for cards/rows. */
    public static void stagger(ViewGroup container) {
        if (container == null) return;
        for (int i = 0; i < container.getChildCount(); i++) {
            View child = container.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;

            child.animate().cancel();
            child.setAlpha(0f);
            child.setTranslationY(12f);

            child.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setStartDelay(Math.min(i * 34L, 320L))
                    .setDuration(300L)
                    .setInterpolator(EMPHASIZED_DECELERATE)
                    .start();
        }
    }

    private static void attachPress(final View view) {
        if (view == null || !view.isClickable()) return;
        synchronized (PRESS_ATTACHED) {
            if (PRESS_ATTACHED.containsKey(view)) return;
            PRESS_ATTACHED.put(view, Boolean.TRUE);
        }

        view.setOnTouchListener(new View.OnTouchListener() {
            boolean inside;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        inside = true;
                        v.animate().cancel();
                        v.setScaleX(0.94f);
                        v.setScaleY(0.94f);
                        v.setAlpha(0.84f);
                        break;
                    case MotionEvent.ACTION_MOVE:
                        boolean nowInside = event.getX() >= 0
                                && event.getX() <= v.getWidth()
                                && event.getY() >= 0
                                && event.getY() <= v.getHeight();
                        if (inside != nowInside) {
                            inside = nowInside;
                            if (inside) {
                                v.setScaleX(0.94f);
                                v.setScaleY(0.94f);
                                v.setAlpha(0.84f);
                            } else {
                                release(v);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        release(v);
                        break;
                    default:
                        break;
                }
                return false;
            }
        });
    }

    private static void release(View view) {
        view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(430L)
                .setInterpolator(SOFT_JELLY)
                .start();
    }

    private static void walkClickable(View view) {
        if (view == null || shouldSkip(view)) return;

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            if (group.isClickable() && group.hasOnClickListeners()) {
                attachPress(group);
                return;
            }
            for (int i = 0; i < group.getChildCount(); i++) {
                walkClickable(group.getChildAt(i));
            }
        } else if (view.isClickable() && view.hasOnClickListeners()) {
            attachPress(view);
        }
    }

    private static boolean shouldSkip(View view) {
        return view instanceof RecyclerView
                || view instanceof AdapterView
                || view instanceof ScrollView
                || view instanceof android.widget.HorizontalScrollView
                || view instanceof android.widget.SeekBar
                || view instanceof android.widget.EditText;
    }

    private static void animateChildren(ViewGroup parent) {
        // Never reposition RecyclerView/adapter children; Android owns their lifecycle.
        if (parent instanceof AdapterView || parent instanceof RecyclerView) return;

        long index = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;

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
                    .setDuration(240L)
                    .setInterpolator(EMPHASIZED_DECELERATE)
                    .withEndAction(() -> child.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(70L)
                            .setInterpolator(new DecelerateInterpolator())
                            .start())
                    .start();

            if (child instanceof ViewGroup
                    && !(child instanceof ScrollView)
                    && !(child instanceof RecyclerView)) {
                animateChildren((ViewGroup) child);
            }
            index++;
        }
    }
}
