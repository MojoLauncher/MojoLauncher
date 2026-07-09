package net.kdt.pojavlaunch.tasks;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.util.Log;
import android.widget.Toast;

import com.kdt.mcgui.ProgressLayout;

import net.kdt.pojavlaunch.Tools;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import git.artdeell.mojo.R;

/**
 * A class for migrating data from other launcher installations
 */
public class DataMigrator {
    private final Uri uri;
    private final Activity activity;
    private double progress;
    private static final int MIN_FREE_SPACE = 2048; // required free space in megabytes
    private static final String[] TREE_PROJECTION = {
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_SIZE
    };
    /** Initialize data migrator
     * @param activity App activity
     * @param uri Uri to the external root directory of the source installation (i.e. /sdcard/Android/data/git.artdeell.../). Must have "files" subdir
     */
    public DataMigrator(Activity activity, Uri uri){
        this.activity = activity;
        this.uri = uri;
    }

    private void updateProgress(double step, String entry){
        progress += step;
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, Math.min(100, (int) progress), activity.getString(R.string.migration_progress_copying, entry));
    }

    private Uri getFilesUri(Uri uri){
        // Extract files subdirectory not to confuse copyFileTree
        // Actually it shouldn't confuse anymore, but we copy files directly into "files" subdir already
        String[] projection = {DocumentsContract.Document.COLUMN_DOCUMENT_ID};
        String[] to = {"files"};
        Cursor cursor = activity.getContentResolver().query(uri, projection, null, to, null);
        if (cursor == null) throw new IllegalArgumentException();
        cursor.moveToFirst();
        return DocumentsContract.buildChildDocumentsUriUsingTree(uri, cursor.getString(0));
    }

    private void executeMigrate(){
        File root = new File(Tools.DIR_GAME_HOME);
        StatFs stat = new StatFs(root.getAbsolutePath());
        long space = stat.getAvailableBytes();
        if(MIN_FREE_SPACE > space){
            Tools.dialogOnUiThread(activity, activity.getString(R.string.migration_progress_warning_title),
                    activity.getString(R.string.migration_progress_space, MIN_FREE_SPACE - space));
            return;
        }
        Log.i("DataMigration", "Begin data migration!");
        ProgressLayout.setProgress(ProgressLayout.DATA_MIGRATION, 0);
        try {
            activity.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            copyFileTree(activity, getFilesUri(DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))), root, 100);
            Tools.runOnUiThread(() -> Toast.makeText(activity, R.string.migration_progress_finish, Toast.LENGTH_LONG).show());
        } catch (Exception e) {
            Log.e("DataMigration", "Failed to import data to the launcher: " + e.getMessage());
            Tools.runOnUiThread(() -> Toast.makeText(activity, R.string.migration_progress_failed, Toast.LENGTH_LONG).show());
            Tools.showErrorRemote(e);
        }
        progress = 0;
        Log.i("DataMigration", "End data migration!");
        ProgressLayout.clearProgress(ProgressLayout.DATA_MIGRATION);
    }

    /**
     * Migrate data from other MojoLauncher installations.
    */
    public void migrateData(){
        String authority = uri.getAuthority();
        if(authority == null) return;
        // Shouldn't allow importing from any non-Mojo app
        if(!authority.contains(activity.getString(R.string.group_id))) {
            Toast.makeText(activity, R.string.migration_progress_foreign, Toast.LENGTH_LONG).show();
            return;
        }
        // also shouldn't allow importing from self
        if(authority.equals(activity.getString(R.string.storageProviderAuthorities))){
            Toast.makeText(activity, R.string.migration_progress_self, Toast.LENGTH_LONG).show();
            return;
        }
        sExecutorService.submit(this::executeMigrate);
    }

    // Copy a file tree into the home directory
    // The progress bar here works easy & dumb: each entry is a portion of initial 100 percents
    // Each file will increment the progress by this portion, each directory will receive the portion
    // to further divide it by files/folders amount in this directory
    // both files in the end will increment the progress bar by the portion this call received
    // Folder1(100%)
    //          -> Folder2(50%), File2(50%)
    //                  -> Folder3(25%), File3(25%)
    //                          -> (File4(12,5%), File5(12,5%)
    // Surprisingly no LLM model told me about this algorithm lol
    private void copyFileTree(Activity activity, Uri source, File dest, double progressPortion) throws IOException {
        ContentResolver cr = activity.getContentResolver();
        try(Cursor cursor = cr.query(source, TREE_PROJECTION, null, null, null)) {
            if (cursor == null) throw new IllegalArgumentException();
            int count = cursor.getCount();
            double step = progressPortion / (double) count;
            cursor.moveToPosition(-1);
            while (cursor.moveToNext()) {
                String file = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                String type = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE));
                String id = cursor.getString(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE));
                Uri child = DocumentsContract.buildChildDocumentsUriUsingTree(source, id);
                if (type.equals(DocumentsContract.Document.MIME_TYPE_DIR)) {
                    File destDir = new File(dest, file);
                    // Prevent instance collisions
                    if(destDir.exists() && dest.getName().equals("instances"))
                        continue;
                    if (!destDir.exists()) destDir.mkdirs();
                    copyFileTree(activity, child, destDir, step);
                }
                // Assuming file
                else {
                    File destFile = new File(dest, file);
                    // Ignore files with the same size
                    // I mean this check may trigger for non-equal files, but this is designed only for clean import anyway
                    if(destFile.length() == size) continue;
                    Tools.write(cr.openInputStream(child), destFile);
                }
                updateProgress(step, file);
            }
        }
    }
}
