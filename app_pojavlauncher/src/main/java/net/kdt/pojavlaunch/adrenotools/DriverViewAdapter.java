package net.kdt.pojavlaunch.adrenotools;

import static net.kdt.pojavlaunch.PojavApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

import git.artdeell.mojo.R;

import java.util.List;

public class DriverViewAdapter extends RecyclerView.Adapter<DriverViewAdapter.DriverViewHolder> {

    private boolean mIsDeleting = false;

    @NonNull
    @Override
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View recyclableView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_driver_picker,parent,false);
        return new DriverViewHolder(recyclableView);
    }

    @Override
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        final List<BaseDriver> drivers = AdrenoManager.getDrivers();
        holder.bindDriver(drivers.get(position), position);
    }

    @Override
    public int getItemCount() {
        return AdrenoManager.getDriverPaths().size() + 1; // this may!! throw null if not adreno, but we init this view only if adreno anyway
    }

    public boolean isDefault(BaseDriver driver) {
        return AdrenoManager.isPreferredDriver(driver);
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(BaseDriver driver){
        AdrenoManager.setPreferredDriver(driver);
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setIsEditing(boolean isEditing) {
        mIsDeleting = isEditing;
        notifyDataSetChanged();
    }

    public boolean getIsEditing(){
        return mIsDeleting;
    }


    public class DriverViewHolder extends RecyclerView.ViewHolder {
        final TextView mDriverNameTextView;
        final TextView mDriverExtraNameTextView;
        final ColorStateList mDefaultColors;
        final Button mSetDefaultButton;
        final ImageButton mDeleteButton;
        final Context mContext;
        BaseDriver mCurrentDriver;
        int mCurrentPosition;

        public DriverViewHolder(View itemView) {
            super(itemView);
            mDriverNameTextView = itemView.findViewById(R.id.driver_view_name);
            mDriverExtraNameTextView = itemView.findViewById(R.id.driver_view_ext_name);
            mSetDefaultButton = itemView.findViewById(R.id.driver_view_setdefaultbtn);
            mDeleteButton = itemView.findViewById(R.id.driver_view_removebtn);

            mDefaultColors =  mDriverExtraNameTextView.getTextColors();
            mContext = itemView.getContext();

            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                if(mCurrentDriver != null) {
                    setDefault(mCurrentDriver);
                    DriverViewAdapter.this.notifyDataSetChanged();
                }
            });

            mDeleteButton.setOnClickListener(v -> {
                if (mCurrentDriver == null || mCurrentDriver.isDefault()) return;


                sExecutorService.execute(() -> {
                    boolean state = AdrenoManager.removePackage(((AdrenoDriver) mCurrentDriver).toHash());
                    mDeleteButton.post(() -> {
                        if(state) {
                            Log.e(AdrenoManager.TAG, "Unable to remove the package");
                        }
                        if(getBindingAdapter() != null)
                            getBindingAdapter().notifyDataSetChanged();
                    });
                });

            });
        }

        @SuppressLint("SetTextI18n")
        public void bindDriver(BaseDriver driver, int pos) {
            Log.i(AdrenoManager.TAG, "Binding driver " + driver.getName());
            mCurrentDriver = driver;
            mCurrentPosition = pos;
            if(driver.isDefault()){
                mDriverNameTextView.setText(R.string.driver_default_name);
                mDriverExtraNameTextView.setText(R.string.driver_default_extra_name);
            } else {
                mDriverNameTextView.setText(driver.getName());
                AdrenoDriver adr = (AdrenoDriver) driver;
                mDriverExtraNameTextView.setText(adr.getAuthor() + " - " + adr.getDriverVersion());
            }
            boolean isPreferred = isDefault(driver);
            mSetDefaultButton.setEnabled(!isPreferred);
            // TODO: Abstract this
            mSetDefaultButton.setText(isPreferred ? R.string.multirt_config_setdefault_already : R.string.multirt_config_setdefault);
            updateButtonsVisibility();
            if(driver.isDefault()){
                mDeleteButton.setVisibility(View.GONE);
                mDeleteButton.setClickable(false);
            }
            // TODO: Check for driver validness
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(mIsDeleting ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(mIsDeleting ? View.VISIBLE : View.GONE);
        }
    }
}
