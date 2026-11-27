package git.artdeell.mojo.utils;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import git.artdeell.mojo.R;

/**
 * A class managing fragments of a provided activity
 */
public class FragmentUtils {

    /**
     * Swap to the specified fragment
     * @param fragmentActivity activity
     * @param fragmentClass fragment
     * @param fragmentTag tag of the fragment
     * @param bundle Bundle
     */
    public static void swapFragment(FragmentActivity fragmentActivity , Class<? extends Fragment> fragmentClass,
                                    @Nullable String fragmentTag, @Nullable Bundle bundle) {
        fragmentActivity.getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .addToBackStack(fragmentClass.getName())
                .replace(R.id.container_fragment, fragmentClass, bundle, fragmentTag).commit();
    }

    /**
     * Clear fragment stack and push user back to the main menu (main fragment)
     * @param fragmentActivity activity
     */
    public static void backToMainMenu(FragmentActivity fragmentActivity) {
        fragmentActivity.getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

    }

    /**
     * Remove current fragment i.e. go back
     * @param fragmentActivity activity
     */
    public static void removeCurrentFragment(FragmentActivity fragmentActivity){
        fragmentActivity.getSupportFragmentManager().popBackStack();
    }
}
