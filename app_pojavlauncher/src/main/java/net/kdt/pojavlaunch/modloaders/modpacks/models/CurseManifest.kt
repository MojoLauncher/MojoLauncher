package net.kdt.pojavlaunch.modloaders.modpacks.models

class CurseManifest {
    var name: String? = null
    var version: String? = null
    var manifestType: String? = null
    var manifestVersion: Int = 0
    var files: Array<CurseFile?>? = null
    var minecraft: CurseMinecraft? = null
    var overrides: String? = null

    class CurseFile {
        var projectID: Long = 0
        var fileID: Long = 0
        var required: Boolean = false
    }

    class CurseMinecraft {
        var version: String? = null
        var modLoaders: Array<CurseModLoader?>? = null
    }

    class CurseModLoader {
        var id: String? = null
        var primary: Boolean = false
    }
}
