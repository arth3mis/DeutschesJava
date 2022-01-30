package main;

import java.util.Locale;

public class OS {

    private static final String OS = System.getProperty("os.name", "unspecified").toLowerCase(Locale.ENGLISH);

    public static boolean isWindows() {
        return (OS.contains("win"));
    }
    public static boolean isMac() {
        return (OS.contains("mac"));
    }
    public static boolean isLinux() {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }
    public static boolean isSolaris() {
        return (OS.contains("sunos"));
    }

    public static String getOsName() {
        return System.getProperty("os.name", "unspecified");
    }

}
