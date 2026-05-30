package com.kdt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

/**
 * Basic adapter, expect it uses the what is passed by the code, no the resources
 * @param <T>
</T> */
class SimpleArrayAdapter<T>(objects: MutableList<T?>?) : BaseAdapter() {
    private var mObjects: MutableList<T?>? = null

    init {
        setObjects(objects)
    }

    fun setObjects(objects: MutableList<T?>?) {
        if (objects == null) {
            if (mObjects !== mutableListOf<Any?>()) {
                mObjects = mutableListOf<T?>()
                notifyDataSetChanged()
            }
        } else {
            if (objects !== mObjects) {
                mObjects = objects
                notifyDataSetChanged()
            }
        }
    }

    override fun getCount(): Int {
        return mObjects!!.size
    }

    override fun getItem(position: Int): T? {
        return mObjects!!.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false)
        }

        val v = convertView as TextView
        v.setText(mObjects!!.get(position).toString())
        return v
    }
}
