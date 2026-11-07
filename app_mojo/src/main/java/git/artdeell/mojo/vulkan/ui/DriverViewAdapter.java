package git.artdeell.mojo.vulkan.ui;


import static git.artdeell.mojo.MojoApplication.sExecutorService;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import git.artdeell.mojo.dialogs.PackageAdapter;
import git.artdeell.mojo.vulkan.driver.AdrenoDriver;
import git.artdeell.mojo.vulkan.DriverManager;
import git.artdeell.mojo.vulkan.driver.BuiltinDriver;
import git.artdeell.mojo.vulkan.driver.DisplayDriver;
import git.artdeell.mojo.vulkan.driver.Driver;

import git.artdeell.mojo.R;

import java.util.ArrayList;
import java.util.List;

public class DriverViewAdapter extends PackageAdapter<DriverViewAdapter.DriverViewHolder> {
    // On devices with the default driver we always bind it first in the list.
    // Then we need to sync the size of driver list and recycler view list
    private final int driverOffset = BuiltinDriver.deviceHasDriver() ? 1 : 0;
    private final List<DisplayDriver> displayDrivers = new ArrayList<>();

    void refreshList() {
        displayDrivers.clear();
        for(String hash : DriverManager.getDriverPaths()) {
            DisplayDriver dd = DriverManager.loadDisplayDriver(hash);
            if(dd != null) displayDrivers.add(dd);
        }
    }

    public DriverViewAdapter(DriverConfigDialog dialog) {
        super(dialog);
        refreshList();
    }

    @NonNull
    public DriverViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View recyclableView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_package_selector,parent,false);
        return new DriverViewHolder(recyclableView);
    }
    public void onBindViewHolder(@NonNull DriverViewHolder holder, int position) {
        // Always bind the default driver first
        if(position == 0 && driverOffset > 0)
            holder.bind(null, 0);
        else {
            holder.bind(displayDrivers.get(position - driverOffset), position);
        }
    }
    public int getItemCount() { return displayDrivers.size() + driverOffset; }
    protected DriverViewHolder abstractHolder(View recyclableView) {
        return new DriverViewHolder(recyclableView);
    }
    public boolean isDefault(Driver driver) {
        return DriverManager.isPreferredDriver(driver);
    }
    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    public void setDefault(Driver driver){
        DriverManager.setPreferredDriver(driver);
    }

    public class DriverViewHolder extends PackageAdapter.AbstractHolder<DisplayDriver> implements View.OnClickListener {
        DisplayDriver mCurrentDriver;
        int mCurrentPosition;

        public DriverViewHolder(View itemView) {
            super(itemView);
            setupOnClickListeners();
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private void setupOnClickListeners(){
            mSetDefaultButton.setOnClickListener(v -> {
                setDefault(mCurrentDriver);
                DriverViewAdapter.this.dialog.close();
            });

            mDeleteButton.setOnClickListener(this);
        }

        @SuppressLint("SetTextI18n")
        public void bind(DisplayDriver driver, int pos) {
            mCurrentDriver = driver;
            mCurrentPosition = pos;
            updateButtonsVisibility();
            if(mCurrentDriver == null){
                mNameTextView.setText(R.string.driver_default_name);
                mExtraNameTextView.setText(R.string.driver_default_extra_name);
                mDeleteButton.setVisibility(View.GONE);
                mDeleteButton.setClickable(false);
            } else {
                mNameTextView.setText(mCurrentDriver.name);
                mExtraNameTextView.setText(mCurrentDriver.author + " - " + mCurrentDriver.driverVersion);
            }
            boolean isPreferred = isDefault(mCurrentDriver);
            mSetDefaultButton.setEnabled(!isPreferred);
            mSetDefaultButton.setText(isPreferred ? R.string.driver_config_already_used : R.string.driver_config_use);
        }

        private void updateButtonsVisibility(){
            mSetDefaultButton.setVisibility(isEditing() ? View.GONE : View.VISIBLE);
            mDeleteButton.setVisibility(isEditing() ? View.VISIBLE : View.GONE);
        }

        private void afterDelete(boolean deleted) {
            if(!deleted) {
                Toast.makeText(mContext, R.string.driver_config_remove_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            displayDrivers.remove(mCurrentDriver);
            if(getBindingAdapter() != null) getBindingAdapter().notifyDataSetChanged();
        }

        // Delete driver
        @Override
        public void onClick(View v) {
            if (mCurrentDriver == null) return;
            sExecutorService.execute(() -> mDeleteButton.post(() -> afterDelete(DriverManager.removeDriver(mCurrentDriver.getHash()))));
        }
    }
}
