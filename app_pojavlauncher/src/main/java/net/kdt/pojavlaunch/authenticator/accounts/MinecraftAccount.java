package net.kdt.pojavlaunch.authenticator.accounts;


import android.graphics.BitmapFactory;
import android.util.Log;

import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.authenticator.AuthType;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.*;

import android.graphics.Bitmap;
import android.util.Base64;

import androidx.annotation.Keep;

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
    public String skinFaceBase64;
    private transient Bitmap mFaceCache;

    protected MinecraftAccount() {}

    void updateSkinFace(String uuid) {
        try {
            File skinFile = getSkinFaceFile(username);
            Tools.downloadFile("https://mc-heads.net/head/" + uuid + "/100", skinFile.getAbsolutePath());
            
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
    
    public void updateSkinFace() {
        updateSkinFace(profileId);
    }
    
    public void save() throws IOException {
        FileUtils.ensureDirectory(mSaveLocation);
        JSONUtils.writeToFile(mSaveLocation, this);
    }

    public Bitmap getSkinFace(){
        if(isLocal()) return null;

        File skinFaceFile = getSkinFaceFile(username);
        if (!skinFaceFile.exists()) {
            // Legacy version, storing the head inside the json as base 64
            if(skinFaceBase64 == null) return null;
            byte[] faceIconBytes = Base64.decode(skinFaceBase64, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(faceIconBytes, 0, faceIconBytes.length);
        } else {
            if(mFaceCache == null) {
                mFaceCache = BitmapFactory.decodeFile(skinFaceFile.getAbsolutePath());
            }
        }

        return mFaceCache;
    }

    private static File getSkinFaceFile(String username) {
        return new File(Tools.DIR_CACHE, username + ".png");
    }
}
