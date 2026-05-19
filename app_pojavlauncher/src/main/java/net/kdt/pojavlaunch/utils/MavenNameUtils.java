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

    public static StringBuilder mavenNameToPathBuilder(String libName) {
        String[] libInfos = libName.split(":");
        return new StringBuilder()
                .append(libInfos[0].replaceAll("\\.", "/"))
                .append('/')
                .append(libInfos[1])
                .append('/')
                .append(libInfos[2])
                .append('/')
                .append(libInfos[1]).append('-').append(libInfos[2]);
    }

    public static String mavenNameToAarPath(String libName) {
        return mavenNameToPathBuilder(libName).append(".aar").toString();
    }

    public static String mavenNameToPath(String libName) {
        return mavenNameToPathBuilder(libName).append(".jar").toString();
    }
}
