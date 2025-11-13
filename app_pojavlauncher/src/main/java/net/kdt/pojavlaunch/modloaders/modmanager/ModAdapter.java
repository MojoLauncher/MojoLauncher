package net.kdt.pojavlaunch.modloaders.modmanager;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.modloaders.modpacks.SelfReferencingFuture;
import net.kdt.pojavlaunch.utils.FilteredSubList;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import git.artdeell.mojo.R;
import me.andreasmelone.basicmodinfoparser.modfile.ModFile;
import me.andreasmelone.basicmodinfoparser.platform.BasicModInfo;
import me.andreasmelone.basicmodinfoparser.platform.Platform;
import me.andreasmelone.basicmodinfoparser.platform.dependency.version.Version;

public class ModAdapter extends RecyclerView.Adapter<ModAdapter.ViewHolder> {
    private final ExecutorService mIconLoadingService = Executors.newFixedThreadPool(2);
    private final List<ModInfo> mModInfos;
    private final FilteredSubList<ModInfo> mDisplayModInfos;
    private final Platform mPlatform;

    public ModAdapter(List<ModInfo> modInfos, Platform mPlatform)  {
        this.mModInfos = modInfos;
        this.mPlatform = mPlatform;
        this.mDisplayModInfos = new FilteredSubList<>(Collections.emptySet(), i->true);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void performSearch(FilteredSubList.BasicPredicate<ModInfo> mModInfoPredicate, View hideView) {
        mDisplayModInfos.clear();
        notifyDataSetChanged();
        PojavApplication.sExecutorService.execute(()-> {
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
        return mDisplayModInfos.get(position).expanded ? 1 : 0;
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

    private class ExpandedViewHolder extends ViewHolder {
        public ExpandedViewHolder(@NonNull View itemView) {
            super(itemView);
            View expansionView = ((ViewStub) itemView.findViewById(R.id.mod_limited_state_stub)).inflate();
        }
    }
}
