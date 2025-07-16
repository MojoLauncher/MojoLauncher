package net.kdt.pojavlaunch.modloaders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class NeoforgeVersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
    private final List<String> mGameVersions;
    private final List<List<String>> mNeoforgeVersions;
    private final LayoutInflater mLayoutInflater;

    public NeoforgeVersionListAdapter(List<String> neoforgeVersions, LayoutInflater layoutInflater) {
        this.mLayoutInflater = layoutInflater;
        mGameVersions = new ArrayList<>();
        mNeoforgeVersions = new ArrayList<>();
        for(String version : neoforgeVersions) {
            String gameVersion = NeoforgeUtils.ComparableVersionString.parse(NeoforgeUtils.getMcVersionForNeoVersion(version)).getProper();
            List<String> versionList;
            int gameVersionIndex = mGameVersions.indexOf(gameVersion);
            if(gameVersionIndex != -1) versionList = mNeoforgeVersions.get(gameVersionIndex);
            else {
                versionList = new ArrayList<>();
                mGameVersions.add(gameVersion);
                mNeoforgeVersions.add(versionList);
            }
            versionList.add(version);
        }
    }

    @Override
    public int getGroupCount() {
        return mGameVersions.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mNeoforgeVersions.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return getGameVersion(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return getNeoForgeVersion(i, i1);
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

        ((TextView) convertView).setText(getGameVersion(i));

        return convertView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView, ViewGroup viewGroup) {
        if(convertView == null)
            convertView = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false);
        ((TextView) convertView).setText(getNeoForgeVersion(i, i1));
        return convertView;
    }

    private String getGameVersion(int i) {
        return mGameVersions.get(i);
    }

    private String getNeoForgeVersion(int i, int i1){
        return mNeoforgeVersions.get(i).get(i1);
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
