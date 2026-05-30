package net.kdt.pojavlaunch.extra

object ExtraConstants {
    /* ExtraCore constant: a HashMap for converting values such as latest-snapshot or latest-release to actual game version names */
    const val RELEASE_TABLE: String = "release_table"

    /* ExtraCore constant: Serpent's back button tracking thing */
    const val BACK_PREFERENCE: String = "back_preference"

    /* ExtraCore constant: The OPENGL version that should be exposed */
    const val OPEN_GL_VERSION: String = "open_gl_version"

    /* ExtraCore constant: When the microsoft authentication via webview is done */
    const val MICROSOFT_LOGIN_TODO: String = "ms_login_done"

    /* ExtraCore constant: Mojang or "local" authentication to perform */
    const val MOJANG_LOGIN_TODO: String = "mojang_login_todo"

    /* ExtraCore constant: Ely.by authentication to perform */
    const val ELYBY_LOGIN_TODO: String = "elyby_login_done"

    /* ExtraCore constant: Add minecraft account procedure, the user has to select between mojang or microsoft */
    const val SELECT_AUTH_METHOD: String = "start_login_procedure"

    /* ExtraCore constant: Selected file or folder, as a String */
    const val FILE_SELECTOR: String = "file_selector"

    /* ExtraCore constant: Need to refresh the version spinner, selecting the uuid at the same time. Can be DELETED_PROFILE */
    const val REFRESH_VERSION_SPINNER: String = "refresh_version"

    /* ExtraCore Constant: When we want to launch the game */
    const val LAUNCH_GAME: String = "launch_game"

    /* ExtraCore constant: Notify the account spinner that user has returned to the main menu. */
    const val REFRESH_ACCOUNT_SPINNER: String = "refresh_account_spinner"
}
