package com.xl.launcher.auth

import android.util.Log

/** Simple account manager to track a GitHub-linked account (mock). */
object AccountManager {
    private var githubLinked: Boolean = false
    private var githubUser: String? = null

    fun isGithubLinked(): Boolean = githubLinked

    fun linkGithub(user: String) {
        githubLinked = true
        githubUser = user
        Log.i("AccountManager", "Linked GitHub user=${'$'}user")
    }

    fun unlinkGithub() {
        githubLinked = false
        githubUser = null
        Log.i("AccountManager", "Unlinked GitHub account")
    }

    fun githubUserName(): String? = githubUser
}
