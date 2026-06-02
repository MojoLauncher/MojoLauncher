package net.kdt.pojavlaunch.tasks;

import net.kdt.pojavlaunch.Architecture;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class NativesExtractor {
    private static final ArrayList<String> LIBRARY_BLACKLIST = createLibraryBlacklist();
    private final File mDestinationDir;
    private final String mLibraryLocation;

    public NativesExtractor(File mDestinationDir) {
        this.mDestinationDir = mDestinationDir;
        this.mLibraryLocation = "jni/"+getAarArchitectureName()+"/";
    }

    /**
     * Create a library blacklist so that downloaded natives are not able to
     * override built-in libraries.
     * @return the resulting blacklist of library file names
     */
    private static ArrayList<String> createLibraryBlacklist() {
        String[] includedLibraryNames = new File(Tools.NATIVE_LIB_DIR).list();
        ArrayList<String> blacklist = new ArrayList<>(includedLibraryNames.length);
        for(String libraryName : includedLibraryNames) {
            // allow overriding jnidispatch (as the integrated version may be too old)
            if(libraryName.equals("libjnidispatch.so")) continue;
            blacklist.add(libraryName);
        }
        blacklist.trimToSize();
        return blacklist;
    }

    private static String getAarArchitectureName() {
        int architecture = Architecture.getDeviceArchitecture();
        switch (architecture) {
            case Architecture.ARCH_ARM:
                return "armeabi-v7a";
            case Architecture.ARCH_ARM64:
                return "arm64-v8a";
            case Architecture.ARCH_X86:
                return "x86";
            case Architecture.ARCH_X86_64:
                return "x86_64";
        }
        throw new RuntimeException("Unknown CPU architecture: "+architecture);
    }

    public void extractFromAar(File source) throws IOException {
        byte[] buffer = new byte[8192];
        try (FileInputStream fileInputStream = new FileInputStream(source);
             ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            // Wrap the ZIP input stream into a non-closeable stream to
            // avoid it being closed by processEntry()
            NonCloseableInputStream entryCopyStream = new NonCloseableInputStream(zipInputStream);
            ZipEntry entry;
            while((entry = zipInputStream.getNextEntry()) != null) {
                String entryName = entry.getName();
                if(!entryName.startsWith(mLibraryLocation) || entry.isDirectory()) continue;
                // Entry name is actually the full path, so we need to strip the path before extraction
                entryName = FileUtils.getFileName(entryName);
                // getFileName may make the file name null, avoid that case.
                if(entryName == null || LIBRARY_BLACKLIST.contains(entryName)) continue;

                processEntry(entryCopyStream, entry, new File(mDestinationDir, entryName), buffer);
            }
        }
    }

    private static long fileCrc32(File target, byte[] buffer) throws IOException {
        try(FileInputStream fileInputStream = new FileInputStream(target)) {
            CRC32 crc32 = new CRC32();
            int len;
            while((len = fileInputStream.read(buffer)) != -1) {
                crc32.update(buffer, 0, len);
            }
            return crc32.getValue();
        }
    }

    private void processEntry(InputStream sourceStream, ZipEntry zipEntry, File entryDestination, byte[] buffer) throws IOException {
        if(entryDestination.exists()) {
            long expectedSize = zipEntry.getSize();
            long expectedCrc32 = zipEntry.getCrc();
            long realSize = entryDestination.length();
            long realCrc32 = fileCrc32(entryDestination, buffer);
            // File in archive is the same as the local one, don't extract
            if(realSize == expectedSize && realCrc32 == expectedCrc32) return;
        }
        // copyInputStreamToFile copies the stream to a file and then closes it.
        org.apache.commons.io.FileUtils.copyInputStreamToFile(sourceStream, entryDestination);
    }


    private static class NonCloseableInputStream extends FilterInputStream {

        protected NonCloseableInputStream(InputStream in) {
            super(in);
        }

        @Override
        public void close() {
            // Do nothing (the point of this class)
        }
    }
}
