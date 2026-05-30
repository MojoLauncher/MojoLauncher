package net.kdt.pojavlaunch.profiles

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.utils.FilteredSubList
import net.kdt.pojavlaunch.utils.FilteredSubList.BasicPredicate
import java.io.File
import java.util.Arrays

class VersionListAdapter(
    versionList: Array<JMinecraftVersionList.Version?>?,
    private val mHideCustomVersions: Boolean,
    ctx: Context
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    private val mLayoutInflater: LayoutInflater = ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    private val mGroups: Array<String>
    private val mInstalledVersions: Array<String>?
    private val mData: Array<MutableList<*>>
    private val mSnapshotListPosition: Int

    init {
        val safeVersionList = versionList ?: emptyArray()

        val releaseList: MutableList<JMinecraftVersionList.Version?> =
            FilteredSubList(
                safeVersionList,
                BasicPredicate { item -> item?.type == "release" })
        val snapshotList: MutableList<JMinecraftVersionList.Version?> =
            FilteredSubList(
                safeVersionList,
                BasicPredicate { item -> item?.type == "snapshot" })
        val betaList: MutableList<JMinecraftVersionList.Version?> =
            FilteredSubList(
                safeVersionList,
                BasicPredicate { item -> item?.type == "old_beta" })
        val alphaList: MutableList<JMinecraftVersionList.Version?> =
            FilteredSubList(
                safeVersionList,
                BasicPredicate { item -> item?.type == "old_alpha" })

        // Query installed versions
        mInstalledVersions = File(Tools.DIR_GAME_NEW + "/versions").list()
        if (mInstalledVersions != null) Arrays.sort(mInstalledVersions)

        if (!areInstalledVersionsAvailable()) {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(releaseList, snapshotList, betaList, alphaList)
            mSnapshotListPosition = 1
        } else {
            mGroups = arrayOf(
                ctx.getString(R.string.mcl_setting_veroption_installed),
                ctx.getString(R.string.mcl_setting_veroption_release),
                ctx.getString(R.string.mcl_setting_veroption_snapshot),
                ctx.getString(R.string.mcl_setting_veroption_oldbeta),
                ctx.getString(R.string.mcl_setting_veroption_oldalpha)
            )
            mData = arrayOf(
                mInstalledVersions!!.toMutableList(),
                releaseList,
                snapshotList,
                betaList,
                alphaList
            )
            mSnapshotListPosition = 2
        }
    }

    override fun getGroupCount(): Int {
        return mGroups.size
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return mData[groupPosition].size
    }

    override fun getGroup(groupPosition: Int): Any {
        return mData[groupPosition]
    }

    override fun getChild(groupPosition: Int, childPosition: Int): String? {
        if (isInstalledVersionSelected(groupPosition)) {
            return mInstalledVersions!![childPosition]
        }
        return (mData[groupPosition][childPosition] as JMinecraftVersionList.Version).id
    }

    override fun getGroupId(groupPosition: Int): Long {
        return groupPosition.toLong()
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return childPosition.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getGroupView(
        groupPosition: Int,
        isExpanded: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        (view as TextView).text = mGroups[groupPosition]
        return view
    }

    override fun getChildView(
        groupPosition: Int,
        childPosition: Int,
        isLastChild: Boolean,
        convertView: View?,
        parent: ViewGroup?
    ): View {
        val view = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_expandable_list_item_1, parent, false)
        (view as TextView).text = getChild(groupPosition, childPosition)
        return view
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    fun isSnapshotSelected(groupPosition: Int): Boolean {
        return groupPosition == mSnapshotListPosition
    }

    private fun areInstalledVersionsAvailable(): Boolean {
        if (mHideCustomVersions) return false
        return !mInstalledVersions.isNullOrEmpty()
    }

    private fun isInstalledVersionSelected(groupPosition: Int): Boolean {
        return groupPosition == 0 && areInstalledVersionsAvailable()
    }
}
