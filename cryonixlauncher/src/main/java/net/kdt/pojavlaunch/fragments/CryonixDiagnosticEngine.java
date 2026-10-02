package net.kdt.pojavlaunch.fragments;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Local crash/log diagnostic engine for Cryonix AI Assist.
 *
 * It does not pretend to be a remote LLM: it extracts the actual exception,
 * native error and relevant launcher/game context from the supplied log and
 * turns known signatures into concrete recovery steps.
 */
public final class CryonixDiagnosticEngine {

    private CryonixDiagnosticEngine() {}

    public static String analyze(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "Paste the crash text or launcher log here and I’ll analyze it.\n\n"
                    + "Best input: the full stack trace, especially the first "
                    + "“Caused by:” line and the lines immediately below it.";
        }

        String log = raw.replace("\r", "");
        String lower = log.toLowerCase(Locale.ROOT);
        String exception = extractException(log);
        StringBuilder out = new StringBuilder();

        out.append("🔎 Cryonix crash analysis\n\n");
        if (exception != null) {
            out.append("Detected error\n").append(exception).append("\n\n");
        }

        if (contains(lower, "unsatisfiedlinkerror") || contains(lower, "no implementation found")
                || contains(lower, "native method")) {
            out.append("Likely area: native library / JNI mismatch.\n\n")
               .append("Steps to try:\n")
               .append("1. Fully close Cryonix Launcher and open it again.\n")
               .append("2. Reinstall or refresh the Java/runtime used by the affected instance.\n")
               .append("3. If the crash started after an APK update, clear the launcher app cache and retry.\n")
               .append("4. Make sure you are using the correct build for your device ABI (arm64-v8a / armeabi-v7a / x86_64).\n")
               .append("5. If the same JNI symbol is still missing, send the complete stack trace — the missing native method identifies the exact backend that needs investigation.\n\n");
        } else if (contains(lower, "outofmemoryerror") || contains(lower, "java heap space")
                || contains(lower, "could not reserve enough space")) {
            out.append("Likely area: memory allocation.\n\n")
               .append("Steps to try:\n")
               .append("1. Lower the launcher RAM allocation instead of giving Minecraft nearly all device RAM.\n")
               .append("2. Close other apps before launching.\n")
               .append("3. Disable heavy shaders/resource packs and lower render distance.\n")
               .append("4. If it only happens with one modpack, test the same Minecraft version without mods.\n\n");
        } else if (contains(lower, "classcastexception") || contains(lower, "noclassdeffounderror")
                || contains(lower, "classnotfoundexception") || contains(lower, "nosuchmethoderror")
                || contains(lower, "nosuchfielderror")) {
            out.append("Likely area: incompatible or missing Java/mod/library classes.\n\n")
               .append("Steps to try:\n")
               .append("1. Read the class named after the exception — it usually points to the incompatible component.\n")
               .append("2. Check that the mod/loader version matches the Minecraft version.\n")
               .append("3. Remove the last mod you installed and launch again.\n")
               .append("4. For a loader crash, reinstall the matching Fabric/Forge/NeoForge/Quilt loader.\n")
               .append("5. If a library is explicitly missing, reinstall that instance rather than randomly replacing jars.\n\n");
        } else if (contains(lower, "glfw") || contains(lower, "opengl") || contains(lower, "egl")
                || contains(lower, "vulkan") || contains(lower, "libgl")) {
            out.append("Likely area: graphics/renderer initialization.\n\n")
               .append("Steps to try:\n")
               .append("1. Switch to another renderer available in Cryonix Launcher.\n")
               .append("2. Disable shaders and advanced graphics options.\n")
               .append("3. Lower resolution/render distance and test vanilla Minecraft.\n")
               .append("4. If the log names a specific OpenGL/EGL/Vulkan function, keep that exact line when reporting the crash.\n\n");
        } else if (contains(lower, "awtbridge") || contains(lower, "surfacecreated")
                || contains(lower, "nativebeginrendering")) {
            out.append("Likely area: Android AWT/rendering bridge.\n\n")
               .append("Steps to try:\n")
               .append("1. Confirm the crash happens only when the affected game/OptiFine screen opens.\n")
               .append("2. Test the same Minecraft version without OptiFine or the last rendering-related mod.\n")
               .append("3. Try a different renderer/runtime combination.\n")
               .append("4. If the log contains “nativeBeginRendering” or another missing JNI symbol, keep the entire UnsatisfiedLinkError block for developer debugging.\n\n");
        } else if (contains(lower, "unsupportedclassversionerror") || contains(lower, "class file version")) {
            out.append("Likely area: wrong Java version.\n\n")
               .append("Steps to try:\n")
               .append("1. Find the required Java version in the error text.\n")
               .append("2. Select/install a matching runtime in Cryonix Launcher.\n")
               .append("3. Re-launch the same instance without changing mods yet.\n\n");
        } else if (contains(lower, "modresolutionexception") || contains(lower, "depends on")
                || contains(lower, "incompatible mod set") || contains(lower, "requires")) {
            out.append("Likely area: mod dependency/version conflict.\n\n")
               .append("Steps to try:\n")
               .append("1. Find the first “requires”, “depends on”, or “incompatible” line.\n")
               .append("2. Install the exact dependency version requested there.\n")
               .append("3. Remove duplicate/older copies of the same mod from the instance.\n")
               .append("4. Verify every mod targets the same Minecraft version and loader.\n\n");
        } else if (contains(lower, "connectexception") || contains(lower, "unknownhostexception")
                || contains(lower, "sockettimeoutexception") || contains(lower, "sslhandshakeexception")
                || contains(lower, "http 403") || contains(lower, "http 404")) {
            out.append("Likely area: network/download service.\n\n")
               .append("Steps to try:\n")
               .append("1. Check the connection and retry the download.\n")
               .append("2. If only one file fails, keep its URL/status line in the report.\n")
               .append("3. Check date/time and certificate-related errors for SSL failures.\n")
               .append("4. Do not replace random libraries when the log points to a remote HTTP error.\n\n");
        } else if (contains(lower, "permission denied") || contains(lower, "eacces")
                || contains(lower, "access denied") || contains(lower, "read-only file system")) {
            out.append("Likely area: storage/file permission.\n\n")
               .append("Steps to try:\n")
               .append("1. Verify Cryonix Launcher can access its game storage.\n")
               .append("2. Retry after granting the storage access requested by Android.\n")
               .append("3. Check whether the failing path is inside the launcher/game directory.\n")
               .append("4. If only one file is affected, recreate that file rather than changing unrelated permissions.\n\n");
        } else if (contains(lower, "exit code: 1") || contains(lower, "process exited with code 1")) {
            out.append("Detected: Minecraft exited with code 1, but that code alone does not identify the cause.\n\n")
               .append("Steps to try:\n")
               .append("1. Scroll upward to the first “Caused by:” or exception before the exit code.\n")
               .append("2. Check the first mod/loader error above it.\n")
               .append("3. Test a clean instance of the same Minecraft version.\n")
               .append("4. Send the full latest log if the cause is still unclear.\n\n");
        } else if (contains(lower, "signal 11") || contains(lower, "sigsegv")
                || contains(lower, "segmentation fault") || contains(lower, "fatal signal")) {
            out.append("Likely area: native crash.\n\n")
               .append("Steps to try:\n")
               .append("1. Record the exact signal and native library named near it.\n")
               .append("2. Test another renderer and a clean instance.\n")
               .append("3. Disable shaders/mods that hook rendering.\n")
               .append("4. If it repeats, provide the native crash block and device ABI; Java stack traces alone may not explain it.\n\n");
        } else if (contains(lower, "optifine")) {
            out.append("Likely area: OptiFine/rendering compatibility.\n\n")
               .append("Steps to try:\n")
               .append("1. Test the same Minecraft version without OptiFine.\n")
               .append("2. If vanilla works, test the exact OptiFine build against that Minecraft version.\n")
               .append("3. Check the log for AWT, OpenGL, GLFW or native-library errors.\n")
               .append("4. If another performance/rendering mod is installed, test without it too.\n\n");
        } else {
            out.append("I could not safely identify a known signature from this log.\n\n")
               .append("Do this next:\n")
               .append("1. Find the first exception or “Caused by:” line.\n")
               .append("2. Include 15–30 lines after that line.\n")
               .append("3. If the crash is native, include the signal/native library block too.\n")
               .append("4. Tell me what you were doing immediately before the crash (launch, login, OptiFine, mod install, etc.).\n\n");
        }

        String version = firstMatch(log, "(?i)Minecraft(?: version)?[:= ]+([0-9][0-9A-Za-z.\-]+)");
        String java = firstMatch(log, "(?i)Java(?: version)?[:= ]+([^\\n]+)");
        if (version != null || java != null) {
            out.append("Context found:\n");
            if (version != null) out.append("• Minecraft: ").append(version).append("\n");
            if (java != null) out.append("• Java: ").append(java.trim()).append("\n");
            out.append("\n");
        }

        out.append("If these steps do not fix it, send the full latest log to Cryonix AI Assist. "
                + "I’ll use the exact exception/native symbol/mod name instead of guessing.");
        return out.toString();
    }

    private static boolean contains(String text, String value) {
        return text.contains(value);
    }

    private static String extractException(String log) {
        String[] lines = log.split("\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("Exception") || trimmed.contains("Error:")
                    || trimmed.startsWith("Caused by:") || trimmed.contains("FATAL EXCEPTION")) {
                if (trimmed.length() > 220) trimmed = trimmed.substring(0, 220) + "…";
                return trimmed;
            }
        }
        return null;
    }

    private static String firstMatch(String input, String regex) {
        try {
            Matcher m = Pattern.compile(regex).matcher(input);
            return m.find() ? m.group(1) : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
