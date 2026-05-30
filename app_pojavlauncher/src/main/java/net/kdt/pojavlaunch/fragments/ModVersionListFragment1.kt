package net.kdt.pojavlaunch.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListAdapter
import android.widget.ExpandableListView
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.dialog
import net.kdt.pojavlaunch.Tools.runOnUiThread
import net.kdt.pojavlaunch.Tools.showError
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.modloaders.ModloaderDownloadListener
import net.kdt.pojavlaunch.modloaders.ModloaderListenerProxy
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper.hasOngoingTasks
import java.io.File
import java.io.IOException

abstract class ModVersionListFragment<T>(mFragmentTag: String?) :
    Fragment(R.layout.fragment_mod_version_list), Runnable, View.OnClickListener,
    OnChildClickListener, ModloaderDownloadListener {
    private val mExtraTag: String
    private var mExpandableListView: ExpandableListView? = null
    private var mProgressBar: ProgressBar? = null
    private var mInflater: LayoutInflater? = null
    private var mRetryView: View? = null

    init {
        this.mExtraTag = mFragmentTag + "_proxy"
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        this.mInflater = LayoutInflater.from(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (view.findViewById<View?>(R.id.title_textview) as TextView).text = getString(this.titleText)
        mProgressBar = view.findViewById(R.id.mod_dl_list_progress)
        mExpandableListView =
            view.findViewById(R.id.mod_dl_expandable_version_list)
        mExpandableListView!!.setOnChildClickListener(this)
        mRetryView = view.findViewById(R.id.mod_dl_retry_layout)
        view.findViewById<View?>(R.id.forge_installer_retry_button).setOnClickListener(this)
        val taskProxy = this.taskProxy
        if (taskProxy != null) {
            mExpandableListView!!.isEnabled = false
            taskProxy.attachListener(this)
        }
        Thread(this).start()
    }

    override fun onStop() {
        val taskProxy = this.taskProxy
        if (taskProxy != null) taskProxy.detachListener()
        super.onStop()
    }

    override fun run() {
        try {
            val versions = loadVersionList()
            runOnUiThread {
                if (versions != null) {
                    mExpandableListView!!.setAdapter(createAdapter(versions, mInflater))
                } else {
                    mRetryView!!.visibility = View.VISIBLE
                }
                mProgressBar!!.visibility = View.GONE
            }
        } catch (e: IOException) {
            runOnUiThread {
                if (context != null) {
                    showError(requireContext(), e)
                    mRetryView!!.visibility = View.VISIBLE
                    mProgressBar!!.visibility = View.GONE
                }
            }
        }
    }

    override fun onClick(view: View?) {
        mRetryView!!.visibility = View.GONE
        mProgressBar!!.visibility = View.VISIBLE
        Thread(this).start()
    }

    override fun onChildClick(
        expandableListView: ExpandableListView,
        view: View?,
        i: Int,
        i1: Int,
        l: Long
    ): Boolean {
        if (hasOngoingTasks()) {
            Toast.makeText(
                expandableListView.context,
                R.string.tasks_ongoing,
                Toast.LENGTH_LONG
            ).show()
            return true
        }
        val forgeVersion = expandableListView.expandableListAdapter.getChild(i, i1)
        val taskProxy = ModloaderListenerProxy()
        val downloadTask = createDownloadTask(forgeVersion, taskProxy)
        this.taskProxy = taskProxy
        taskProxy.attachListener(this)
        mExpandableListView!!.isEnabled = false
        if (downloadTask != null) {
            Thread(downloadTask).start()
        }
        return true
    }

    override fun onDownloadFinished(downloadedFile: File?) {
        runOnUiThread {
            val context = context
            if (context == null) return@runOnUiThread
            this.taskProxy!!.detachListener()
            this.taskProxy = null
            mExpandableListView!!.isEnabled = true
            // Read the comment in FabricInstallFragment.onDownloadFinished() to see how this works
            parentFragmentManager.popBackStackImmediate()
            onDownloadFinished(context, downloadedFile)
        }
    }

    override fun onDataNotAvailable() {
        runOnUiThread {
            val context = context
            if (context == null) return@runOnUiThread
            this.taskProxy!!.detachListener()
            this.taskProxy = null
            mExpandableListView!!.isEnabled = true
            dialog(
                context,
                context.getString(R.string.global_error),
                context.getString(this.noDataMsg)
            )
        }
    }

    override fun onDownloadError(e: Exception?) {
        runOnUiThread {
            val context = context
            if (context == null) return@runOnUiThread
            this.taskProxy!!.detachListener()
            this.taskProxy = null
            mExpandableListView!!.isEnabled = true
            if (e != null) showError(context, e)
        }
    }

    private var taskProxy: ModloaderListenerProxy?
        get() = ExtraCore.getValue(mExtraTag) as? ModloaderListenerProxy
        private set(proxy) {
            ExtraCore.setValue(mExtraTag, proxy)
        }

    abstract val titleText: Int
    abstract val noDataMsg: Int

    @Throws(IOException::class)
    abstract fun loadVersionList(): T?

    abstract fun createAdapter(
        versionList: T?,
        layoutInflater: LayoutInflater?
    ): ExpandableListAdapter?

    abstract fun createDownloadTask(
        selectedVersion: Any?,
        listenerProxy: ModloaderListenerProxy?
    ): Runnable?

    abstract fun onDownloadFinished(context: Context?, downloadedFile: File?)
}
