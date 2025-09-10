package net.kdt.pojavlaunch.fragments;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ExpandableListAdapter;

import androidx.annotation.NonNull;

import net.kdt.pojavlaunch.modloaders.ForgelikeUtils;
import net.kdt.pojavlaunch.modloaders.NeoforgeVersionListAdapter;

import java.util.List;

import git.artdeell.mojo.R;

public class NeoforgeInstallFragment extends ForgelikeInstallFragment {
    public static final String TAG = "NeoforgeInstallFragment";
    public NeoforgeInstallFragment() {
        super(ForgelikeUtils.NEOFORGE_UTILS, TAG);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
    }

    @Override
    public int getTitleText() {
        return R.string.neoforge_dl_select_version;
    }

    @Override
    public int getNoDataMsg() {
        return R.string.neoforge_dl_no_installer;
    }

    @Override
    public ExpandableListAdapter createAdapter(List<String> versionList, LayoutInflater layoutInflater) {
        return new NeoforgeVersionListAdapter(versionList, layoutInflater);
    }
}
