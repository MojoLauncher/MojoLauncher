package net.kdt.pojavlaunch.modloaders;

public class ComparableVersionString implements Comparable<ComparableVersionString> {
    private int major;
    private int minor;
    private int patch;
    private final String original;
    private final boolean isValid;

    private ComparableVersionString(String str) {
        this.original = str;
        this.isValid = false;
    }

    public ComparableVersionString(String original, int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.original = original;
        this.isValid = true;
    }

    @Override
    public int compareTo(ComparableVersionString str) {
        if(!this.isValid) return str.getProper().compareTo(this.original);

        if(this.major != str.major) return Integer.compare(this.major, str.major);
        if(this.minor != str.minor) return Integer.compare(this.minor, str.minor);
        if(this.patch != str.patch) return Integer.compare(this.patch, str.patch);
        return 0;
    }

    public String getOriginal() {
        return original;
    }

    /**
     * @return the original but if the patch was .0 it will not include it, e.g.
     *         "1.20.0" -> "1.20"
     */
    public String getProper() {
        if(!this.isValid) return original;

        StringBuilder sb = new StringBuilder();
        sb.append(major);
        sb.append('.');
        sb.append(minor);
        if(patch != 0) {
            sb.append('.');
            sb.append(patch);
        }
        return sb.toString();
    }

    public boolean isValid() {
        return isValid;
    }

    public static ComparableVersionString parse(String str) {
        String[] split = str.split("\\.");
        if (split.length < 2) return new ComparableVersionString(str);

        try {
            int major = Integer.parseInt(split[0]);
            int minor = Integer.parseInt(split[1]);
            int patch = (split.length >= 3) ? Integer.parseInt(split[2]) : 0;
            return new ComparableVersionString(str, major, minor, patch);
        } catch (NumberFormatException e) {
            return new ComparableVersionString(str);
        }
    }
}