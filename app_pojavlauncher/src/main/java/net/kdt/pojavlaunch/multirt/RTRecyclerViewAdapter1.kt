package net.kdt.pojavlaunch.multirt

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.edit
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Architecture
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import java.io.IOException

class RTRecyclerViewAdapter : RecyclerView.Adapter<RTRecyclerViewAdapter.RTViewHolder>() {
    private var mIsDeleting = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RTViewHolder {
        val recyclableView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_multirt_runtime, parent, false)
        return RTViewHolder(recyclableView)
    }

    override fun onBindViewHolder(holder: RTViewHolder, position: Int) {
        val runtimes = MultiRTUtils.runtimes
        holder.bindRuntime(runtimes[position], position)
    }

    override fun getItemCount(): Int {
        return MultiRTUtils.runtimes.size
    }

    fun isDefaultRuntime(rt: Runtime): Boolean {
        return LauncherPreferences.PREF_DEFAULT_RUNTIME == rt.name
    }

    @SuppressLint("NotifyDataSetChanged") //not a problem, given the typical size of the list
    fun setDefault(rt: Runtime) {
        LauncherPreferences.PREF_DEFAULT_RUNTIME = rt.name
        LauncherPreferences.DEFAULT_PREF?.edit {
            putString("defaultRuntime", LauncherPreferences.PREF_DEFAULT_RUNTIME)
        }
        notifyDataSetChanged()
    }

    @set:SuppressLint("NotifyDataSetChanged")
    var isEditing: Boolean
        get() = mIsDeleting
        set(isEditing) {
            mIsDeleting = isEditing
            notifyDataSetChanged()
        }


    inner class RTViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version)
        val mFullJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version_full)
        val mDefaultColors: ColorStateList? = mFullJavaVersionTextView.textColors
        val mSetDefaultButton: Button = itemView.findViewById(R.id.multirt_view_setdefaultbtn)
        val mDeleteButton: ImageButton = itemView.findViewById(R.id.multirt_view_removebtn)
        val mContext: Context = itemView.context
        var mCurrentRuntime: Runtime? = null
        var mCurrentPosition: Int = 0

        init {
            setupOnClickListeners()
        }

        @SuppressLint("NotifyDataSetChanged") // same as all the other ones
        private fun setupOnClickListeners() {
            mSetDefaultButton.setOnClickListener {
                mCurrentRuntime?.let {
                    setDefault(it)
                }
            }

            mDeleteButton.setOnClickListener {
                val runtime = mCurrentRuntime ?: return@setOnClickListener
                if (MultiRTUtils.runtimes.size < 2) {
                    MaterialAlertDialogBuilder(mContext)
                        .setTitle(R.string.global_error)
                        .setMessage(R.string.multirt_config_removeerror_last)
                        .setPositiveButton(android.R.string.ok) { adapter, _ -> adapter.dismiss() }
                        .show()
                    return@setOnClickListener
                }
                PojavApplication.sExecutorService.execute {
                    try {
                        MultiRTUtils.removeRuntimeNamed(runtime.name)
                        mDeleteButton.post {
                            bindingAdapter?.notifyDataSetChanged()
                        }
                    } catch (e: IOException) {
                        Tools.showError(itemView.context, e)
                    }
                }
            }
        }

        fun bindRuntime(runtime: Runtime, pos: Int) {
            mCurrentRuntime = runtime
            mCurrentPosition = pos
            if (runtime.versionString != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(
                    runtime.arch ?: ""
                )
            ) {
                mJavaVersionTextView.text = runtime.name
                    .replace(".tar.xz", "")
                    .replace("-", " ")
                mFullJavaVersionTextView.text = runtime.versionString
                mFullJavaVersionTextView.setTextColor(mDefaultColors)

                updateButtonsVisibility()

                val defaultRuntime = isDefaultRuntime(runtime)
                mSetDefaultButton.isEnabled = !defaultRuntime
                mSetDefaultButton.setText(if (defaultRuntime) R.string.multirt_config_setdefault_already else R.string.multirt_config_setdefault)
                return
            }

            // Problematic runtime moment, force propose deletion
            mDeleteButton.visibility = View.VISIBLE
            if (runtime.versionString == null) {
                mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt)
            } else {
                mFullJavaVersionTextView.text = mContext.getString(
                    R.string.multirt_runtime_incompatiblearch,
                    runtime.arch
                )
            }
            mJavaVersionTextView.text = runtime.name
            mFullJavaVersionTextView.setTextColor(Color.RED)
            mSetDefaultButton.visibility = View.GONE
        }

        private fun updateButtonsVisibility() {
            mSetDefaultButton.visibility = if (mIsDeleting) View.GONE else View.VISIBLE
            mDeleteButton.visibility = if (mIsDeleting) View.VISIBLE else View.GONE
        }
    }
}
