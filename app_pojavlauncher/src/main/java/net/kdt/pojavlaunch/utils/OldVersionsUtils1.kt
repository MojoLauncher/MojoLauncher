package net.kdt.pojavlaunch.utils

import android.util.Log
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import java.text.ParseException

/** Class here to help with various stuff to help run lower versions smoothly  */
object OldVersionsUtils {
    /** Lower minecraft versions fare better with opengl 1
     * @param version The version about to be launched
     */
    fun selectOpenGlVersion(version: JMinecraftVersionList.Version) {
        // 1309989600 is 2011-07-07  2011-07-07T22:00:00+00:00
        val creationTime = version.time
        if (!Tools.isValidString(creationTime)) {
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
            return
        }

        try {
            val creationDate = DateUtils.parseReleaseDate(creationTime)
            if (creationDate == null) {
                Log.e("GL_SELECT", "Failed to parse version date")
                ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
                return
            }
            val openGlVersion = if (DateUtils.dateBefore(creationDate, 2011, 6, 8)) "1" else "2"
            Log.i("GL_SELECT", openGlVersion)
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, openGlVersion)
        } catch (exception: ParseException) {
            Log.e("GL_SELECT", exception.toString())
            ExtraCore.setValue(ExtraConstants.OPEN_GL_VERSION, "2")
        }
    }
}
