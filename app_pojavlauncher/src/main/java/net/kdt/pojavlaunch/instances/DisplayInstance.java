package net.kdt.pojavlaunch.instances;

import java.io.File;

public class DisplayInstance {
    public String name;
    public String versionId;
    public String icon;
    protected transient File mInstanceRoot;

    protected DisplayInstance() {
    }

    protected void sanitize() {
        sanitizeIcon();
    }

    protected File getInstanceIconLocation() {
        return new File(mInstanceRoot, "icon.webp");
    }

    private void sanitizeIcon() {
        if (!InstanceIconProvider.hasStaticIcon(icon)) {
            icon = InstanceIconProvider.FALLBACK_ICON_NAME;
        }
    }
}
