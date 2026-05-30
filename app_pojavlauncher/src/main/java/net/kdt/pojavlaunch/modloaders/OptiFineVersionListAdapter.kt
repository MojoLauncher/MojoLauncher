package net.kdt.pojavlaunch.modloaders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersion
import net.kdt.pojavlaunch.modloaders.OptiFineUtils.OptiFineVersions

class OptiFineVersionListAdapter(
    private val mOptiFineVersions: OptiFineVersions,
    private val mLayoutInflater: LayoutInflater
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    override fun getGroupCount(): Int {
        return mOptiFineVersions.minecraftVersions?.size ?: 0
    }

    override fun getChildrenCount(i: Int): Int {
        return mOptiFineVersions.optifineVersions?.get(i)?.size ?: 0
    }

    override fun getGroup(i: Int): Any? {
        return mOptiFineVersions.minecraftVersions?.get(i)
    }

    override fun getChild(i: Int, i1: Int): Any? {
        return mOptiFineVersions.optifineVersions?.get(i)?.get(i1)
    }

    override fun getGroupId(i: Int): Long {
        return i.toLong()
    }

    override fun getChildId(i: Int, i1: Int): Long {
        return i1.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(i: Int, b: Boolean, convertView: View?, viewGroup: ViewGroup?): View {
        var view = convertView
        if (view == null) view =
            mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)

        (view as TextView).text = getGroup(i) as String?

        return view
    }

    override fun getChildView(
        i: Int,
        i1: Int,
        b: Boolean,
        convertView: View?,
        viewGroup: ViewGroup?
    ): View {
        var view = convertView
        if (view == null) view =
            mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, viewGroup, false)
        (view as TextView).text = (getChild(i, i1) as? OptiFineVersion)?.versionName
        return view
    }

    override fun isChildSelectable(i: Int, i1: Int): Boolean {
        return true
    }
}
