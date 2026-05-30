/*
 * Copyright (C) 2012 Paul Burke
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ipaulpro.afilechooser

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import net.ashmeet.hyperlauncher.R
import java.io.File

/**
 * List adapter for Files.
 * 
 * @version 2013-12-11
 * @author paulburke (ipaulpro)
 * 
 * @addDate 2018-08-08
 * @addToMyProject khanhduy032
 */
class FileListAdapter(context: Context?) : BaseAdapter() {
    private val mInflater: LayoutInflater

    private var mData: MutableList<File> = ArrayList<File>()

    init {
        mInflater = LayoutInflater.from(context)
    }

    fun add(file: File?) {
        mData.add(file!!)
        notifyDataSetChanged()
    }

    fun remove(file: File?) {
        mData.remove(file)
        notifyDataSetChanged()
    }

    fun insert(file: File?, index: Int) {
        mData.add(index, file!!)
        notifyDataSetChanged()
    }

    fun clear() {
        mData.clear()
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): File {
        return mData.get(position)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return mData.size
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var row = convertView

        if (row == null) row = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false)

        val view = row as TextView

        // Get the file at the current position
        val file = getItem(position)

        // Set the TextView as the file name
        view.setText(file.getName())

        // If the item is not a directory, use the file icon
        val icon: Int = if (file.isDirectory()) ICON_FOLDER else ICON_FILE
        view.setCompoundDrawablesWithIntrinsicBounds(icon, 0, 0, 0)
        view.setCompoundDrawablePadding(20)
        return row
    }

    companion object {
        private val ICON_FOLDER = R.drawable.ic_px_folder
        private val ICON_FILE = R.drawable.ic_px_file
    }
}
