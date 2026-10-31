package git.artdeell.mojo.modding.modmanager;

import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.andreasmelone.basicmodinfoparser.platform.BasicModInfo;
import me.andreasmelone.basicmodinfoparser.platform.Platform;
import me.andreasmelone.basicmodinfoparser.platform.dependency.Dependency;
import me.andreasmelone.basicmodinfoparser.platform.dependency.ProvidedMod;
import me.andreasmelone.basicmodinfoparser.platform.modinfo.ProvidesList;
import me.andreasmelone.basicmodinfoparser.util.ModInfoParseException;

// TODO: implement jar in jar scanning (this will fix fabric-api provides)
public class DependencyScanner {
    private final Platform mPlatform;
    private final Map<String, ModInfo> mProviders = new HashMap<>();
    private final Map<String, List<ModInfo>> mDeps = new HashMap<>();

    public DependencyScanner(Platform platform, List<ModInfo> modInfos) {
        this.mPlatform = platform;
        build(modInfos);
    }

    private BasicModInfo getBasicInfo(ContainedModInfo cmi) {
        try {
            BasicModInfo[] infos = cmi.modFile.getInfo(mPlatform);
            return infos.length > 0 ? infos[0] : null;
        } catch (ModInfoParseException e) {
            Log.e("DependencyScanner", "Unable to parse mod " + cmi.jarFile.getName() + "!");
            return  null;
        }
    }

    private void build(List<ModInfo> modInfos) {
        for(ModInfo mi : modInfos) {
            if(!(mi instanceof ContainedModInfo)) continue;
            BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
            if(bmi == null) continue;
            if(bmi.getId() != null) mProviders.put(bmi.getId(), mi);
            for(Dependency dep : bmi.getDependencies()) {
                List<ModInfo> dependants = mDeps.get(dep.getModId());
                if(dependants == null) {
                    dependants = new ArrayList<>();
                    mDeps.put(dep.getModId(), dependants);
                }
                dependants.add(mi);
            }
        }
    }

    private List<String> namesOf(BasicModInfo bmi) {
        List<String> res = new ArrayList<>();
        if(bmi.getId() != null) res.add(bmi.getId());
        if(bmi instanceof ProvidesList) {
            List<? extends ProvidedMod<?>> provided = ((ProvidesList) bmi).getProvidedIds();
            if(provided != null) {
                for(ProvidedMod<?> providedMod : provided) {
                    if(providedMod != null && providedMod.getId() != null) {
                        Log.i("DependencyScanner", "Adding mod with id " + providedMod.getId());
                        res.add(providedMod.getId());

                    }
                }
            }
        }
        return res;
    }

    private void enqueueIds(BasicModInfo bmi, Deque<ModInfo> deque) {
        for(String name : namesOf(bmi)) {
            List<ModInfo> next = mDeps.get(name);
            if(next != null) deque.addAll(next);
        }
    }

    /**
     * Get all dependant mods of the target that are currently enabled
     * @param mi target mod info
     * @return a list of dependencies
     */
    public List<ModInfo> getEnabledDependencies(ModInfo mi) {
        BasicModInfo bmi = getBasicInfo((ContainedModInfo) mi);
        if(bmi == null || bmi.getId() == null) return Collections.emptyList();
        List<ModInfo> res = new ArrayList<>();
        Set<ModInfo> visited = new HashSet<>();
        Deque<ModInfo> queue = new ArrayDeque<>();
        visited.add(mi);
        enqueueIds(bmi, queue);
        while(!queue.isEmpty()) {
            ModInfo current = queue.poll();
            if(current == null || current == mi || visited.contains(current)) continue;
            visited.add(current);
            if(current.disabled) continue;
            res.add(current);
            BasicModInfo currInfo = getBasicInfo((ContainedModInfo) current);
            if(currInfo == null || currInfo.getId() == null) continue;
            enqueueIds(currInfo, queue);
        }
        return res;
    }
}
