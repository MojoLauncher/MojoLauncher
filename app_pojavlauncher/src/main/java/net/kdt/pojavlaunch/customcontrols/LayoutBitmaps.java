package net.kdt.pojavlaunch.customcontrols;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class LayoutBitmaps {
    private static final Random mKeyPicker = new Random(System.nanoTime());
    private final Map<String, Bitmap> mBitmaps;

    private LayoutBitmaps() {
        mBitmaps = new HashMap<>();
    }

    private String pickKey() {
        String key;
        do {
            key = Integer.toString(mKeyPicker.nextInt());
        } while (mBitmaps.containsKey(key));
        return key;
    }

    public Bitmap getBitmap(String key) {
        return mBitmaps.get(key);
    }

    public String putBitmap(Bitmap bitmap, String oldKey) {
        String newKey = pickKey();
        mBitmaps.remove(oldKey);
        if(bitmap != null) mBitmaps.put(newKey, bitmap);
        return newKey;
    }

    private static ControlsContainer createEmpty(String controlsJson) {
        return new ControlsContainer(controlsJson, new LayoutBitmaps());
    }

    private static ControlsContainer loadFromZip(ZipInputStream zipIn) throws IOException {
        LayoutBitmaps layoutBitmaps = new LayoutBitmaps();
        String layoutContent = null;
        for(ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
            if(entry.isDirectory()) continue;
            String entryName = entry.getName();
            if(entryName.equals("layout.json")) {
                layoutContent = IOUtils.toString(zipIn, StandardCharsets.UTF_8);
                continue;
            }
            layoutBitmaps.mBitmaps.put(entryName, BitmapFactory.decodeStream(zipIn));
            zipIn.closeEntry();
        }
        if(layoutContent == null) throw new ZipException("Incorrect ZIP file structure");
        return new ControlsContainer(layoutContent, layoutBitmaps);
    }

    private static ControlsContainer load(FileInputStream fileInputStream) throws IOException{
        try(BufferedInputStream bufferedIn = new BufferedInputStream(fileInputStream)) {
            boolean isZip;
            bufferedIn.mark(4096);
            try {
                ZipInputStream zipIn = new ZipInputStream(bufferedIn);
                isZip = zipIn.getNextEntry() != null;
            } catch (ZipException e) {
                isZip = false;
            }
            bufferedIn.reset();
            if(isZip) {
                try(ZipInputStream zipIn = new ZipInputStream(bufferedIn)) {
                    return loadFromZip(zipIn);
                }
            } else {
                return createEmpty(IOUtils.toString(bufferedIn, StandardCharsets.UTF_8));
            }
        }
    }

    private static void storeZip(FileOutputStream fileOutputStream, ControlsContainer controlsContainer) throws IOException {
        LayoutBitmaps bitmaps = controlsContainer.mLayoutZip;
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
            zipOutputStream.putNextEntry(new ZipEntry("layout.json"));
            IOUtils.write(controlsContainer.mControlsJson, zipOutputStream, StandardCharsets.UTF_8);
            zipOutputStream.closeEntry();
            for(Map.Entry<String, Bitmap> bitmapEntry : bitmaps.mBitmaps.entrySet()) {
                Bitmap outBitmap = bitmapEntry.getValue();
                if(outBitmap == null) continue;
                zipOutputStream.putNextEntry(new ZipEntry(bitmapEntry.getKey()));
                outBitmap.compress(Bitmap.CompressFormat.WEBP, 100, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }

    public static void store(FileOutputStream fileOutputStream, ControlsContainer controlsContainer) throws IOException {
        LayoutBitmaps bitmaps = controlsContainer.mLayoutZip;
        String controlsContent = controlsContainer.mControlsJson;
        if(bitmaps.mBitmaps.isEmpty()) {
            IOUtils.write(controlsContent, fileOutputStream, StandardCharsets.UTF_8);
            return;
        }
        storeZip(fileOutputStream, controlsContainer);
    }

    public static @NonNull ControlsContainer load(File jsonLocation) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(jsonLocation)) {
            return load(fileInputStream);
        }
    }

    public static final class ControlsContainer {
        public final String mControlsJson;
        public final LayoutBitmaps mLayoutZip;

        public ControlsContainer(String mControlsJson, LayoutBitmaps mLayoutZip) {
            this.mControlsJson = mControlsJson;
            this.mLayoutZip = mLayoutZip;
        }
    }
}
