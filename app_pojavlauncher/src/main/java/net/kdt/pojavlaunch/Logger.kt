package net.kdt.pojavlaunch

import androidx.annotation.Keep

/** Singleton class made to log on one file
 * The singleton part can be removed but will require more implementation from the end-dev
 */
@Keep
object Logger {
    /** Print the text to the log file if not censored  */
    external fun appendToLog(text: String?)


    /** Reset the log file, effectively erasing any previous logs  */
    external fun begin(logFilePath: String?)

    /** Link a log listener to the logger  */
    @JvmStatic
    external fun setLogListener(logListener: eventLogListener?)

    /** Small listener for anything listening to the log  */
    @Keep
    fun interface eventLogListener {
        fun onEventLogged(text: String?)
    }
}
