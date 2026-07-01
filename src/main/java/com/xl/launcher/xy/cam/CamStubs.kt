package com.xl.launcher.xy.cam

object CAMManager { fun createProject(name: String) = name }
object ProjectGenerator { fun generate(template: String) = "project-${'$'}{template.hashCode()}" }
object AssetGenerator { fun generateAsset(name: String) = ByteArray(0) }
object TextureGenerator { fun generateTexture(name: String) = ByteArray(0) }
object ExportManager { fun export(id: String) = true }
object ProjectValidator { fun validate(id: String) = true }
