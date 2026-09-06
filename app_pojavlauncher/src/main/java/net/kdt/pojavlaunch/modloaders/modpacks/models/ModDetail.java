package net.kdt.pojavlaunch.modloaders.modpacks.models;


import androidx.annotation.NonNull;

import java.util.Arrays;

public class ModDetail extends ModItem {
    /* A cheap way to map from the front facing name to the underlying id */
    public ModDownload[] downloads;
    public ModDetail(ModItem item, ModDownload[] downloads) {
        super(item.apiSource, item.isModpack, item.id, item.title, item.description, item.imageUrl);
        this.downloads = downloads;
    }

    @NonNull
    @Override
    public String toString() {
        return "ModDetail{" +
                "versions=" + Arrays.toString(downloads) +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}';
    }
}
