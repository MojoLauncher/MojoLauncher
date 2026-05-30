package net.kdt.pojavlaunch.multirt

import android.content.Context
import android.database.DataSetObserver
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SpinnerAdapter
import android.widget.TextView
import net.ashmeet.hyperlauncher.R

class RTSpinnerAdapter(context: Context, val mRuntimes: MutableList<Runtime>) : SpinnerAdapter {
    val mContext: Context

    init {
        val runtime = Runtime("<Default>", "", null, 0)
        mRuntimes.add(runtime)
        mContext = context
    }

    override fun registerDataSetObserver(observer: DataSetObserver?) {}

    override fun unregisterDataSetObserver(observer: DataSetObserver?) {}

    override fun getCount(): Int {
        return mRuntimes.size
    }

    override fun getItem(position: Int): Any? {
        return mRuntimes.get(position)
    }

    override fun getItemId(position: Int): Long {
        return mRuntimes.get(position).name.hashCode().toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = if (convertView != null) convertView else LayoutInflater.from(mContext)
            .inflate(R.layout.item_simple_list_1, parent, false)

        val runtime = mRuntimes.get(position)
        if (position == mRuntimes.size - 1) {
            (view as TextView).setText(runtime.name)
        } else {
            (view as TextView).setText(
                String.format(
                    "%s - %s",
                    runtime.name.replace(".tar.xz", ""),
                    if (runtime.versionString == null) view.getResources()
                        .getString(R.string.multirt_runtime_corrupt) else runtime.versionString
                )
            )
        }

        return view
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return mRuntimes.isEmpty()
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getView(position, convertView, parent)
    }
}
