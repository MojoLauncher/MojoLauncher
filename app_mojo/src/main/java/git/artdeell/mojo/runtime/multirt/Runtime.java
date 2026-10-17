package git.artdeell.mojo.runtime.multirt;

import java.util.Objects;

public class Runtime {
    public final String name;
    public final String versionString;
    public final String arch;
    public final int version;
    public final String root;
    public Runtime(String name) {
        this.name = name;
        this.versionString = null;
        this.arch = null;
        this.root = null;
        this.version = 0;
    }
    Runtime(String name, String versionString, String arch, String root, int version) {
        this.name = name;
        this.versionString = versionString;
        this.arch = arch;
        this.version = version;
        this.root = root;
    }
    


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Runtime runtime = (Runtime) o;
        return name.equals(runtime.name);
    }
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}