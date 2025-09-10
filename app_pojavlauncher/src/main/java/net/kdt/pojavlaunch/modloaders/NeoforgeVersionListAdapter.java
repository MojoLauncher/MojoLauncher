package net.kdt.pojavlaunch.modloaders;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NeoforgeVersionListAdapter extends BaseExpandableListAdapter implements ExpandableListAdapter {
    private final List<String> mGameVersions;
    private final List<List<String>> mNeoforgeVersions;
    private final LayoutInflater mLayoutInflater;

    public NeoforgeVersionListAdapter(List<String> neoforgeVersions, LayoutInflater layoutInflater) {
        this.mLayoutInflater = layoutInflater;
        mGameVersions = new ArrayList<>();
        mNeoforgeVersions = new ArrayList<>();
        for (String version : neoforgeVersions) {
            if (version.startsWith("0")) continue;
            String gameVersion = ComparableVersionString.parse(getMcVersionForNeoVersion(version)).getProper();
            List<String> versionList;
            int gameVersionIndex = mGameVersions.indexOf(gameVersion);
            if (gameVersionIndex != -1) {
                versionList = mNeoforgeVersions.get(gameVersionIndex);
            } else {
                versionList = new ArrayList<>();
                mGameVersions.add(gameVersion);
                mNeoforgeVersions.add(versionList);
            }
            versionList.add(version);
        }
        for (List<String> versionList : mNeoforgeVersions) {
            Collections.sort(versionList, Collections.reverseOrder());
        }
        Collections.sort(mGameVersions, Collections.reverseOrder());
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
        if (convertView == null)
            convertView = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false);

        ((TextView) convertView).setText(getGameVersion(i));

        return convertView;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View convertView, ViewGroup viewGroup) {
        if (convertView == null)
            convertView = mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false);
        ((TextView) convertView).setText(getNeoForgeVersion(i, i1));
        return convertView;
    }

    private String getGameVersion(int i) {
        return mGameVersions.get(i);
    }

    private String getNeoForgeVersion(int i, int i1) {
        return mNeoforgeVersions.get(i).get(i1);
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return true;
    }

    private static String getMcVersionForNeoVersion(String neoVersion) {
        // I feel like it's necessary to explain the NeoForge versioning format
        // basically, what it does is it trims the major version from minecrafts version
        // e.g.: 1.20.1 -> 20.1, and then appends its own "patch" version to that
        // e.g.: 20.1 -> 20.1.8, which means the version string includes both, the minecraft
        // and the loader version at once
        try {
            int firstIndex = neoVersion.indexOf('.');
            int secondIndex = neoVersion.indexOf('.', firstIndex + 1);
            if (firstIndex == -1 || secondIndex == -1) {
                Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion + "; not enough '.' found");
            }
            return "1." + neoVersion.substring(0, secondIndex);
        } catch (StringIndexOutOfBoundsException e) {
            Log.e("NeoforgeUtils", "Failed to parse neoforge version: " + neoVersion, e);
            return neoVersion;
        }
    }
}
