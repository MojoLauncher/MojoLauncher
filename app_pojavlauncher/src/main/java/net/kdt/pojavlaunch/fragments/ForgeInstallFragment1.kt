package net.kdt.pojavlaunch.fragments

import android.content.Context
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.modloaders.ForgelikeUtils
import java.io.File

class ForgeInstallFragment : ForgelikeInstallFragment(ForgelikeUtils.FORGE_UTILS, TAG) {
    override val titleText: Int
        get() = R.string.forge_dl_select_version

    override val noDataMsg: Int
        get() = R.string.forge_dl_no_installer

    override fun onDownloadFinished(context: Context?, downloadedFile: File?) {
        // Stub
    }

    companion object {
        const val TAG: String = "ForgeInstallFragment"
    }
}
