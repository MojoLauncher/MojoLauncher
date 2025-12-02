package net.kdt.pojavlaunch;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import git.artdeell.mojo.R;

public class MissingStorageActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Tools.THEME_MANAGER.applyPrefTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.storage_test_no_sdcard);
    }
}