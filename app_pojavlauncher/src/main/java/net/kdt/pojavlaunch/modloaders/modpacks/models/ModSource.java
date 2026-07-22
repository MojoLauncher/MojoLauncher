package net.kdt.pojavlaunch.modloaders.modpacks.models;

public class ModSource {
    public final int apiSource;
    public final String id;

    protected ModSource(int apiSource, String id) {
        this.apiSource = apiSource;
        this.id = id;
    }

    protected ModSource(ModSource another) {
        this.apiSource = another.apiSource;
        this.id = another.id;
    }

    public String getIconCacheTag() {
        return apiSource+"_"+id;
    }
}
