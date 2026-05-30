package net.kdt.pojavlaunch.authenticator.impl

import android.content.Context

class PresentedException : RuntimeException {
    val localizationStringId: Int
    val extraArgs: Array<out Any?>

    constructor(localizationStringId: Int, vararg extraArgs: Any?) {
        this.localizationStringId = localizationStringId
        this.extraArgs = extraArgs
    }

    constructor(throwable: Throwable?, localizationStringId: Int, vararg extraArgs: Any?) : super(
        throwable
    ) {
        this.localizationStringId = localizationStringId
        this.extraArgs = extraArgs
    }

    fun toString(context: Context): String {
        return context.getString(localizationStringId, *extraArgs)
    }
}
