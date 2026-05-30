package net.kdt.pojavlaunch

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.ashmeet.hyperlauncher.R

class MissingStorageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.storage_test_no_sdcard)
    }
}
