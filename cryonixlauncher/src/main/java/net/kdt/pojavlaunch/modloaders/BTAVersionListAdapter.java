package net.kdt.pojavlaunch.modloaders;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import git.artdeell.mojo.R;

import java.util.ArrayList;
import java.util.List;

public class BTAVersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter{
    private final LayoutInflater mLayoutInflater;
    private final ArrayList<String> mGroupNames;
    private final ArrayList<List<BTAUtils.BTAVersion>> mGroups;

    public BTAVersionListAdapter(BTAUtils.BTAVersionList versionList, LayoutInflater mLayoutInflater) {
        this.mLayoutInflater = mLayoutInflater;
        Context context = mLayoutInflater.getContext();
        mGroupNames = new ArrayList<>(2);
        mGroups = new ArrayList<>(2);
        if(!versionList.testedVersions.isEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_available_versions));
            mGroups.add(versionList.testedVersions);
        }
        if(!versionList.untestedVersions.isEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_untested_versions));
            mGroups.add(versionList.untestedVersions);
        }
        if(!versionList.nightlyVersions.isEmpty()) {
            mGroupNames.add(context.getString(R.string.bta_installer_nightly_versions));
            mGroups.add(versionList.nightlyVersions);
        }
        mGroupNames.trimToSize();
        mGroups.trimToSize();
    }

    @Override
    public int getGroupCount() {
        return mGroups.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mGroups.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return mGroupNames.get(i);
    }

    @Override
    public BTAUtils.BTAVersion getChild(int i, int i1) {
        return mGroups.get(i).get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View convertView, ViewGroup viewGroup) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false);

        ((TextView) convertView).setText((String)getGroup(i));

        return convertView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView, ViewGroup viewGroup) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false);
        ((TextView) convertView).setText(getChild(i,i1).versionName);
        return convertView;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
