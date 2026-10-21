package git.artdeell.mojo.modding.bta;

import com.kdt.mcgui.ProgressLayout;

import git.artdeell.mojo.utils.jre.classfile.ClassFormatException;
import git.artdeell.mojo.utils.jre.classfile.ClassVersionReader;
import git.artdeell.mojo.utils.jre.classfile.ConstantFieldReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import git.artdeell.mojo.R;

public class BTAHeuristics {
    private static final String LWJGL_VERSION_CLASS = "org/lwjgl/Version.class";
    private static final String BTA_MAIN_CLASS = "net/minecraft/client/Minecraft.class";
    private static final int TOTAL_HEURISTICS = 2;

    public final String lwjglVersion;
    public final int runtimeMajorVersion;

    private BTAHeuristics(String lwjglVersion, int runtimeMajorVersion) {
        this.lwjglVersion = lwjglVersion;
        this.runtimeMajorVersion = runtimeMajorVersion;
    }

    private static int objectToInt(Object obj) {
        if(obj instanceof Integer) return (int) obj;
        if(obj instanceof Float) return (int) ((float)obj);
        if(obj instanceof Long) return (int) ((long)obj);
        if(obj instanceof Double) return (int) ((double)obj);
        try {
            if (obj instanceof String) return Integer.parseInt((String) obj);
        }catch (NumberFormatException ignored) {}
        return -1;
    }

    private static String findLwjglVersion(InputStream inputStream) throws IOException, ClassFormatException {
        ConstantFieldReader fieldReader = new ConstantFieldReader().read(inputStream);
        int major = objectToInt(fieldReader.getFieldValue("VERSION_MAJOR"));
        int minor = objectToInt(fieldReader.getFieldValue("VERSION_MINOR"));
        int patch = objectToInt(fieldReader.getFieldValue("VERSION_REVISION"));
        String versionString = "";
        if(major == -1 || minor == -1 || patch == -1) return versionString;
        versionString = major+"."+minor+"."+patch;
        return versionString;
    }

    private static void updateDetectionProgress(int count) {
        ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, (int) (((float)count / TOTAL_HEURISTICS) * 100), R.string.bta_configuring);
    }

    public static BTAHeuristics detect(File jarFile) throws IOException, ClassFormatException {
        int detectCount = 0;
        updateDetectionProgress(detectCount);

        try(ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(jarFile))) {
            String lwjglVersion = null;
            int runtimeMajorVersion = 0;

            ZipEntry zipEntry;
            while((zipEntry = zipInputStream.getNextEntry()) != null && detectCount < 2) {
                String name = zipEntry.getName();
                switch (name) {
                    case LWJGL_VERSION_CLASS:
                        lwjglVersion = findLwjglVersion(zipInputStream);
                        updateDetectionProgress(++detectCount);
                        break;
                    case BTA_MAIN_CLASS:
                        int classMajorVersion = ClassVersionReader.readClassMajorVersion(zipInputStream);
                        runtimeMajorVersion = ClassVersionReader.classMajorToVMMajor(classMajorVersion);
                        updateDetectionProgress(++detectCount);
                        break;
                }
            }
            return new BTAHeuristics(lwjglVersion, runtimeMajorVersion);
        }
    }
}
