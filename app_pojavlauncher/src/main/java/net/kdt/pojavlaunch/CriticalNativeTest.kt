package net.kdt.pojavlaunch

import androidx.annotation.Keep
import dalvik.annotation.optimization.CriticalNative

@Keep
object CriticalNativeTest {
    @JvmStatic
    @CriticalNative
    external fun testCriticalNative(arg0: Int, arg1: Int)

    @JvmStatic
    fun invokeTest() {
        testCriticalNative(0, 0)
    }
}
