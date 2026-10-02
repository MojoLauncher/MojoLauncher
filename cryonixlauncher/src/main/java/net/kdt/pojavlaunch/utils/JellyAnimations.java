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
    private static final Map<View, Long> LAST_SCREEN_ANIMATION = new WeakHashMap<>();

    private JellyAnimations() {
    }

    /**
     * Screen entrance: fade + emphasized deceleration + a tiny scale settle.
     * Kept deliberately subtle so it matches the CryonixLauncher video style.
     */
    public static void animateScreen(View root) {
        if (root == null) return;

        // Activity + fragment callbacks can both request an entrance. Avoid
        // replaying the exact same root twice during one navigation event.
        synchronized (LAST_SCREEN_ANIMATION) {
            Long last = LAST_SCREEN_ANIMATION.get(root);
            long now = android.os.SystemClock.uptimeMillis();
            if (last != null && now - last < 180L) return;
            LAST_SCREEN_ANIMATION.put(root, now);
        }

        root.animate().cancel();
        root.setAlpha(0.985f);
        root.setScaleX(0.997f);
        root.setScaleY(0.997f);

        root.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(170L)
                .setInterpolator(EMPHASIZED_DECELERATE)
                .start();

        if (root instanceof ViewGroup) {
            animateCascade((ViewGroup) root, 0L, 0);
        }

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
        animateCascade(container, 0L, 0);
    }

    /**
     * Individual screen choreography. Each visible control gets its own
     * fade + tiny scale + directional jelly motion instead of the whole
     * screen appearing as one block.
     */
    private static void animateCascade(ViewGroup parent, long baseDelay, int depth) {
        if (parent == null || shouldSkip(parent)) return;

        int visibleIndex = 0;
        for (int i = 0; i < parent.getChildCount(); i++) {
            View child = parent.getChildAt(i);
            if (child == null || child.getVisibility() != View.VISIBLE) continue;

            child.animate().cancel();
            child.setAlpha(0f);
            child.setScaleX(0.965f);
            child.setScaleY(0.965f);

            // Alternate directions so the screen feels assembled rather than
            // dropping every item from the same point.
            int direction = (visibleIndex + depth) % 3;
            float distance = 12f;
            if (direction == 0) {
                child.setTranslationX(-distance);
                child.setTranslationY(5f);
            } else if (direction == 1) {
                child.setTranslationX(0f);
                child.setTranslationY(9f);
            } else {
                child.setTranslationX(distance);
                child.setTranslationY(5f);
            }

            long delay = Math.min(baseDelay + visibleIndex * 32L, 560L);
            child.animate()
                    .alpha(1f)
                    .translationX(0f)
                    .translationY(0f)
                    .scaleX(1.006f)
                    .scaleY(1.006f)
                    .setStartDelay(delay)
                    .setDuration(225L)
                    .setInterpolator(SOFT_JELLY)
                    .withEndAction(() -> child.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(85L)
                            .setInterpolator(new DecelerateInterpolator())
                            .start())
                    .start();

            // Let nested cards, text, icons and buttons enter separately too.
            if (child instanceof ViewGroup
                    && !(child instanceof RecyclerView)
                    && !(child instanceof AdapterView)
                    && !(child instanceof ScrollView)
                    && !(child instanceof android.widget.HorizontalScrollView)
                    && !(child instanceof android.widget.SeekBar)
                    && !(child instanceof android.widget.EditText)) {
                animateCascade((ViewGroup) child,
                        Math.min(delay + 42L, 600L), depth + 1);
            }
            visibleIndex++;
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
