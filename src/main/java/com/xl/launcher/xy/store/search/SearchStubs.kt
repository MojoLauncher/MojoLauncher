package com.xl.launcher.xy.store.search

object SmartSearchFilter {
    fun filter(items: List<String>, q: String) = items.filter { it.contains(q, true) }
}

object SortEngine {
    fun sortByPriority(items: List<String>) = items.sorted()
}
