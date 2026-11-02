package git.artdeell.mojo.fragments;

import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.utils.DownloadUtils;
import git.artdeell.mojo.utils.ForkUi;

/**
 * Patch notes from Mojang's launcher content feed (Java and Bedrock tabs).
 * Each entry expands to show its text. The feed is read defensively: a missing
 * field just hides that part.
 */
public class PatchNotesFragment extends Fragment {
    public static final String TAG = "PatchNotesFragment";
    private static final String BASE = "https://launchercontent.mojang.com/v2/";

    private LinearLayout mList;
    private TextView mStatus;
    private Button mJavaTab, mBedrockTab;
    private int mLoadId = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Context c = requireContext();
        LinearLayout root = new LinearLayout(c);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(git.artdeell.mojo.utils.McStyle.screenBackground(c));
        root.setPadding(ForkUi.dp(c, 16), ForkUi.dp(c, 12), ForkUi.dp(c, 16), ForkUi.dp(c, 12));

        LinearLayout tabs = ForkUi.row(c);
        mJavaTab = ForkUi.button(c, getString(R.string.fork_patch_java), true);
        mBedrockTab = ForkUi.button(c, getString(R.string.fork_patch_bedrock), false);
        LinearLayout.LayoutParams l1 = ForkUi.weight(1f);
        l1.rightMargin = ForkUi.dp(c, 4);
        LinearLayout.LayoutParams l2 = ForkUi.weight(1f);
        l2.leftMargin = ForkUi.dp(c, 4);
        tabs.addView(mJavaTab, l1);
        tabs.addView(mBedrockTab, l2);
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tl.topMargin = ForkUi.dp(c, 8);
        tl.bottomMargin = ForkUi.dp(c, 8);
        root.addView(tabs, tl);

        mStatus = ForkUi.text(c, "", 14, false);
        root.addView(mStatus);
        ScrollView scroll = new ScrollView(c);
        mList = new LinearLayout(c);
        mList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(mList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        mJavaTab.setOnClickListener(v -> load("javaPatchNotes.json"));
        mBedrockTab.setOnClickListener(v -> load("bedrockPatchNotes.json"));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        load("javaPatchNotes.json");
    }

    private void load(String file) {
        final int id = ++mLoadId;
        boolean java = file.startsWith("java");
        mJavaTab.setAlpha(java ? 1f : 0.6f);
        mBedrockTab.setAlpha(java ? 0.6f : 1f);
        mList.removeAllViews();
        mStatus.setText(R.string.fork_patch_loading);
        MojoApplication.sExecutorService.execute(() -> {
            try {
                String json = DownloadUtils.downloadString(BASE + file);
                JsonArray entries = JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("entries");
                Tools.runOnUiThread(() -> { if(id == mLoadId) show(entries); });
            } catch (Exception e) {
                Tools.runOnUiThread(() -> {
                    if(id == mLoadId && getContext() != null) mStatus.setText(R.string.fork_patch_error);
                });
            }
        });
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }

    private void show(JsonArray entries) {
        Context c = getContext();
        if(c == null) return;
        mStatus.setText(entries.size() == 0 ? getString(R.string.fork_patch_error) : "");
        int shown = 0;
        for(JsonElement el : entries) {
            if(!el.isJsonObject() || shown++ >= 60) continue;
            JsonObject entry = el.getAsJsonObject();
            String title = str(entry, "title");
            String version = str(entry, "version");
            String type = str(entry, "type");
            String body = str(entry, "body");
            String path = str(entry, "contentPath");

            LinearLayout card = new LinearLayout(c);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setBackground(ForkUi.box(c, ForkUi.CARD, 0xFF111111, 1));
            card.setPadding(ForkUi.dp(c, 12), ForkUi.dp(c, 10), ForkUi.dp(c, 12), ForkUi.dp(c, 10));
            card.addView(ForkUi.text(c, title != null ? title : String.valueOf(version), 16, true));
            String sub = (version != null ? version : "") + (type != null ? "  ·  " + type : "");
            TextView subView = ForkUi.text(c, sub, 12, false);
            subView.setTextColor(0xFF7CD65A);
            card.addView(subView);

            TextView content = ForkUi.text(c, "", 13, false);
            content.setVisibility(View.GONE);
            content.setMovementMethod(LinkMovementMethod.getInstance());
            content.setPadding(0, ForkUi.dp(c, 8), 0, 0);
            card.addView(content);

            final String[] loaded = {body};
            card.setOnClickListener(v -> {
                if(content.getVisibility() == View.VISIBLE) { content.setVisibility(View.GONE); return; }
                content.setVisibility(View.VISIBLE);
                if(loaded[0] != null) { setHtml(content, loaded[0]); return; }
                if(path == null) { content.setText(R.string.fork_patch_error); return; }
                content.setText(R.string.fork_patch_loading);
                MojoApplication.sExecutorService.execute(() -> {
                    try {
                        JsonObject full = JsonParser.parseString(DownloadUtils.downloadString(BASE + path)).getAsJsonObject();
                        String html = str(full, "body");
                        loaded[0] = html;
                        Tools.runOnUiThread(() -> {
                            if(html != null) setHtml(content, html);
                            else content.setText(R.string.fork_patch_error);
                        });
                    } catch (Exception e) {
                        Tools.runOnUiThread(() -> content.setText(R.string.fork_patch_error));
                    }
                });
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = ForkUi.dp(c, 6);
            mList.addView(card, lp);
        }
    }

    private static void setHtml(TextView view, String html) {
        view.setText(Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT));
    }
}
