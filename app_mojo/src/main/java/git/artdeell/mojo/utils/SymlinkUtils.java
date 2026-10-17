package git.artdeell.mojo.utils;

import android.system.ErrnoException;
import android.system.Os;

import java.io.File;
import java.io.IOException;

public class SymlinkUtils {
    private final File mGameLibDir;
    private final File mNativeLibDir;

    public SymlinkUtils(File mGameLibDir, File mNativeLibDir) {
        this.mGameLibDir = mGameLibDir;
        this.mNativeLibDir = mNativeLibDir;
    }

    public void symlinkLibrary(String srcLib, String targetLib) throws IOException {

        File src = new File(mNativeLibDir, srcLib);
        File targetFile = new File(mGameLibDir, targetLib);

        try {
            String link = Os.readlink(targetFile.getAbsolutePath());
            if(new File(link).canRead()) return;
        }catch (ErrnoException ignored) {}

        boolean ignored = targetFile.delete();

        try {
            Os.symlink(src.getAbsolutePath(), targetFile.getAbsolutePath());
        }catch (ErrnoException e) {
            throw new IOException(e);
        }
    }
}
