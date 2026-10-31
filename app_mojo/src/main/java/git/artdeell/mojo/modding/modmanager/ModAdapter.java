package git.artdeell.mojo.modding.modmanager;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.modding.modpacks.SelfReferencingFuture;
import git.artdeell.mojo.utils.FilteredSubList;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import git.artdeell.mojo.MojoApplication;
import git.artdeell.mojo.R;
import me.andreasmelone.basicmodinfoparser.modfile.DependencyChecker;
import me.andreasmelone.basicmodinfoparser.modfile.ModFile;
import me.andreasmelone.basicmodinfoparser.platform.BasicModInfo;
import me.andreasmelone.basicmodinfoparser.platform.Platform;
import me.andreasmelone.basicmodinfoparser.platform.dependency.version.Version;

public class ModAdapter extends RecyclerView.Adapter<ModAdapter.ViewHolder> {
    private final ExecutorService mIconLoadingService = Executors.newFixedThreadPool(2);
    private final List<ModInfo> mModInfos;
    private final FilteredSubList<ModInfo> mDisplayModInfos;
    private final Platform mPlatform;
    private final DependencyScanner mDependencyScanner;

    public ModAdapter(List<ModInfo> modInfos, Platform mPlatform, DependencyScanner dependencyScanner)  {
        this.mModInfos = modInfos;
        this.mPlatform = mPlatform;
        this.mDependencyScanner = dependencyScanner;
        this.mDisplayModInfos = new FilteredSubList<>(Collections.emptySet(), i->true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void performSearch(FilteredSubList.BasicPredicate<ModInfo> mModInfoPredicate, View hideView) {
        mDisplayModInfos.clear();
        notifyDataSetChanged();
        MojoApplication.sExecutorService.execute(()-> {
            mDisplayModInfos.refresh(mModInfos, mModInfoPredicate);
            Tools.runOnUiThread(()->{
                Log.i("ModAdapter", "Updated display infos, length: "+mDisplayModInfos.size());
                hideView.setVisibility(View.GONE);
                notifyDataSetChanged();
            });
        });
    }

    @Override
    public int getItemViewType(int position) {
        return mDisplayModInfos.get(position) != null && mDisplayModInfos.get(position).expanded ? 1 : 0;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View holderLayout = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_mod_modmanager, parent, false);
        switch (viewType) {
            case 0: return new ViewHolder(holderLayout);
            case 1: return new ExpandedViewHolder(holderLayout);
            default: throw new RuntimeException("Unknown viewType");
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(position, mDisplayModInfos.get(position));
    }

    @Override
    public int getItemCount() {
        return mDisplayModInfos.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private final ConstraintLayout mHeaderContainer;
        private final ImageView mThumbnail;
        private final TextView mName;
        private final TextView mDescription;
        private final TextView mVersion;
        private final TextView mJarFile;
        private final ColorStateList mDefaultNameColor;
        private final ColorStateList mDefaultDescriptionColor;
        private Future<?> mIconLoadingFuture;
        protected ModInfo mModInfo;
        protected int mIndex;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mHeaderContainer = itemView.findViewById(R.id.mod_header_container);
            mThumbnail = itemView.findViewById(R.id.mod_thumbnail_imageview);
            mName = itemView.findViewById(R.id.mod_title_textview);
            mDescription = itemView.findViewById(R.id.mod_body_textview);
            mVersion = itemView.findViewById(R.id.mod_version_textview);
            mJarFile = itemView.findViewById(R.id.mod_name_textview);
            mDefaultNameColor = mName.getTextColors();
            mDefaultDescriptionColor = mDescription.getTextColors();
            itemView.setOnClickListener(this);
        }

        public void bind(int index, ModInfo modInfo) {
            mIndex = index;
            mModInfo = modInfo;
            if(mIconLoadingFuture != null) mIconLoadingFuture.cancel(true);
            mJarFile.setText(modInfo.jarFile.getName());
            if(modInfo instanceof ContainedModInfo) bindContained((ContainedModInfo) modInfo);
            else if(modInfo instanceof CorruptModInfo) bindCorrupt((CorruptModInfo) modInfo);
            mHeaderContainer.setAlpha(modInfo.disabled ? 0.25f:1.0f);
        }

        private void setBitmapThumbnail(Bitmap bitmap) {
            if(bitmap == null) {
                mThumbnail.setVisibility(View.GONE);
                return;
            }else {
                mThumbnail.setVisibility(View.VISIBLE);
            }
            Drawable drawable = mThumbnail.getDrawable();
            Bitmap lastBitmap = null;
            if(drawable instanceof BitmapDrawable) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable)drawable;
                lastBitmap = bitmapDrawable.getBitmap();
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    bitmapDrawable.setBitmap(bitmap);
                }else {
                    mThumbnail.setImageBitmap(bitmap);
                }
            }else {
                mThumbnail.setImageBitmap(bitmap);
            }
            if(lastBitmap != null && !lastBitmap.isRecycled()) lastBitmap.recycle();
        }

        private void applyThumbnail(final ModFile modFile) {
            mIconLoadingFuture = new SelfReferencingFuture(f->{
                Bitmap iconBitmap = null;
                try(InputStream inputStream = modFile.getIconStream()) {
                    if(inputStream != null) iconBitmap = BitmapFactory.decodeStream(inputStream);
                }catch (IOException e) {
                    Log.w("ModAdapter", "Failed to load icon", e);
                }
                Bitmap finalIcon = iconBitmap;
                Tools.runOnUiThread(()->{
                    if(f.isCancelled()) return;
                    setBitmapThumbnail(finalIcon);
                });
            }).startOnExecutor(mIconLoadingService);
        }
        
        private void bindContained(ContainedModInfo containedModInfo) {
            mName.setTextColor(mDefaultNameColor);
            mDescription.setTextColor(mDefaultDescriptionColor);
            BasicModInfo basicModInfo = containedModInfo.modFile.getInfo(mPlatform)[0];
            String name = basicModInfo.getName();
            String description = basicModInfo.getDescription();
            Version<?> version = basicModInfo.getVersion();
            if(name == null) name = containedModInfo.jarFile.getName();
            mName.setText(name);
            if(description == null) mDescription.setText(R.string.mod_manager_mod_unknown_description);
            else mDescription.setText(description);
            if(version == null) mVersion.setText(R.string.mod_manager_mod_unknown_version);
            else mVersion.setText(version.toString());
            applyThumbnail(containedModInfo.modFile);
        }

        private void bindCorrupt(CorruptModInfo corruptModInfo) {
            mName.setTextColor(Color.RED);
            mDescription.setTextColor(Color.RED);
            int corruptionMsgRes;
            switch (corruptModInfo.corruptionReason) {
                case CorruptModInfo.CORRUPTION_REASON_NOT_READABLE:
                    corruptionMsgRes = R.string.mod_manager_mod_corrupt_cant_read;
                    break;
                case CorruptModInfo.CORRUPTION_REASON_NOT_A_MOD:
                    corruptionMsgRes = R.string.mod_manager_mod_corrupt_not_a_mod;
                    break;
                default:
                    throw new RuntimeException("Unknown corruption reason");
            }
            mName.setText(R.string.mod_manager_mod_corrupt);
            mDescription.setText(corruptionMsgRes);
            mVersion.setText(R.string.mod_manager_mod_unknown_version);
            mThumbnail.setVisibility(View.GONE);
        }

        @Override
        public void onClick(View view) {
            mModInfo.expanded = !mModInfo.expanded;
            notifyItemChanged(mIndex);
        }
    }

    private boolean toggleMod(ModInfo modInfo) {
        String name = modInfo.disabled ?
                modInfo.jarFile.getAbsolutePath().replace(".jar.disabled", ".jar") :
                modInfo.jarFile.getAbsolutePath() + ".disabled";
        File newFile = new File(name);
        if(modInfo.jarFile.renameTo(newFile)) {
            modInfo.jarFile = newFile;
            modInfo.disabled = !modInfo.disabled;
            if(modInfo instanceof ContainedModInfo) {
                try {
                    ((ContainedModInfo)modInfo).modFile = ModFile.create(newFile);
                    return true;
                } catch (IOException e) {
                    Log.e("ModAdapter", "Failed to rescan the mod!");
                }
            }
        }
        return false;
    }
    private void toggleModWithDependencies(Context context, ModInfo modInfo, int index) {
        List<ModInfo> deps = !modInfo.disabled ? mDependencyScanner.getEnabledDependencies(modInfo) : null;
        if(deps != null && !deps.isEmpty()) {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.mod_manager_disable_title)
                    .setMessage(context.getString(R.string.mod_manager_disable_message, deps.size()))
                    .setPositiveButton(android.R.string.ok, (d, i) -> {
                        for(ModInfo mod : deps) {
                            toggleMod(mod);
                        }
                        toggleMod(modInfo);
                        notifyDataSetChanged(); })
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
        }
        else {
            if(toggleMod(modInfo)) {
                Toast.makeText(context, modInfo.disabled ? R.string.mod_manager_disable_toast : R.string.mod_manager_enable_toast, Toast.LENGTH_SHORT).show();
                notifyItemChanged(index);
            }
        }
    }

    private class ExpandedViewHolder extends ViewHolder {
        public ExpandedViewHolder(@NonNull View itemView) {
            super(itemView);
            View expansionView = ((ViewStub) itemView.findViewById(R.id.mod_limited_state_stub)).inflate();
            ImageButton delete = expansionView.findViewById(R.id.mod_manager_delete_button);
            ImageButton disable = expansionView.findViewById(R.id.mod_manager_disable_button);
            delete.setOnClickListener(v ->{
                mModInfo.jarFile.delete();
                mDisplayModInfos.remove(mModInfo);
                ModAdapter.this.notifyDataSetChanged();
            });
            disable.setOnClickListener(v -> toggleModWithDependencies(v.getContext(), mModInfo, mIndex));
        }
    }
}
