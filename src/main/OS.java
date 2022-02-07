package main;

import java.util.Locale;

public class OS {

    public enum OS_FAMILY {
        WINDOWS, MAC, LINUX, SOLARIS, UNKNOWN
    }

    private static final String OS;
    private static final OS_FAMILY thisOS;

    static {
        OS = System.getProperty("os.name", "unspecified").toLowerCase(Locale.ENGLISH);

        if (OS.contains("mac") || OS.contains("darwin"))
            thisOS = OS_FAMILY.MAC;
        else if (OS.contains("win"))
            thisOS = OS_FAMILY.WINDOWS;
        else if (OS.contains("nix") || OS.contains("nux") || OS.contains("aix"))
            thisOS = OS_FAMILY.LINUX;
        else if (OS.contains("sunos"))
            thisOS = OS_FAMILY.SOLARIS;
        else
            thisOS = OS_FAMILY.UNKNOWN;
    }

    public static boolean isWindows() {
        return thisOS == OS_FAMILY.WINDOWS;
    }
    public static boolean isMac() {
        return thisOS == OS_FAMILY.MAC;
    }
    public static boolean isLinux() {
        return thisOS == OS_FAMILY.LINUX;
    }
    public static boolean isSolaris() {
        return thisOS == OS_FAMILY.SOLARIS;
    }
    public static boolean isUnknown() {
        return thisOS == OS_FAMILY.UNKNOWN;
    }

    public static OS_FAMILY getOS() {
        return thisOS;
    }

    public static String getOsName() {
        return System.getProperty("os.name", "unspecified");
    }

}
