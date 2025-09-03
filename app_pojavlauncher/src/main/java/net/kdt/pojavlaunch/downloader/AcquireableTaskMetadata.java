package net.kdt.pojavlaunch.downloader;

import java.io.IOException;

public abstract class AcquireableTaskMetadata extends TaskMetadata {
    public AcquireableTaskMetadata(int mirrorType) {
        super(null, null, mirrorType);
    }

    /**
     * Fill the missing fields of this AcquireableTaskMetadata (by, for example, performing an API request)
     * @throws IOException if metadata acquisition failed
     */
    public abstract void acquireMetadata() throws IOException;
}
