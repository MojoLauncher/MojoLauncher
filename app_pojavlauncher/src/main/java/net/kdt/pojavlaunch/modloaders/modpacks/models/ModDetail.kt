package net.kdt.pojavlaunch.modloaders.modpacks.models


class ModDetail(
    item: ModItem,
    versionNames: Array<String?>,
    mcVersionNames: Array<String?>,
    versionUrls: Array<String?>?,
    hashes: Array<String?>?
) : ModItem(item.apiSource, item.isModpack, item.id, item.title, item.description, item.imageUrl) {
    /* A cheap way to map from the front facing name to the underlying id */
    var versionNames: Array<String?>?
    var mcVersionNames: Array<String?>?
    var versionUrls: Array<String?>?

    /* SHA 1 hashes, null if a hash is unavailable */
    var versionHashes: Array<String?>?

    init {
        this.versionNames = versionNames
        this.mcVersionNames = mcVersionNames
        this.versionUrls = versionUrls
        this.versionHashes = hashes

        // Add the mc version to the version model
        for (i in versionNames.indices) {
            if (!versionNames[i]!!.contains(mcVersionNames[i]!!)) versionNames[i] += " - " + mcVersionNames[i]
        }
    }

    override fun toString(): String {
        return "ModDetail{" +
                "versionNames=" + versionNames.contentToString() +
                ", mcVersionNames=" + mcVersionNames.contentToString() +
                ", versionIds=" + versionUrls.contentToString() +
                ", id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}'
    }
}
