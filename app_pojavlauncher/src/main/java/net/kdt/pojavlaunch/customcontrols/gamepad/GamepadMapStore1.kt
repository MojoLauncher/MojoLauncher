package net.kdt.pojavlaunch.customcontrols.gamepad

import android.util.Log
import com.google.gson.JsonParseException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.Tools.read
import net.kdt.pojavlaunch.Tools.write
import net.kdt.pojavlaunch.utils.FileUtils.ensureParentDirectory
import java.io.File
import java.io.IOException

class GamepadMapStore {
    private var mInMenuMap: GamepadMap? = null
    private var mInGameMap: GamepadMap? = null

    companion object {
        private val STORE_FILE = File(Tools.DIR_DATA, "gamepad_map.json")
        private var sMapStore: GamepadMapStore? = null
        private fun createDefault(): GamepadMapStore {
            val mapStore = GamepadMapStore()
            mapStore.mInGameMap = GamepadMap.defaultGameMap
            mapStore.mInMenuMap = GamepadMap.defaultMenuMap
            return mapStore
        }

        private fun loadIfNecessary() {
            if (sMapStore == null) return
            load()
        }

        fun load() {
            var mapStore: GamepadMapStore? = null
            if (STORE_FILE.exists() && STORE_FILE.canRead()) {
                try {
                    val storeFileContent: String? = read(STORE_FILE)
                    mapStore = Tools.GLOBAL_GSON.fromJson<GamepadMapStore?>(
                        storeFileContent,
                        GamepadMapStore::class.java
                    )
                } catch (e: JsonParseException) {
                    Log.w("GamepadMapStore", "Map store failed to load!", e)
                } catch (e: IOException) {
                    Log.w("GamepadMapStore", "Map store failed to load!", e)
                }
            }
            if (mapStore == null) mapStore = createDefault()
            sMapStore = mapStore
        }

        @Throws(IOException::class)
        fun save() {
            if (sMapStore == null) throw RuntimeException("Must load map store first!")
            ensureParentDirectory(STORE_FILE)
            val jsonData = Tools.GLOBAL_GSON.toJson(sMapStore)
            write(STORE_FILE, jsonData)
        }

        val gameMap: GamepadMap?
            get() {
                loadIfNecessary()
                return sMapStore!!.mInGameMap
            }

        val menuMap: GamepadMap?
            get() {
                loadIfNecessary()
                return sMapStore!!.mInMenuMap
            }
    }
}
