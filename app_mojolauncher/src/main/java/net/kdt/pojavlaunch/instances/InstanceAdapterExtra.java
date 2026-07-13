package net.kdt.pojavlaunch.instances;

import android.graphics.drawable.Drawable;

public class InstanceAdapterExtra {
    public final int id;
    public final int name;
    public final Drawable icon;

    public InstanceAdapterExtra(int id, int name, Drawable icon) {
        this.id = id;
        this.name = name;
        this.icon = icon;
    }
}
