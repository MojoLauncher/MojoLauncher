package net.kdt.pojavlaunch.instances;

import com.google.gson.JsonSyntaxException;

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

public class Instances {
    private static final File sInstancePath = new File(Tools.DIR_GAME_HOME, "instances");
    public static final File SHARED_DATA_DIRECTORY = new File(Tools.DIR_GAME_HOME, "shared_dir");

    public final List<DisplayInstance> list;
    public final int selectedIndex;

    private Instances(List<DisplayInstance> instances, int selectedIndex) {
        this.list = instances;
        this.selectedIndex = selectedIndex;
    }

    private static <T extends DisplayInstance> T read(File instanceRoot, Class<T> tClass) {
        try {
            T instance = JSONUtils.readFromFile(metadataLocation(instanceRoot), tClass);
            instance.mInstanceRoot = instanceRoot;
            return instance;
        }catch (IOException | JsonSyntaxException e) {
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

    private static <T extends DisplayInstance> List<T> loadInstances(Class<T> tClass, int[] selectionDst) throws IOException {
        synchronized (sInstancePath) {
            FileUtils.ensureDirectory(sInstancePath);
        }
        File[] instanceDirectories = sInstancePath.listFiles(Instances::filterInstanceDirectories);
        if(instanceDirectories == null) throw new IOException("Failed to enumerate instances");
        File selectedInstanceLocation = selectionDst != null ? selectedInstanceLocation() : null;
        ArrayList<T> instances = new ArrayList<>(instanceDirectories.length);

        for(File instanceDir : instanceDirectories) {
            T instance = read(instanceDir, tClass);

            if(instance == null) continue;
            instance.sanitize();
            instances.add(instance);

            if(selectionDst != null && instanceDir.equals(selectedInstanceLocation)) {
                selectionDst[0] = instances.size() - 1;
            }
        }
        instances.trimToSize();
        return instances;
    }

    public static Instances loadDisplay() throws IOException {
        int[] selectionIndex = new int[] { -1 };
        List<DisplayInstance> instances = loadInstances(DisplayInstance.class, selectionIndex);
        if(instances.isEmpty()) {
            createFirstTimeInstance();
            return loadDisplay();
        }else if(selectionIndex[0] == -1) {
            setSelectedInstance(instances.get(0));
            selectionIndex[0] = 0;
        }
        return new Instances(Collections.unmodifiableList(instances), selectionIndex[0]);
    }

    public static List<Instance> loadAllInstances() throws IOException {
        return loadInstances(Instance.class, null);
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
     * Set the currently selected instance and save it in user preferences
     * @param instance new selected instance
     */
    public static void setSelectedInstance(DisplayInstance instance) {
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(
                        LauncherPreferences.PREF_KEY_CURRENT_INSTANCE,
                        instance.mInstanceRoot.getName()
                ).apply();
    }

    /**
     * Remove the instance. This also removes its data storage folder.
     * @param instance the Instance to remove
     * @throws IOException in case of errors during directory removal
     */
    public static void removeInstance(Instance instance) throws IOException {
        File instanceDirectory = instance.mInstanceRoot;
        if(instanceDirectory == null) return;
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
        return internalCreateInstance(instanceSetter, namePrefix);
    }

    /**
     * Load the currently selected instance. Note that this method must not be used along with any code
     * which uses getImmutableInstanceList()
     * @return currently selected instance
     */
    public static Instance loadSelectedInstance() {
        File selectedInstanceLocation = selectedInstanceLocation();
        Instance instance = read(selectedInstanceLocation, Instance.class);
        if(instance == null) return null;
        instance.sanitize();
        return instance;
    }
}
