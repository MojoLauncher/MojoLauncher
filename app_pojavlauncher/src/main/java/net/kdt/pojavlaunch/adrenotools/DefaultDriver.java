package net.kdt.pojavlaunch.adrenotools;

public final class DefaultDriver implements BaseDriver {
    @Override
    public String getName() {
        return "default";
    }

    @Override
    public String getMainLibrary() {
        return "libvulkan_freedreno.so";
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public String getHash() {
        return null;
    }
}
