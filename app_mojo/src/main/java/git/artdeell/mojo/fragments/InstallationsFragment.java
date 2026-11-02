package git.artdeell.mojo.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.io.IOException;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.instances.DisplayInstance;
import git.artdeell.mojo.instances.Instances;
import git.artdeell.mojo.utils.ForkUi;

/** "Installations" screen: all instances, tap one to select it, with edit and new buttons. */
public class InstallationsFragment extends Fragment {
    public static final String TAG = "InstallationsFragment";

    private LinearLayout mList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(git.artdeell.mojo.utils.McStyle.screenBackground(c));
        root.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 12), ForkUi.dp(c, 16), ForkUi.dp(c, 12));

        LinearLayout header = ForkUi.row(c);
        header.addView(new View(c), ForkUi.weight(1f));
        Button create = ForkUi.button(c, getString(R.string.fork_installs_new), true);
        create.setOnClickListener(v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                ProfileTypeSelectFragment.TAG, null));
        header.addView(create, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header);

        TextView hint = ForkUi.text(c, getString(R.string.fork_installs_hint), 13, false);
        hint.setPadding(0, ForkUi.dp(c, 4), 0, ForkUi.dp(c, 8));
        root.addView(hint);

        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        MojoApplication.sExecutorService.execute(() -> {
            try {
                Instances instances = Instances.loadDisplay();
                Tools.runOnUiThread(() -> show(instances));
            } catch (IOException e) {
                Tools.runOnUiThread(() -> { if(getContext() != null) Tools.showError(getContext(), e); });
            }
        });
    }

    private void show(Instances instances) {
        Context c = getContext();
        if(c == null || mList == null) return;
        mList.removeAllViews();
        for(int i = 0; i < instances.list.size(); i++) {
            DisplayInstance instance = instances.list.get(i);
            boolean selected = i == instances.selectedIndex;

            LinearLayout row = ForkUi.row(c);
            row.setBackground(ForkUi.box(c, selected ? ForkUi.CARD_SELECTED : ForkUi.CARD,
                    selected ? ForkUi.GREEN : 0xFF111111, selected ? 2 : 1));
            row.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 10), ForkUi.dp(c, 8), ForkUi.dp(c, 10));

            LinearLayout texts = new LinearLayout(c);
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.addView(ForkUi.text(c, String.valueOf(instance.name), 16, true));
            TextView version = ForkUi.text(c, String.valueOf(instance.versionId), 13, false);
            version.setTextColor(0xFFBBBBBB);
            texts.addView(version);
            row.addView(texts, ForkUi.weight(1f));

            if(selected) {
                TextView mark = ForkUi.text(c, getString(R.string.fork_installs_selected), 12, true);
                mark.setTextColor(0xFF7CD65A);
                mark.setGravity(Gravity.CENTER);
                mark.setPadding(0, 0, ForkUi.dp(c, 8), 0);
                row.addView(mark);
            }
            Button edit = ForkUi.button(c, getString(R.string.fork_installs_edit), false);
            edit.setOnClickListener(v -> {
                Instances.setSelectedInstance(instance);
                Tools.swapFragment(requireActivity(), InstanceEditorFragment.class, InstanceEditorFragment.TAG, null);
            });
            row.addView(edit);

            row.setOnClickListener(v -> {
                Instances.setSelectedInstance(instance);
                load();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = ForkUi.dp(c, 6);
            mList.addView(row, lp);
        }
    }
}
