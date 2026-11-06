package git.artdeell.mojo.tasks.mojson;

import git.artdeell.mojo.JVersionList;
import git.artdeell.mojo.extra.ExtraConstants;
import git.artdeell.mojo.extra.ExtraCore;
import git.artdeell.mojo.instances.Instance;

import java.io.File;

public class MoJsonExtras {
    public static String normalizeVersionId(String versionString) {
        JVersionList versionList = (JVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) return versionString;
        if(Instance.VERSION_LATEST_RELEASE.equals(versionString)) versionString = versionList.latest.get("release");
        if(Instance.VERSION_LATEST_SNAPSHOT.equals(versionString)) versionString = versionList.latest.get("snapshot");
        return versionString;
    }

    public static JVersionList.Version getListedVersion(String normalizedVersionString) {
        JVersionList versionList = (JVersionList) ExtraCore.getValue(ExtraConstants.RELEASE_TABLE);
        if(versionList == null || versionList.versions == null) return null; // can't have listed versions if there's no list
        for(JVersionList.Version version : versionList.versions) {
            if(version.id.equals(normalizedVersionString)) return version;
        }
        return null;
    }

    public interface DoneListener{
        void onDownloadDone(File[] classpath);
        void onDownloadFailed(Throwable throwable);
    }
}
