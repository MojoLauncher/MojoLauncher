package git.artdeell.mojo.runtime.dotnet;

import android.system.Os;

import java.io.File;
import java.io.IOException;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.runtime.multirt.Runtime;
import git.artdeell.mojo.utils.SymlinkUtils;

public class DotnetStarter {
    public static native void runDotnet(String dotnetRoot, String gameRoot);
}
