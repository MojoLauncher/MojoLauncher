package git.artdeell.mojo.vulkan.driver;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class DisplayDriver implements Driver {
    public String name;
    public String author;
    public String driverVersion;
    protected DisplayDriver() {}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getMainLibrary() {
        return null;
    }

    public String getHash() {
        String to = name + "::" + author;
        return UUID.nameUUIDFromBytes(to.getBytes(StandardCharsets.UTF_8)).toString();
    }

}
