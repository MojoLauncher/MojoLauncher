package net.kdt.pojavlaunch

import android.os.Bundle
import net.ashmeet.hyperlauncher.R

class MissingStorageActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_test_no_sdcard)
    }
}
