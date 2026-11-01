package git.artdeell.mojo.utils;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.List;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import git.artdeell.mojo.authenticator.accounts.Account;
import git.artdeell.mojo.authenticator.accounts.Accounts;
import git.artdeell.mojo.authenticator.accounts.OfflineSkin;
import git.artdeell.mojo.authenticator.accounts.SkinLibrary;

/** Skin selector: saved skins with a model preview, applicable to offline and Microsoft accounts. */
public class SkinLibraryDialog {
    private static int dp(Context c, int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    /** @param onImport called when the user wants to import a new PNG from the gallery */
    public static void show(Context context, Runnable onImport) {
        List<SkinLibrary.Entry> skins = SkinLibrary.list(context);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(context, 16);
        root.setPadding(pad, pad / 2, pad, 0);

        SkinModelView preview = new SkinModelView(context);
        root.addView(preview, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 220)));

        TextView hint = new TextView(context);
        hint.setText(skins.isEmpty() ? R.string.fork_skins_empty : R.string.fork_skins_hint);
        hint.setPadding(0, dp(context, 6), 0, dp(context, 6));
        root.addView(hint);

        ListView list = new ListView(context);
        list.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        list.setAdapter(new ArrayAdapter<>(context, android.R.layout.simple_list_item_single_choice, skins));
        root.addView(list, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(context, 150)));

        final int[] selected = {-1};
        list.setOnItemClickListener((p, v, position, id) -> {
            selected[0] = position;
            Bitmap bmp = SkinLibrary.load(skins.get(position));
            if(bmp != null) preview.setSkin(bmp, SkinLibrary.isSlim(bmp));
        });
        if(!skins.isEmpty()) {
            list.setItemChecked(0, true);
            selected[0] = 0;
            Bitmap bmp = SkinLibrary.load(skins.get(0));
            if(bmp != null) preview.setSkin(bmp, SkinLibrary.isSlim(bmp));
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.fork_skins_title)
                .setView(root)
                .setNeutralButton(R.string.fork_skins_import, (d, w) -> onImport.run())
                .setNegativeButton(R.string.fork_skins_delete, null)
                .setPositiveButton(R.string.fork_skins_apply, null);
        AlertDialog dialog = builder.create();
        dialog.show();

        // Delete and apply keep the dialog open on failure, so they are wired after show()
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            if(selected[0] < 0) return;
            SkinLibrary.delete(skins.get(selected[0]));
            dialog.dismiss();
            show(context, onImport);
        });
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            if(selected[0] < 0) return;
            apply(context, skins.get(selected[0]));
            dialog.dismiss();
        });
    }

    private static void toast(Context context, int res, Object... args) {
        Toast.makeText(context, context.getString(res, args), Toast.LENGTH_LONG).show();
    }

    public static void apply(Context context, SkinLibrary.Entry entry) {
        apply(context, entry, null);
    }

    /** @param slimOverride true/false forces the arm model for Microsoft accounts, null detects it */
    public static void apply(Context context, SkinLibrary.Entry entry, Boolean slimOverride) {
        Account account = Accounts.getCurrent();
        if(account == null) { toast(context, R.string.fork_skins_no_account); return; }
        if(account.isLocal()) {
            try {
                OfflineSkin.setFromFile(context, account, entry.file);
                toast(context, R.string.fork_skins_applied_offline);
            } catch (Exception e) {
                toast(context, R.string.fork_skin_error);
            }
        } else if(account.isMicrosoft) {
            toast(context, R.string.fork_skins_uploading);
            Bitmap bmp = SkinLibrary.load(entry);
            boolean slim = slimOverride != null ? slimOverride : (bmp != null && SkinLibrary.isSlim(bmp));
            Handler main = new Handler(Looper.getMainLooper());
            MojoApplication.sExecutorService.execute(() -> {
                String error = SkinLibrary.uploadToMicrosoft(account, entry.file, slim);
                main.post(() -> {
                    if(error == null) toast(context, R.string.fork_skins_applied_online);
                    else toast(context, R.string.fork_skins_upload_error, error);
                });
            });
        } else {
            toast(context, R.string.fork_skins_unsupported);
        }
    }
}
