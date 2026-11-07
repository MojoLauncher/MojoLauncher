package git.artdeell.mojo.dialogs;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import git.artdeell.mojo.R;

public abstract class PackageAdapter<T extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<T> implements EditAdapter {

    protected final PackageSelector dialog;
    public PackageAdapter(PackageSelector dialog){
        this.dialog = dialog;
    }

    private boolean mIsDeleting = false;

    @NonNull
    @Override
    public T onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View recyclableView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package_selector, parent,false);
        return this.abstractHolder(recyclableView);
    }

    public abstract void onBindViewHolder(@NonNull T holder, int position);
    protected abstract T abstractHolder(View recyclableView);

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void setEditing(boolean editing) {
        this.mIsDeleting = editing;
        this.notifyDataSetChanged();
    }

    @Override
    public boolean isEditing() {
        return mIsDeleting;
    }

    public abstract static class AbstractHolder<T> extends RecyclerView.ViewHolder {

        protected final TextView mNameTextView;
        protected final TextView mExtraNameTextView;
        protected final ColorStateList mDefaultColors;
        protected final Button mSetDefaultButton;
        protected final ImageButton mDeleteButton;
        protected final Context mContext;

        public AbstractHolder(@NonNull View itemView) {
            super(itemView);

            mNameTextView = itemView.findViewById(R.id.selector_view_name);
            mExtraNameTextView = itemView.findViewById(R.id.selector_view_ext_name);
            mSetDefaultButton = itemView.findViewById(R.id.selector_view_setdefaultbtn);
            mDeleteButton = itemView.findViewById(R.id.selector_view_removebtn);
            mDefaultColors =  mExtraNameTextView.getTextColors();
            mContext = itemView.getContext();
        }

        public abstract void bind(T entry, int pos);
    }
}
