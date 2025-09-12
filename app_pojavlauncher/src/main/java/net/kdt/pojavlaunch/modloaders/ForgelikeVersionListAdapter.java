package net.kdt.pojavlaunch.modloaders;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ForgelikeVersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
    private final List<String> mGameVersions;
    private final List<List<String>> mLoaderVersions;
    private final LayoutInflater mLayoutInflater;

    public ForgelikeVersionListAdapter(List<String> forgeVersions, LayoutInflater layoutInflater, ForgelikeUtils utils) {
        this.mLayoutInflater = layoutInflater;
        mGameVersions = new ArrayList<>();
        mLoaderVersions = new ArrayList<>();
        for(String version : forgeVersions) {
            if(utils.shouldSkipVersion(version)) continue;
            String gameVersion = utils.processVersionString(version);
            List<String> versionList;
            int gameVersionIndex = mGameVersions.indexOf(gameVersion);
            if(gameVersionIndex != -1) {
                versionList = mLoaderVersions.get(gameVersionIndex);
            } else {
                versionList = new ArrayList<>();
                mGameVersions.add(gameVersion);
                mLoaderVersions.add(versionList);
            }
            versionList.add(version);
        }
        if(utils.isVersionOrderInversed()) {
            for (List<String> versionList : mLoaderVersions) {
                Collections.sort(versionList, Collections.reverseOrder());
            }
            Collections.sort(mGameVersions, Collections.reverseOrder());
        }
    }

    @Override
    public int getGroupCount() {
        return mGameVersions.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return mLoaderVersions.get(i).size();
    }

    @Override
    public Object getGroup(int i) {
        return getGameVersion(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return getForgeVersion(i, i1);
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
        ((TextView) convertView).setText(getForgeVersion(i, i1));
        return convertView;
    }

    private String getGameVersion(int i) {
        return mGameVersions.get(i);
    }

    private String getForgeVersion(int i, int i1){
        return mLoaderVersions.get(i).get(i1);
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }
}
