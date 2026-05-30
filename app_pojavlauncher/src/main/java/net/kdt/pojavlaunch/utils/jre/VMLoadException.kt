package net.kdt.pojavlaunch.utils.jre

import android.content.Context
import net.ashmeet.hyperlauncher.R

class VMLoadException(errorInfo: String?, private val loadStep: Int, private val errorCode: Int) :
    Exception(errorInfo) {
    fun toString(context: Context): String {
        val loadStepRes: Int = getLoadStepRes(loadStep)
        when (loadStep) {
            0 -> return context.getString(loadStepRes, message)
            1, 4 -> return context.getString(
                loadStepRes,
                context.getString(getErrorCodeRes(errorCode))
            )

            else -> return context.getString(loadStepRes)
        }
    }

    companion object {
        private fun getLoadStepRes(loadStep: Int): Int {
            when (loadStep) {
                0 -> return R.string.vml_fail_load_runtime
                1 -> return R.string.vml_fail_create_runtime
                2 -> return R.string.vml_fail_find_hooks_native
                3 -> return R.string.vml_fail_find_hooks
                4 -> return R.string.vml_fail_insert_hooks
                5 -> return R.string.vml_fail_load_classpath
                6 -> return R.string.vml_fail_run_main
                else -> return R.string.vml_huh
            }
        }

        private fun getErrorCodeRes(errorCode: Int): Int {
            when (errorCode) {
                0 -> return R.string.vml_err_ok
                -2 -> return R.string.vml_err_detached
                -3 -> return R.string.vml_err_version
                -4 -> return R.string.vml_err_nomem
                -5 -> return R.string.vml_err_exists
                -6 -> return R.string.vml_err_inval
                -1 -> return R.string.vml_err_unknown
                else -> return R.string.vml_err_unknown
            }
        }
    }
}
