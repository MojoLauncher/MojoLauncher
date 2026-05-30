package net.kdt.pojavlaunch.instances

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.core.graphics.ColorUtils
import com.kdt.mcgui.mcVersionSpinner
import fr.spse.extended_view.ExtendedTextView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools

/*
 * Adapter for listing launcher profiles in a Spinner
 */
class InstanceAdapter(extraEntries: Array<InstanceAdapterExtra?>?) : BaseAdapter() {
    private var mInstances: Instances? = null
    private var mSelectionIndex = 0
    private val mExtraEntires: Array<InstanceAdapterExtra?>

    init {
        mExtraEntires = extraEntries ?: arrayOfNulls(0)
    }

    /**
     * @return how much entries (both instances and extra adapter entries) are in the adapter right now
     */
    override fun getCount(): Int {
        val instancesCount = mInstances?.list?.size ?: 0
        return instancesCount + mExtraEntires.size
    }

    /**
     * Gets the adapter entry at a given index
     * @param position index to retrieve
     * @return Instance, ProfileAdapterExtra or null
     */
    override fun getItem(position: Int): Any? {
        val instances = mInstances
        val extraEntires = mExtraEntires
        if (instances == null) {
            return if (position >= 0 && position < extraEntires.size) extraEntires[position] else null
        }
        
        val instanceListSize = instances.list.size
        if (position < instanceListSize) {
            return instances.list[position]
        } else {
            val extraPosition = position - instanceListSize
            if (extraPosition >= 0 && extraPosition < extraEntires.size) {
                return extraEntires[extraPosition]
            }
        }
        return null
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View? {
        var v = convertView
        if (v == null) v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_version_profile_layout, parent, false)
        setView(v!!, position, true)
        return v
    }

    fun setViewInstance(v: View, i: DisplayInstance, idx: Int, displaySelection: Boolean) {
        val extendedTextView = v as? ExtendedTextView ?: return

        val cachedIcon = InstanceIconProvider.fetchIcon(v.resources, i)
        extendedTextView.setCompoundDrawablesRelative(
            cachedIcon,
            null,
            extendedTextView.getCompoundsDrawables()[2],
            null
        )

        // Historically, the profile name "New" was hardcoded as the default profile name
        // We consider "New" the same as putting no name at all
        val profileName = Tools.validOrNullString(i.name)
        var versionName = Tools.validOrNullString(i.versionId)

        if (Instance.VERSION_LATEST_RELEASE.equals(
                versionName,
                ignoreCase = true
            )
        ) versionName = v.context.getString(R.string.profiles_latest_release)
        else if (Instance.VERSION_LATEST_SNAPSHOT.equals(
                versionName,
                ignoreCase = true
            )
        ) versionName = v.context.getString(R.string.profiles_latest_snapshot)

        if (versionName == null && profileName != null) extendedTextView.text = profileName
        else if (versionName != null && profileName == null) extendedTextView.text = versionName
        else extendedTextView.text = String.format("%s - %s", profileName, versionName)

        // Set selected background if needed
        if (displaySelection) {
            if (idx == mSelectionIndex) {
                extendedTextView.setBackgroundColor(ColorUtils.setAlphaComponent(Color.WHITE, 60))
            } else {
                extendedTextView.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    fun setViewExtra(v: View?, extra: InstanceAdapterExtra) {
        val extendedTextView = v as? ExtendedTextView ?: return
        extendedTextView.setCompoundDrawablesRelative(
            extra.icon,
            null,
            extendedTextView.getCompoundsDrawables()[2],
            null
        )
        extendedTextView.setText(extra.name)
        if (v !is mcVersionSpinner) {
            extendedTextView.setBackgroundColor(Color.TRANSPARENT)
        }
    }

    fun setView(v: View, index: Int, displaySelection: Boolean) {
        val item = getItem(index)
        if (item is DisplayInstance) {
            setViewInstance(v, item, index, displaySelection)
        } else if (item is InstanceAdapterExtra) {
            setViewExtra(v, item)
        }
    }

    fun applySelectionIndex(index: Int) {
        mSelectionIndex = index
    }

    fun applyInstances(instances: Instances) {
        mInstances = instances
        mSelectionIndex = instances.selectedIndex
        notifyDataSetChanged()
    }
}
