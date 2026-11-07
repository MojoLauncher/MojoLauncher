package git.artdeell.mojo.vulkan.driver;

import java.io.File;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.utils.GpuUtils;

public final class BuiltinDriver implements Driver {
    public static final String DRIVER_LIB = "libvulkan_freedreno.so";
    @Override
    public String getName() {
        return "Built-in";
    }

    @Override
    public String getMainLibrary() {
        return deviceHasDriver() ? DRIVER_LIB : null;
    }

    @Override
    public String getHash() {
        return null;
    }

    public static boolean deviceHasDriver(){
        // In the future we may include more drivers so this should be extended then
        return GpuUtils.getGlInfo().isAdreno() && new File(Tools.NATIVE_LIB_DIR, DRIVER_LIB).exists();
    }
}
