package net.kdt.pojavlaunch.instances;

import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.FileUtils;
import net.kdt.pojavlaunch.utils.JSONUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class InstanceManager {
    private static boolean sIsLoaded;
    private static final File sInstancePath = new File(Tools.DIR_GAME_HOME, "instances");
    public static final File SHARED_DATA_DIRECTORY = new File(Tools.DIR_GAME_HOME, "shared_dir");
    private static Instance sSelectedInstance;
    private static ArrayList<Instance> sInstanceList;

    private static Instance read(File instanceRoot) {
        try {
            Instance instance = JSONUtils.readFromFile(metadataLocation(instanceRoot), Instance.class);
            instance.mInstanceRoot = instanceRoot;
            return instance;
        }catch (IOException e) {
            return null;
        }
    }

    protected static File metadataLocation(File instanceDir) {
        return new File(instanceDir, "mojo_instance.json");
    }

    private static File selectedInstanceLocation() {
        String directoryName = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_INSTANCE, "");
        File instanceRoot = new File(sInstancePath, directoryName);
        if(!metadataLocation(instanceRoot).exists()) return null;
        return instanceRoot;
    }

    private static boolean filterInstanceDirectories(File instanceDir) {
        if(!instanceDir.canRead() || !instanceDir.canWrite()) return false;
        if(!instanceDir.isDirectory()) return false;
        File instanceMetadata = metadataLocation(instanceDir);
        if(!instanceMetadata.isFile()) return false;
        return instanceMetadata.canRead();
    }

    private static void loadInstances() throws IOException {
        FileUtils.ensureDirectory(sInstancePath);
        File[] instanceDirectories = sInstancePath.listFiles(InstanceManager::filterInstanceDirectories);
        if(instanceDirectories == null) throw new IOException("Failed to enumerate instances");

        File selectedInstanceLocation = selectedInstanceLocation();
        sSelectedInstance = null;
        sInstanceList = new ArrayList<>(instanceDirectories.length);
        for(File instanceDir : instanceDirectories) {
            Instance instance = read(instanceDir);

            if(instanceDir.equals(selectedInstanceLocation)) {
                sSelectedInstance = instance;
            }

            if(instance != null) {
                instance.sanitize();
                sInstanceList.add(instance);
            }
        }

        if(sInstanceList.isEmpty()) {
            createFirstTimeInstance();
        }

        if(sSelectedInstance == null) {
            setSelectedInstance(sInstanceList.get(0));
        }
    }

    private static void load(){
        if(sIsLoaded) return;
        try {
            loadInstances();
        }catch (IOException e) {
            throw new RuntimeException(e);
        }
        sIsLoaded = true;
    }

    private static File findNewInstanceRoot(String prefix) {
        File instanceRoot;
        do {
            String proposedDirectoryName = UUID.randomUUID().toString();
            if(prefix != null) {
                proposedDirectoryName = prefix + "-" + proposedDirectoryName;
            }
            instanceRoot = new File(sInstancePath, proposedDirectoryName);
        } while(instanceRoot.exists() && instanceRoot.isDirectory());
        return instanceRoot;
    }

    /**
     * Get an unmodifiable list of instances. To add an instance, call createDefaultInstance()
     * @return the unmodifiable list of instances
     */
    public static List<Instance> getImmutableInstanceList() {
        load();
        return Collections.unmodifiableList(sInstanceList);
    }

    /**
     * @return selected instance that is guaranteed to be a member of the instance list.
     * Note that this method will load the full instance list. If this is undesirable, consider
     * using loadSelectedInstance()
     */
    public static Instance getSelectedListedInstance() {
        load();
        return sSelectedInstance;
    }

    /**
     * Set the currently selected instance and save it in user preferences
     * @param instance new selected instance
     */
    public static void setSelectedInstance(Instance instance) {
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(
                        LauncherPreferences.PREF_KEY_CURRENT_INSTANCE,
                        instance.mInstanceRoot.getName()
                ).apply();
        sSelectedInstance = instance;
    }

    /**
     * Remove the instance. This also removes its data storage folder.
     * @param instance the Instance to remove
     * @throws IOException in case of errors during directory removal
     */
    public static void removeInstance(Instance instance) throws IOException {
        File instanceDirectory = instance.mInstanceRoot;
        if(instanceDirectory == null) return;
        sInstanceList.remove(instance);
        if(instance.isSelected()) {
            setSelectedInstance(sInstanceList.get(0));
        }
        org.apache.commons.io.FileUtils.deleteDirectory(instanceDirectory);
    }

    /**
     * Create a new instance intended for first-time launcher users.
     */
    private static void createFirstTimeInstance() throws IOException {
        internalCreateInstance((instance)-> {
            instance.sharedData = true;
            instance.versionId = "1.12.2";
        }, null);
    }

    /**
     * Create a new instance based on a default template.
     * @return the new instance
     */
    public static Instance createDefaultInstance() throws IOException {
        return createInstance((instance)-> {
            instance.sharedData = true;
            instance.versionId = Instance.VERSION_LATEST_RELEASE;
        }, null);
    }

    /**
     * Create an instance without attempting to load the instance list first. Only use this
     * method during initialization.
     */
    private static Instance internalCreateInstance(InstanceSetter instanceSetter, String namePrefix) throws IOException{
        File root = findNewInstanceRoot(namePrefix);
        FileUtils.ensureDirectory(root);
        Instance instance = new Instance();
        instance.mInstanceRoot = root;
        instanceSetter.setInstanceProperties(instance);
        instance.write();
        sInstanceList.add(instance);
        return instance;
    }

    /**
     * Create a new instance with defaults set by user
     * @param instanceSetter setter function called to set user parameters
     * @param namePrefix a name prefix (for the user to easily distinguish installed instances)
     * @return the created instance
     * @throws IOException if directory creation/instance writing fails
     */
    public static Instance createInstance(InstanceSetter instanceSetter, String namePrefix) throws IOException {
        // Make sure the instance list is loaded before creating a new instance.
        load();
        return internalCreateInstance(instanceSetter, namePrefix);
    }

    /**
     * Load the currently selected instance. Note that this method must not be used along with any code
     * which uses getImmutableInstanceList()
     * @return currently selected instance
     */
    public static Instance loadSelectedInstance() {
        if(sIsLoaded) return sSelectedInstance;
        File selectedInstanceLocation = selectedInstanceLocation();
        return read(selectedInstanceLocation);
    }
}
