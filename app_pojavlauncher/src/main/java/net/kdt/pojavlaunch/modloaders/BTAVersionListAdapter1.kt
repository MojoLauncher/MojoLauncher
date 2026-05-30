package net.kdt.pojavlaunch.modloaders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.modloaders.BTAUtils.BTAVersion
import net.kdt.pojavlaunch.modloaders.BTAUtils.BTAVersionList

class BTAVersionListAdapter(
    versionList: BTAVersionList,
    private val mLayoutInflater: LayoutInflater
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    private val mGroupNames: ArrayList<String?>
    private val mGroups: ArrayList<MutableList<BTAVersion?>?>

    init {
        val context = mLayoutInflater.getContext()
        mGroupNames = ArrayList<String?>(2)
        mGroups = ArrayList<MutableList<BTAVersion?>?>(2)
        if (versionList.testedVersions?.isNotEmpty() == true) {
            mGroupNames.add(context.getString(R.string.bta_installer_available_versions))
            mGroups.add(versionList.testedVersions)
        }
        if (versionList.untestedVersions?.isNotEmpty() == true) {
            mGroupNames.add(context.getString(R.string.bta_installer_untested_versions))
            mGroups.add(versionList.untestedVersions)
        }
        if (versionList.nightlyVersions?.isNotEmpty() == true) {
            mGroupNames.add(context.getString(R.string.bta_installer_nightly_versions))
            mGroups.add(versionList.nightlyVersions)
        }
        mGroupNames.trimToSize()
        mGroups.trimToSize()
    }

    override fun getGroupCount(): Int {
        return mGroups.size
    }

    override fun getChildrenCount(i: Int): Int {
        return mGroups.get(i)?.size ?: 0
    }

    override fun getGroup(i: Int): Any? {
        return mGroupNames.get(i)
    }

    override fun getChild(i: Int, i1: Int): BTAVersion? {
        return mGroups.get(i)?.get(i1)
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
        if (view == null) view = mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1,
            viewGroup,
            false
        )

        (view as TextView).setText(getGroup(i) as String?)

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
        if (view == null) view = mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1,
            viewGroup,
            false
        )
        (view as TextView).setText(getChild(i, i1)?.versionName)
        return view
    }

    override fun isChildSelectable(i: Int, i1: Int): Boolean {
        return true
    }
}
