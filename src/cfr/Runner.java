package cfr;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

public class Runner {

    static File f;
    static String OS = System.getProperty("os.name", "unspecified").toLowerCase(Locale.ENGLISH);

    public static String start(String[] args) {
        if (args == null || args.length == 0)
            return "Keine Argumente gegeben (interner Fehler)";
        f = new File(args[0]);
        if (!f.exists() || !f.isFile())
            return "Argument ist keine Datei";

        // MAC
        if (isMac()) {
            return runMac();
        }
        // WINDOWS
        else if (isWindows()) {
            return runWindows();
        }
        // LINUX/UNIX
        else if (isLinux()) {
            return runLinux();
        }
        // SOLARIS
        else if (isSolaris()) {
            return "'rennen' ist noch nicht möglich mit Betriebssystem: Solaris";
        }
        // OTHER
        else {
            return "'rennen' ist noch nicht möglich mit Betriebssystem: (unbekannt)";
        }
    }

    private static String runWindows() {
        try {
            // TODO: make option to set path which is then saved in appdata; could also search PATH for java.../bin
            String s = "start cmd.exe @cmd /k \"\"C:\\Program Files\\Java\\jdk-17\\bin\\java.exe\" -cp \""+f.getParent()+"\" \""+f.getName().substring(0, f.getName().indexOf("."))+"\"&echo.&echo.&pause&exit\"";
            File x = new File(f.getParent(), "cfr_bat_tmp.bat");
            x.delete();
            if (!x.createNewFile())
                return "Fehler beim Erstellen der Stapel-Datei";
            FileWriter w = new FileWriter(x);
            w.write(s);
            w.write("\ndel \"%~f0\"");
            w.close();
            Process p = Runtime.getRuntime().exec("\""+x.toString()+"\"");
            while (p.isAlive());
            return "Erledigt (Endwert: " + p.exitValue() + ")";
        } catch (IOException e) {
            return "Prozess-Fehler: " + e.getMessage();
        }
    }

    private static String runMac() {
        return "'rennen' ist noch nicht möglich mit Betriebssystem: Mac";
    }

    private static String runLinux() {
        return "'rennen' ist noch nicht möglich mit Betriebssystem: Linux/Unix";
    }

    public static boolean isWindows() {
        return (OS.contains("win"));
    }
    public static boolean isMac() {
        return (OS.contains("mac") || OS.contains("darwin"));
    }
    public static boolean isLinux() {
        return (OS.contains("nix") || OS.contains("nux") || OS.indexOf("aix") > 0);
    }
    public static boolean isSolaris() {
        return (OS.contains("sunos"));
    }
}
