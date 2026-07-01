package com.xl.launcher.xy.store

/** Small extension to expose dependency resolver stub to UI logic. */
object DependencyResolver {
    fun resolve(names: List<String>): Map<String, Boolean> = names.associateWith { true }
}
