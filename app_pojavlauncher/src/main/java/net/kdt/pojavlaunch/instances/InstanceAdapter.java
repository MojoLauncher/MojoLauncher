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

import fr.spse.extended_view.ExtendedTextView;

/*
 * Adapter for listing launcher profiles in a Spinner
 */
public class InstanceAdapter extends BaseAdapter {
    private Instances mInstances;
    private int mSelectionIndex;
    private final InstanceAdapterExtra[] mExtraEntires;


    public InstanceAdapter(InstanceAdapterExtra[] extraEntries) {
        if(extraEntries == null) extraEntries = new InstanceAdapterExtra[0];
        mExtraEntires = extraEntries;
    }
    /**
     * @return how much entries (both instances and extra adapter entries) are in the adapter right now
     */
    @Override
    public int getCount() {
        if(mInstances == null) return 0;
        return mInstances.list.size() + mExtraEntires.length;
    }
    /**
     * Gets the adapter entry at a given index
     * @param position index to retrieve
     * @return Instance, ProfileAdapterExtra or null
     */
    @Override
    public Object getItem(int position) {
        if(mInstances == null) throw new RuntimeException("Attempted to get item before adapter is initialized");
        int instanceListSize = mInstances.list.size();
        int extraPosition = position - instanceListSize;
        if(position < instanceListSize) {
            return mInstances.list.get(position);
        }else if(extraPosition >= 0 && extraPosition < mExtraEntires.length){
            return mExtraEntires[extraPosition];
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_version_profile_layout,parent,false);
        setView(v, position, true);
        return v;
    }

    public void setViewInstance(View v, DisplayInstance i, int idx, boolean displaySelection) {
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
        if(idx == mSelectionIndex && displaySelection) {
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

    public void setView(View v, int index, boolean displaySelection) {
        Object object = getItem(index);
        if(object instanceof DisplayInstance) {
            setViewInstance(v, (DisplayInstance) object, index, displaySelection);
        }else if(object instanceof InstanceAdapterExtra) {
            setViewExtra(v, (InstanceAdapterExtra) object);
        }
    }

    public void applySelectionIndex(int index) {
        mSelectionIndex = index;
    }

    public void applyInstances(Instances instances) {
        mInstances = instances;
        mSelectionIndex = instances.selectedIndex;
        notifyDataSetChanged();
    }
}
