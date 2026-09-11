package net.kdt.pojavlaunch;

public enum InternalRuntime {
    JRE_8(8, "Internal-8", "components/jre-new"),
    JRE_17(17, "Internal-17", "components/jre-new"),
    JRE_21(21, "Internal-21", "components/jre-21"),
    JRE_25(25, "Internal-25", "components/jre-25");

    public final int majorVersion;
    public final String name;
    public final String path;

    InternalRuntime(int majorVersion, String name, String path) {
        this.majorVersion = majorVersion;
        this.name = name;
        this.path = path;
    }
}
