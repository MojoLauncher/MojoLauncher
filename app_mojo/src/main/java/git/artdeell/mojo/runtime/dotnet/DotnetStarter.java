package git.artdeell.mojo.runtime.dotnet;

public class DotnetStarter {

    public static void startDotnet(String dotnetRoot, String root, String... args) {
        String[] _args = new String[args.length+1];
        System.arraycopy(args, 0, _args, 1, args.length);
        _args[0] = dotnetRoot + "/dotnet";
        nrunDotnet(dotnetRoot, root, _args);
    }

    public static native void nrunDotnet(String dotnetRoot, String root, String[] args);
}
