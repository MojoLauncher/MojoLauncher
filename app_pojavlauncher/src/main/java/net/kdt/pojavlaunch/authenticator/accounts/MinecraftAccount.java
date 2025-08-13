package net.kdt.pojavlaunch.authenticator.accounts;


import android.graphics.BitmapFactory;
import android.util.Log;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.*;
import java.net.URL;

import android.graphics.Bitmap;

import androidx.annotation.Keep;

import org.apache.commons.io.IOUtils;

@Keep
public class MinecraftAccount {
    public transient File mSaveLocation;
    public String accessToken = "0"; // access token
    public String profileId = "00000000-0000-0000-0000-000000000000"; // profile UUID, for obtaining skin
    public String username = "Steve";
    public AuthType authType = AuthType.LOCAL;
    public boolean isMicrosoft = false;
    public String refreshToken = "0";
    public String xuid;
    public long expiresAt;
    private transient Bitmap mFaceCache;

    protected MinecraftAccount() {}

    public void updateSkinFace() {
        String skinFaceUrlTemplate = authType.skinUrl;
        if(skinFaceUrlTemplate == null) return;
        String skinFaceUrl = String.format(skinFaceUrlTemplate, username);
        try {
            Log.i("SkinLoader", "Updating skin face...");
            File skinFile = getSkinFaceFile();
            // Streaming it directly breaks on some devices
            byte[] skinBytes = IOUtils.toByteArray(new URL(skinFaceUrl));
            Bitmap skinBitmap = BitmapFactory.decodeByteArray(skinBytes, 0, skinBytes.length);
            if(skinBitmap == null) return;
            Bitmap skinFace = new SkinHeadRenderer().render(100, skinBitmap);
            skinBitmap.recycle();
            if(skinFace == null) return;
            try(FileOutputStream fileOutputStream = new FileOutputStream(skinFile)) {
                skinFace.compress(Bitmap.CompressFormat.WEBP, 90, fileOutputStream);
            }
            Log.i("SkinLoader", "Update skin face success");
        } catch (IOException e) {
            // Skin refresh limit, no internet connection, etc...
            // Simply ignore updating skin face
            Log.w("SkinLoader", "Could not update skin face", e);
        }
    }

    public boolean isLocal(){
        return accessToken.equals("0");
    }
    
    public void save() throws IOException {
        FileUtils.ensureParentDirectory(mSaveLocation);
        JSONUtils.writeToFile(mSaveLocation, this);
    }

    public Bitmap getSkinFace(){
        if(isLocal()) return null;
        File skinFaceFile = getSkinFaceFile();
        if(!skinFaceFile.exists()) return null;
        if(mFaceCache == null) {
            mFaceCache = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath());
        }
        return mFaceCache;
    }

    private File getSkinFaceFile() {
        return new File(Tools.DIR_CACHE,  "skin-face-" + profileId +"-"+authType.name() + ".webp");
    }
}
