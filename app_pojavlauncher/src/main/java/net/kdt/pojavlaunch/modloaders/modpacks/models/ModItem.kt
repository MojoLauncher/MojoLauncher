package net.kdt.pojavlaunch.modloaders.modpacks.models

open class ModItem(
    apiSource: Int,
    isModpack: Boolean,
    id: String?,
    title: String?,
    description: String?,
    imageUrl: String?
) : ModSource() {
    var id: String?
    var title: String?
    var description: String?
    var imageUrl: String?

    init {
        this.apiSource = apiSource
        this.isModpack = isModpack
        this.id = id
        this.title = title
        this.description = description
        this.imageUrl = imageUrl
    }

    override fun toString(): String {
        return "ModItem{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", description='" + description + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", apiSource=" + apiSource +
                ", isModpack=" + isModpack +
                '}'
    }

    val iconCacheTag: String
        get() = apiSource.toString() + "_" + id
}
