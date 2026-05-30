package net.kdt.pojavlaunch.downloader

import java.io.File
import java.net.URL

open class TaskMetadata(var path: File?, var url: URL?) {
    var size: Long = 0
    var sha1Hash: String? = null

    constructor(path: File?, url: URL?, size: Long, hash: String?, mirrorType: Int) : this(
        path,
        url
    ) {
        this.sha1Hash = hash
        this.size = size
    }

    override fun toString(): String {
        return "TaskMetadata{\nurl=" + url + ";\npath=" + path + "\nhash=" + sha1Hash + ";\nsize=" + size + "\n}"
    }
}
