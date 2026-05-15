package net.kdt.pojavlaunch.fragments;

import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.ashmeet.hyperlauncher.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.instances.Instance;
import net.kdt.pojavlaunch.instances.Instances;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.IOUtils;


public class DirectoryManagerFragment extends Fragment {
    public static final String TAG = "DirectoryManagerFragment";
    private static final String ARG_ROOT_PATH = "root_path";
    private static final String ARG_TITLE = "title";

    public static Bundle argsForRoot(@NonNull File rootDir, @Nullable String title) {
        Bundle args = new Bundle();
        args.putString(ARG_ROOT_PATH, rootDir.getAbsolutePath());
        args.putString(ARG_TITLE, title);
        return args;
    }

    private File mRootDir;
    private File mCurrentDir;
    private TextView mTitle;
    private TextView mPath;
    private TextView mStatus;
    private EntryAdapter mAdapter;
    private RecyclerView mList;

    private final ActivityResultLauncher<String> mPickUpload =
            registerForActivityResult(new ActivityResultContracts.GetContent(), this::onUploadPicked);

    public DirectoryManagerFragment() {
        super(R.layout.directory_manager_layout);
    }



    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mTitle = view.findViewById(R.id.dirman_title);
        mPath = view.findViewById(R.id.dirman_path);
        mStatus = view.findViewById(R.id.dirman_status);

        mList = view.findViewById(R.id.dirman_list);

        if (mList != null) {
            mList.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
            mAdapter = new EntryAdapter();
            mList.setAdapter(mAdapter);
            mList.addItemDecoration(new RecyclerView.ItemDecoration() {
                @Override
                public void getItemOffsets(@NonNull android.graphics.Rect outRect,
                                           @NonNull View view,
                                           @NonNull RecyclerView parent,
                                           @NonNull RecyclerView.State state) {

                    outRect.bottom = (int) Tools.dpToPx(10);

                    if (parent.getChildAdapterPosition(view) == 0) {
                        outRect.top = (int) Tools.dpToPx(4);
                    }
                }
            });
        }

        View upBtn = view.findViewById(R.id.dirman_up);
        if (upBtn != null) upBtn.setOnClickListener(v -> goUp());
        
        View newFolderBtn = view.findViewById(R.id.dirman_new_folder);
        if (newFolderBtn != null) newFolderBtn.setOnClickListener(v -> promptNewFolder());
        
        View deleteBtn = view.findViewById(R.id.dirman_delete);
        if (deleteBtn != null) deleteBtn.setOnClickListener(v -> deleteSelected());
        
        View uploadBtn = view.findViewById(R.id.dirman_upload);
        if (uploadBtn != null) uploadBtn.setOnClickListener(v -> mPickUpload.launch("*/*"));

        View renameBtn = view.findViewById(R.id.dirman_rename);
        if (renameBtn != null) renameBtn.setOnClickListener(v -> promptRename());

        View scrollTop = view.findViewById(R.id.dirman_scroll_top);
        if (scrollTop != null) scrollTop.setOnClickListener(v -> scrollToTop());
        
        View scrollBottom = view.findViewById(R.id.dirman_scroll_bottom);
        if (scrollBottom != null) scrollBottom.setOnClickListener(v -> scrollToBottom());
        
        initRoot();
        refresh();
    }

    private void scrollToTop() {
        if (mList == null) return;
        mList.smoothScrollToPosition(0);
    }

    private void scrollToBottom() {
        if (mList == null || mAdapter == null) return;
        int count = mAdapter.getItemCount();
        if (count <= 0) return;
        mList.smoothScrollToPosition(count - 1);
    }

    private void initRoot() {
        Bundle args = getArguments();
        if (args != null) {
            String title = args.getString(ARG_TITLE);
            if (title != null && mTitle != null) mTitle.setText(title);

            String rootPath = args.getString(ARG_ROOT_PATH);
            if (rootPath != null) {
                mRootDir = new File(rootPath);
                mCurrentDir = mRootDir;
                return;
            }
        }

        Instance instance = Instances.loadSelectedInstance();
        if (instance == null) {
            mRootDir = null;
            mCurrentDir = null;
            return;
        }
        mRootDir = instance.getGameDirectory();
        mCurrentDir = mRootDir;
    }

    private void setStatus(@Nullable String text) {
        if (mStatus != null) mStatus.setText(text == null ? "" : text);
    }

    private void refresh() {
        if (mCurrentDir == null) {
            if (mPath != null) mPath.setText("(no instance selected)");
            if (mAdapter != null) mAdapter.setEntries(new ArrayList<>());
            return;
        }

        if (mPath != null) {
            mPath.setMovementMethod(LinkMovementMethod.getInstance());
            mPath.setText(buildBreadcrumb());
        }

        File[] children = mCurrentDir.listFiles();
        List<File> entries = new ArrayList<>();
        if (children != null) entries.addAll(Arrays.asList(children));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            entries.sort(Comparator
                    .comparing((File f) -> !f.isDirectory())
                    .thenComparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        } else {
            Collections.sort(entries, (a, b) -> {
                if (a.isDirectory() && !b.isDirectory()) return -1;
                if (!a.isDirectory() && b.isDirectory()) return 1;
                return a.getName().compareToIgnoreCase(b.getName());
            });
        }

        if (mAdapter != null) mAdapter.setEntries(entries);
    }

    private CharSequence buildBreadcrumb() {
        if (mRootDir == null || mCurrentDir == null) return "-";
        String rootName = mRootDir.getName();
        if (rootName == null || rootName.trim().isEmpty()) rootName = "Root";

        SpannableStringBuilder sb = new SpannableStringBuilder();
        appendCrumb(sb, rootName, mRootDir);

        try {
            String root = mRootDir.getCanonicalPath();
            String current = mCurrentDir.getCanonicalPath();
            if (current.equals(root)) return sb;
            if (!current.startsWith(root)) return mCurrentDir.getAbsolutePath();

            String rel = current.substring(root.length());
            while (rel.startsWith(File.separator)) rel = rel.substring(1);
            if (rel.isEmpty()) return sb;

            File acc = mRootDir;
            String[] parts = rel.split(java.util.regex.Pattern.quote(File.separator));
            for (String part : parts) {
                if (part == null || part.isEmpty()) continue;
                acc = new File(acc, part);
                sb.append(" / ");
                appendCrumb(sb, part, acc);
            }
            return sb;
        } catch (Exception e) {
            return mCurrentDir.getAbsolutePath();
        }
    }

    private void appendCrumb(SpannableStringBuilder sb, String label, File target) {
        int start = sb.length();
        sb.append(label);
        sb.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                openDir(target);
            }
        }, start, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void goUp() {
        if (mCurrentDir == null || mRootDir == null) return;
        File parent = mCurrentDir.getParentFile();
        if (parent == null) return;
        if (!isWithinRoot(parent)) return;
        mCurrentDir = parent;
        setStatus(null);
        refresh();
    }

    private void openDir(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        if (!isWithinRoot(dir)) return;
        mCurrentDir = dir;
        setStatus(null);
        refresh();
    }

    private boolean isWithinRoot(@NonNull File dir) {
        try {
            String root = mRootDir.getCanonicalPath();
            String target = dir.getCanonicalPath();
            if (!target.startsWith(root)) return false;
            // Canonical path of root "C:\root" should not allow "C:\root2"
            if (target.length() > root.length()) {
                char sep = target.charAt(root.length());
                if (sep != File.separatorChar) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void promptNewFolder() {
        if (mCurrentDir == null) return;
        EditText input = new EditText(requireContext());
        input.setHint("Folder name");
        input.setInputType(InputType.TYPE_CLASS_TEXT);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.AppAlertDialogTheme)
                .setTitle("Create folder")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = String.valueOf(input.getText()).trim();
                    if (name.isEmpty()) return;
                    File dir = new File(mCurrentDir, name);
                    if (!isWithinRoot(dir)) return;
                    if (dir.exists()) {
                        setStatus("Already exists");
                        return;
                    }
                    if (dir.mkdirs()) {
                        setStatus("Created: " + name);
                        refresh();
                    } else {
                        setStatus("Failed to create folder");
                    }
                })
                .show();
    }

    private void promptRename() {
        if (mAdapter == null) return;
        File selected = mAdapter.getSelected();
        if (selected == null) {
            setStatus("Select an item first");
            return;
        }

        EditText input = new EditText(requireContext());
        input.setText(selected.getName());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setSelectAllOnFocus(true);

        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.AppAlertDialogTheme)
                .setTitle("Rename")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = String.valueOf(input.getText()).trim();
                    if (name.isEmpty() || name.equals(selected.getName())) return;

                    File target = new File(selected.getParentFile(), name);
                    if (!isWithinRoot(target)) return;
                    if (target.exists()) {
                        setStatus("Name already taken");
                        return;
                    }

                    if (selected.renameTo(target)) {
                        setStatus("Renamed to " + name);
                        refresh();
                    } else {
                        setStatus("Rename failed");
                    }
                })
                .show();
    }

    private void deleteSelected() {
        if (mAdapter == null) return;
        File selected = mAdapter.getSelected();
        if (selected == null) {
            setStatus("Select an item first");
            return;
        }
        if (mRootDir == null || !isWithinRoot(selected)) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.AppAlertDialogTheme)
                .setTitle("Delete?")
                .setMessage(selected.getName())
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    boolean ok = deleteRecursively(selected);
                    setStatus(ok ? "Deleted" : "Delete failed");
                    refresh();
                })
                .show();
    }

    private boolean deleteRecursively(@NonNull File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (!deleteRecursively(child)) return false;
                }
            }
        }
        return file.delete();
    }

    private void onUploadPicked(@Nullable android.net.Uri uri) {
        if (uri == null || mCurrentDir == null) return;
        if (mRootDir == null || !isWithinRoot(mCurrentDir)) return;
        try {
            String name = Tools.getFileName(requireContext(), uri);
            if (name == null || name.trim().isEmpty()) name = "upload";
            name = name.replace("/", "_").replace("\\", "_");

            File target = new File(mCurrentDir, name);
            if (!isWithinRoot(target)) return;
            target = resolveUnique(target);

            FileUtils.ensureParentDirectory(target);
            try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
                 FileOutputStream out = new FileOutputStream(target)) {
                if (in == null) throw new java.io.IOException("Unable to read file");
                IOUtils.copy(in, out);
            }
            setStatus("Uploaded: " + target.getName());
            Toast.makeText(requireContext(), "Uploaded: " + target.getName(), Toast.LENGTH_SHORT).show();
            refresh();
        } catch (Exception e) {
            Tools.showError(requireContext(), e);
        }
    }

    private File resolveUnique(@NonNull File target) {
        if (!target.exists()) return target;
        String name = target.getName();
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        if (dot > 0) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        for (int i = 1; i < 1000; i++) {
            File candidate = new File(target.getParentFile(), base + " (" + i + ")" + ext);
            if (!candidate.exists()) return candidate;
        }
        return target;
    }

    private class EntryAdapter extends RecyclerView.Adapter<EntryViewHolder> {
        private final List<File> mEntries = new ArrayList<>();
        private int mSelectedPos = RecyclerView.NO_POSITION;

        void setEntries(List<File> entries) {
            mEntries.clear();
            mEntries.addAll(entries);
            mSelectedPos = RecyclerView.NO_POSITION;
            notifyDataSetChanged();
        }

        @Nullable File getSelected() {
            if (mSelectedPos < 0 || mSelectedPos >= mEntries.size()) return null;
            return mEntries.get(mSelectedPos);
        }

        @NonNull
        @Override
        public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_directory_entry, parent, false);
            return new EntryViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull EntryViewHolder holder, int position) {
            File file = mEntries.get(position);
            holder.bind(file, position == mSelectedPos);

            holder.itemView.setOnClickListener(v -> {
                if (file.isDirectory()) {
                    openDir(file);
                    return;
                }
                Tools.openPath(requireContext(), file, true);
            });

            holder.itemView.setOnLongClickListener(v -> {
                int pos = holder.getBindingAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return true;
                int prev = mSelectedPos;
                mSelectedPos = (mSelectedPos == pos) ? RecyclerView.NO_POSITION : pos;
                if (prev != RecyclerView.NO_POSITION) notifyItemChanged(prev);
                if (mSelectedPos != RecyclerView.NO_POSITION) notifyItemChanged(mSelectedPos);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return mEntries.size();
        }
    }

    private static class EntryViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mName;
        private final View mSelectedIndicator;

        EntryViewHolder(@NonNull View itemView) {
            super(itemView);
            mIcon = itemView.findViewById(R.id.entry_icon);
            mName = itemView.findViewById(R.id.entry_name);
            mSelectedIndicator = itemView.findViewById(R.id.entry_selected);
        }

        void bind(@NonNull File file, boolean selected) {
            if (mName != null) mName.setText(file.getName());
            if (mSelectedIndicator != null) mSelectedIndicator.setVisibility(selected ? View.VISIBLE : View.GONE);
            if (mIcon != null) {
                mIcon.setImageResource(file.isDirectory() ? R.drawable.ic_px_folder : R.drawable.ic_px_file);
                mIcon.setVisibility(View.VISIBLE);
            }
            itemView.setBackgroundResource(R.drawable.menu_background);
        }
    }
}
