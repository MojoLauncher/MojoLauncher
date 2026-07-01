package com.xl.launcher.xy.profile

import android.util.Log

object ProfileRepository {
    private val store = mutableMapOf<String, String>()
    fun save(id: String, payload: String) { store[id] = payload }
    fun load(id: String): String? = store[id]
}

object ProfileManager {
    fun createProfile(id: String) { ProfileRepository.save(id, "{}"); Log.i("ProfileManager","Created ${'$'}id") }
}

object ProfileSwitcher {
    @Volatile private var active: String? = null
    fun switchTo(id: String) { active = id }
    fun current(): String? = active
}

object ProfileAnalytics {
    fun recordPlaytime(id: String, minutes: Int) { Log.i("ProfileAnalytics","$id played $minutes minutes") }
}
