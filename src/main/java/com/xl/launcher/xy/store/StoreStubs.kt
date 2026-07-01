package com.xl.launcher.xy.store

import android.util.Log

object XYStore {
    fun start() { Log.i("XYStore","Store started (mock)") }
}

object StoreRepository {
    fun fetchTrending(): List<String> = listOf("Mod-A", "Mod-B")
}

data class TrendingMod(val id: String, val name: String)
data class RecommendedMod(val id: String, val reason: String)

object DiscoveryEngine {
    fun rank(items: List<String>): List<String> = items.reversed()
}
