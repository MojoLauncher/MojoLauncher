package net.kdt.pojavlaunch.instances;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.core.graphics.ColorUtils;

import git.artdeell.mojo.R;
import net.kdt.pojavlaunch.Tools;

import java.util.List;

import fr.spse.extended_view.ExtendedTextView;

/*
 * Adapter for listing launcher profiles in a Spinner
 */
public class InstanceAdapter extends BaseAdapter {
    private List<Instance> mInstances;
    private InstanceAdapterExtra[] mExtraEntires;


    public InstanceAdapter(InstanceAdapterExtra[] extraEntries) {
        reloadProfiles(extraEntries);
    }
    /**
     * @return how much entries (both instances and extra adapter entries) are in the adapter right now
     */
    @Override
    public int getCount() {
        return mInstances.size() + mExtraEntires.length;
    }
    /**
     * Gets the adapter entry at a given index
     * @param position index to retrieve
     * @return Instance, ProfileAdapterExtra or null
     */
    @Override
    public Object getItem(int position) {
        int instanceListSize = mInstances.size();
        int extraPosition = position - instanceListSize;
        if(position < instanceListSize) {
            return mInstances.get(position);
        }else if(extraPosition >= 0 && extraPosition < mExtraEntires.length){
            return mExtraEntires[extraPosition];
        }
        return null;
    }


    public int resolveInstanceIndex(Instance instance) {
        return mInstances.indexOf(instance);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public void notifyDataSetChanged() {
        mInstances = InstanceManager.getImmutableInstanceList();
        super.notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_profile_layout,parent,false);
        setView(v, getItem(position), true);
        return v;
    }

    public void setViewInstance(View v, Instance i, boolean displaySelection) {
        ExtendedTextView extendedTextView = (ExtendedTextView) v;

        //MinecraftProfile minecraftProfile = mProfiles.get(nm);
        //if(minecraftProfile == null) minecraftProfile = dummy;
        Drawable cachedIcon = InstanceIconProvider.fetchIcon(v.getResources(), i);
        extendedTextView.setCompoundDrawablesRelative(cachedIcon, null, extendedTextView.getCompoundsDrawables()[2], null);

        // Historically, the profile name "New" was hardcoded as the default profile name
        // We consider "New" the same as putting no name at all

        String profileName = Tools.validOrNullString(i.name);
        String versionName = Tools.validOrNullString(i.versionId);

        if (Instance.VERSION_LATEST_RELEASE.equalsIgnoreCase(versionName))
            versionName = v.getContext().getString(R.string.profiles_latest_release);
        else if (Instance.VERSION_LATEST_SNAPSHOT.equalsIgnoreCase(versionName))
            versionName = v.getContext().getString(R.string.profiles_latest_snapshot);

        if (versionName == null && profileName != null)
            extendedTextView.setText(profileName);
        else if (versionName != null && profileName == null)
            extendedTextView.setText(versionName);
        else extendedTextView.setText(String.format("%s - %s", profileName, versionName));

        // Set selected background if needed
        if(displaySelection && i.isSelected()) {
            extendedTextView.setBackgroundColor(ColorUtils.setAlphaComponent(Color.WHITE, 60));
        }else {
            extendedTextView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    public void setViewExtra(View v, InstanceAdapterExtra extra) {
        ExtendedTextView extendedTextView = (ExtendedTextView) v;
        extendedTextView.setCompoundDrawablesRelative(extra.icon, null, extendedTextView.getCompoundsDrawables()[2], null);
        extendedTextView.setText(extra.name);
        extendedTextView.setBackgroundColor(Color.TRANSPARENT);
    }

    public void setView(View v, Object object, boolean displaySelection) {
        if(object instanceof Instance) {
            setViewInstance(v, (Instance) object, displaySelection);
        }else if(object instanceof InstanceAdapterExtra) {
            setViewExtra(v, (InstanceAdapterExtra) object);
        }
    }

    /** Reload profiles from the file */
    public void reloadProfiles() {
        mInstances = InstanceManager.getImmutableInstanceList();
        notifyDataSetChanged();
    }

    /** Reload profiles from the file, with additional extra entries */
    public void reloadProfiles(InstanceAdapterExtra[] extraEntries) {
        if(extraEntries == null) mExtraEntires = new InstanceAdapterExtra[0];
        else mExtraEntires = extraEntries;
        this.reloadProfiles();
    }
}
