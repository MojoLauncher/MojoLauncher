package net.kdt.pojavlaunch.modloaders

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListAdapter
import android.widget.TextView

class ForgelikeVersionListAdapter(
    forgeVersions: MutableList<String?>,
    private val mLayoutInflater: LayoutInflater,
    utils: ForgelikeUtils
) : BaseExpandableListAdapter(), ExpandableListAdapter {
    private val mGameVersions: MutableList<String?> = ArrayList()
    private val mLoaderVersions: MutableList<MutableList<String?>> = ArrayList()

    init {
        for (version in forgeVersions) {
            if (utils.shouldSkipVersion(version)) continue
            val gameVersion = utils.processVersionString(version)
            val versionList: MutableList<String?>
            val gameVersionIndex = mGameVersions.indexOf(gameVersion)
            if (gameVersionIndex != -1) {
                versionList = mLoaderVersions[gameVersionIndex]
            } else {
                versionList = ArrayList()
                mGameVersions.add(gameVersion)
                mLoaderVersions.add(versionList)
            }
            versionList.add(version)
        }
        if (utils.isVersionOrderInversed) {
            for (versionList in mLoaderVersions) {
                versionList.reverse()
            }
            mLoaderVersions.reverse()
            mGameVersions.reverse()
        }
    }

    override fun getGroupCount(): Int {
        return mGameVersions.size
    }

    override fun getChildrenCount(i: Int): Int {
        return mLoaderVersions[i].size
    }

    override fun getGroup(i: Int): Any? {
        return getGameVersion(i)
    }

    override fun getChild(i: Int, i1: Int): Any? {
        return getForgeVersion(i, i1)
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
        val view = convertView ?: mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1,
            viewGroup,
            false
        )

        (view as TextView).text = getGameVersion(i)

        return view
    }

    override fun getChildView(
        i: Int,
        i1: Int,
        b: Boolean,
        convertView: View?,
        viewGroup: ViewGroup?
    ): View {
        val view = convertView ?: mLayoutInflater.inflate(
            android.R.layout.simple_expandable_list_item_1,
            viewGroup,
            false
        )
        (view as TextView).text = getForgeVersion(i, i1)
        return view
    }

    private fun getGameVersion(i: Int): String? {
        return mGameVersions[i]
    }

    private fun getForgeVersion(i: Int, i1: Int): String? {
        return mLoaderVersions[i][i1]
    }

    override fun isChildSelectable(i: Int, i1: Int): Boolean {
        return true
    }
}
