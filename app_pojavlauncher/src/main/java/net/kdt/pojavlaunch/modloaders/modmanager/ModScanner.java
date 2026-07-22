package net.kdt.pojavlaunch.modloaders.modmanager;

import android.util.Log;

import net.kdt.pojavlaunch.modloaders.modmanager.dumbzip.DumbZipFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import me.andreasmelone.basicmodinfoparser.modfile.ModFile;
import me.andreasmelone.basicmodinfoparser.platform.BasicModInfo;
import me.andreasmelone.basicmodinfoparser.platform.Platform;

public class ModScanner {
    static {
        DumbZipFactory.use();
    }
    private final Platform mPlatform;
    private final ArrayList<ModInfo> mList;
    private ModInfo[] mModInfos;
    private ModScanner(Platform mPlatform, ArrayList<ModInfo> mList) {
        this.mPlatform = mPlatform;
        this.mList = mList;
    }

    public static List<ModInfo> findMods(File directory, Platform platform) {
        ArrayList<ModInfo> modInfos = new ArrayList<>();
        try {
            new ModScanner(platform, modInfos).findModFiles(directory);
        }catch (InterruptedException e) {
            return null;
        }
        return modInfos;
    }

    private void findModFiles(File directory) throws InterruptedException{
        File[] files = directory.listFiles(((file,name) -> name.endsWith(".jar")));
        File[] directories = directory.listFiles(File::isDirectory);
        if(directories != null) for(File f : directories) findModFiles(f);
        if(files == null) return;
        mModInfos = new ModInfo[files.length];
        try(ExecutorService executorService = Executors.newFixedThreadPool(4)) {
            int index = 0;
            for(File f : files) {
                Log.i("ModScanner", "Schedule: "+index);
                executorService.execute(new ModScannerRunnable(f, index++));
            }
            executorService.shutdown();
            while(!executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS));
        }
        mList.addAll(Arrays.asList(mModInfos));
    }

    private class ModScannerRunnable implements Runnable {
        private final File mModFile;
        private final int mIndex;

        private ModScannerRunnable(File mModFile, int mIndex) {
            this.mModFile = mModFile;
            this.mIndex = mIndex;
        }

        @Override
        public void run() {
            ModInfo modInfo;
            try {
                ModFile modFile = ModFile.create(mModFile);
                BasicModInfo[] infos = modFile.getInfo(mPlatform);
                if(infos.length == 0) {
                    modInfo = new CorruptModInfo(mModFile, CorruptModInfo.CORRUPTION_REASON_NOT_A_MOD);
                }else {
                    BasicModInfo basicModInfo = infos[0];
                    modInfo = new ContainedModInfo(mModFile, modFile, createSearchTerms(basicModInfo));
                }

            }catch (IOException e) {
                Log.i("ModScanner", "Exception while reading mod", e);
                modInfo = new CorruptModInfo(mModFile, CorruptModInfo.CORRUPTION_REASON_NOT_READABLE);
            }
            mModInfos[mIndex] = modInfo;
        }

        private String createSearchTerms(BasicModInfo basicModInfo) {
            StringBuilder termsBuilder = new StringBuilder();
            if(basicModInfo.getName() != null) termsBuilder.append(basicModInfo.getName()).append(' ');
            if(basicModInfo.getDescription() != null) termsBuilder.append(basicModInfo.getDescription());
            return toSearchTermString(termsBuilder.toString());
        }
    }

    private static String toSearchTermString(String string) {
        String[] words = string.toLowerCase().split("\\s+");
        HashSet<String> wordSet = new HashSet<>(words.length);
        int wordSetLength = 0;
        for(String s : words) {
            String nString = s.replaceAll("\\P{L}","");
            if(nString.isEmpty()) continue;
            if(wordSet.add(nString)) wordSetLength += nString.length();
        }
        StringBuilder wordSetBuilder = new StringBuilder(wordSetLength);
        for(String word : wordSet) wordSetBuilder.append(word);
        return wordSetBuilder.toString();
    }
}
