package net.kdt.pojavlaunch.instances

import com.google.gson.JsonSyntaxException
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.utils.FileUtils
import net.kdt.pojavlaunch.utils.JSONUtils
import java.io.File
import java.io.FileFilter
import java.io.IOException
import java.util.Collections
import java.util.UUID

class Instances private constructor(
    val list: List<DisplayInstance>,
    @JvmField val selectedIndex: Int
) {
    companion object {
        private val sInstancePath = File(Tools.DIR_GAME_HOME, "instances")
        val SHARED_DATA_DIRECTORY: File = File(Tools.DIR_GAME_HOME, "shared_dir")

        private fun <T : DisplayInstance> read(instanceRoot: File?, tClass: Class<T>): T? {
            if (instanceRoot == null) return null
            try {
                val metadata = metadataLocation(instanceRoot)
                val instance = JSONUtils.readFromFile(metadata, tClass)
                if (instance == null) return null
                instance.mInstanceRoot = instanceRoot
                return instance
            } catch (e: IOException) {
                return null
            } catch (e: JsonSyntaxException) {
                return null
            }
        }

        fun metadataLocation(instanceDir: File?): File {
            return File(instanceDir, "mojo_instance.json")
        }

        private fun selectedInstanceLocation(): File? {
            val directoryName: String = LauncherPreferences.DEFAULT_PREF?.getString(
                LauncherPreferences.PREF_KEY_CURRENT_INSTANCE,
                ""
            ) ?: ""
            if (directoryName.isEmpty()) return null
            val instanceRoot: File = File(sInstancePath, directoryName)
            if (!metadataLocation(instanceRoot).exists()) return null
            return instanceRoot
        }

        private fun filterInstanceDirectories(instanceDir: File): Boolean {
            if (!instanceDir.canRead() || !instanceDir.canWrite()) return false
            if (!instanceDir.isDirectory) return false
            val instanceMetadata: File = metadataLocation(instanceDir)
            if (!instanceMetadata.isFile) return false
            return instanceMetadata.canRead()
        }

        @Throws(IOException::class)
        private fun <T : DisplayInstance> loadInstances(
            tClass: Class<T>,
            selectionDst: IntArray?
        ): MutableList<T> {
            synchronized(sInstancePath) {
                FileUtils.ensureDirectory(sInstancePath)
            }
            val instanceDirectories: Array<File>? =
                sInstancePath.listFiles(FileFilter { instanceDir: File? ->
                    instanceDir != null && filterInstanceDirectories(instanceDir)
                })
            if (instanceDirectories == null) throw IOException("Failed to enumerate instances")
            val selectedInstanceLocation: File? =
                if (selectionDst != null) selectedInstanceLocation() else null
            val instances = ArrayList<T>(instanceDirectories.size)

            for (instanceDir in instanceDirectories) {
                val instance: T? = read(instanceDir, tClass)

                if (instance == null) continue
                instance.sanitize()
                instances.add(instance)

                if (selectionDst != null && instanceDir == selectedInstanceLocation) {
                    selectionDst[0] = instances.size - 1
                }
            }
            instances.trimToSize()
            return instances
        }

        @JvmStatic
        @Throws(IOException::class)
        fun loadDisplay(): Instances {
            val selectionIndex = intArrayOf(-1)
            val instances: MutableList<DisplayInstance> =
                loadInstances(
                    DisplayInstance::class.java,
                    selectionIndex
                )
            if (instances.isEmpty()) {
                createFirstTimeInstance()
                return loadDisplay()
            } else if (selectionIndex[0] == -1) {
                setSelectedInstance(instances[0])
                selectionIndex[0] = 0
            }
            return Instances(
                Collections.unmodifiableList(instances),
                selectionIndex[0]
            )
        }

        @JvmStatic
        @Throws(IOException::class)
        fun loadAllInstances(): MutableList<Instance> {
            return loadInstances(Instance::class.java, null)
        }

        private fun findNewInstanceRoot(prefix: String?): File {
            var instanceRoot: File
            do {
                var proposedDirectoryName = UUID.randomUUID().toString()
                if (prefix != null) {
                    proposedDirectoryName = prefix + "-" + proposedDirectoryName
                }
                instanceRoot = File(sInstancePath, proposedDirectoryName)
            } while (instanceRoot.exists() && instanceRoot.isDirectory)
            return instanceRoot
        }

        /**
         * Set the currently selected instance and save it in user preferences
         * @param instance new selected instance
         */
        @JvmStatic
        fun setSelectedInstance(instance: DisplayInstance) {
            val root = instance.mInstanceRoot
            if (root != null) {
                LauncherPreferences.DEFAULT_PREF?.edit()
                    ?.putString(
                        LauncherPreferences.PREF_KEY_CURRENT_INSTANCE,
                        root.name
                    )?.apply()
            }
        }

        /**
         * Remove the instance. This also removes its data storage folder.
         * @param instance the Instance to remove
         * @throws IOException in case of errors during directory removal
         */
        @JvmStatic
        @Throws(IOException::class)
        fun removeInstance(instance: Instance) {
            val instanceDirectory = instance.mInstanceRoot
            if (instanceDirectory == null) return
            org.apache.commons.io.FileUtils.deleteDirectory(instanceDirectory)
        }

        /**
         * Create a new instance intended for first-time launcher users.
         */
        @Throws(IOException::class)
        private fun createFirstTimeInstance() {
            internalCreateInstance({ instance: Instance? ->
                instance!!.sharedData = true
                instance.versionId = "1.12.2"
            }, null)
        }

        /**
         * Create a new instance based on a default template.
         * @return the new instance
         */
        @JvmStatic
        @Throws(IOException::class)
        fun createDefaultInstance(): Instance {
            return createInstance({ instance: Instance? ->
                instance!!.sharedData = true
                instance.versionId = Instance.VERSION_LATEST_RELEASE
            }, null)
        }

        /**
         * Create an instance without attempting to load the instance list first. Only use this
         * method during initialization.
         */
        @Throws(IOException::class)
        private fun internalCreateInstance(
            instanceSetter: InstanceSetter,
            namePrefix: String?
        ): Instance {
            val root: File = findNewInstanceRoot(namePrefix)
            FileUtils.ensureDirectory(root)
            val instance = Instance()
            instance.mInstanceRoot = root
            instanceSetter.setInstanceProperties(instance)
            instance.write()
            return instance
        }

        /**
         * Create a new instance with defaults set by user
         * @param instanceSetter setter function called to set user parameters
         * @param namePrefix a name prefix (for the user to easily distinguish installed instances)
         * @return the created instance
         * @throws IOException if directory creation/instance writing fails
         */
        @JvmStatic
        @Throws(IOException::class)
        fun createInstance(instanceSetter: InstanceSetter, namePrefix: String?): Instance {
            return internalCreateInstance(instanceSetter, namePrefix)
        }

        /**
         * Load the currently selected instance. Note that this method must not be used along with any code
         * which uses getImmutableInstanceList()
         * @return currently selected instance
         */
        @JvmStatic
        fun loadSelectedInstance(): Instance? {
            val selectedInstanceLocation: File? = selectedInstanceLocation() ?: return null
            val instance: Instance? =
                read(selectedInstanceLocation, Instance::class.java)
            if (instance == null) return null
            instance.sanitize()
            return instance
        }
    }
}
