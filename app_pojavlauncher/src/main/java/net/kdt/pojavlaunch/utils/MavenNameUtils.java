package net.kdt.pojavlaunch.utils;

public class MavenNameUtils {

    public static String mavenBaseName(String libName) {
        String[] libInfos = libName.split(":");
        StringBuilder builder = new StringBuilder()
                .append(libInfos[0]).append(':').append(libInfos[1]);
        for(int i = 3; i < libInfos.length; i++) {
            builder.append(':').append(libInfos[i]);
        }
        return builder.toString();
    }

    public static StringBuilder mavenNameToPathBuilder(String libName, String suffix) {
        return mavenNameToPathBuilder(libName).append('-').append(suffix);
    }

    public static StringBuilder mavenNameToPathBuilder(String libName) {
        String[] libInfos = libName.split(":");
        StringBuilder builder = new StringBuilder()
                .append(libInfos[0].replaceAll("\\.", "/"))
                .append('/')
                .append(libInfos[1])
                .append('/')
                .append(libInfos[2])
                .append('/')
                .append(libInfos[1]);
        for(int i = 2; i < libInfos.length; i++) {
            String libInfo = libInfos[i];
            if(libInfo.isEmpty()) continue;
            builder.append('-').append(libInfo);
        }
        return builder;
    }

    public static String mavenNameToAarPath(String libName) {
        return mavenNameToPathBuilder(libName).append(".aar").toString();
    }

    public static String mavenNameToPath(String libName, String suffix) {
        return mavenNameToPathBuilder(libName, suffix).append(".jar").toString();
    }

    public static String mavenNameToPath(String libName) {
        return mavenNameToPathBuilder(libName).append(".jar").toString();
    }
}
