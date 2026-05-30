package net.kdt.pojavlaunch.fragments

import net.kdt.pojavlaunch.modloaders.FabriclikeUtils

class FabricInstallFragment : FabriclikeInstallFragment(FabriclikeUtils.FABRIC_UTILS, TAG) {
    companion object {
        const val TAG: String = "FabricInstallFragment"
    }
}
