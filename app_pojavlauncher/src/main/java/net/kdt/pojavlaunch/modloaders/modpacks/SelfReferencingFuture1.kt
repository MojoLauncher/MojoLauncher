package net.kdt.pojavlaunch.modloaders.modpacks

import android.util.Log
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future

class SelfReferencingFuture(private val mFutureInterface: FutureInterface) {
    private val mFutureLock = Any()
    private var mMyFuture: Future<*>? = null

    fun startOnExecutor(executorService: ExecutorService): Future<*>? {
        val future = executorService.submit(Runnable { this.run() })
        synchronized(mFutureLock) {
            mMyFuture = future
            (mFutureLock as Object).notify()
        }
        return future
    }

    private fun run() {
        try {
            synchronized(mFutureLock) {
                if (mMyFuture == null) (mFutureLock as Object).wait()
            }
            mFutureInterface.run(mMyFuture)
        } catch (e: InterruptedException) {
            Log.i("SelfReferencingFuture", "Interrupted while acquiring own Future")
        }
    }

    interface FutureInterface {
        fun run(myFuture: Future<*>?)
    }
}
