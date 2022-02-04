package main;

import javax.swing.*;
import java.awt.*;

import main.Main.Flag;

public class Logger {

    static boolean logToSystemOut = false;
    static boolean suppressWarnings = false;  // no user setting yet

    public static void log(String s, Object... args) {
        if (logToSystemOut) {
            System.out.printf(s+"\n", args);
        }
    }

    public static void warning(String s, Object... args) {
        if (!suppressWarnings) {
            System.out.printf(s+"\n", args);
        }
    }

    public static void error(String s, Object... args) {
        System.err.printf(s+"\n", args);
    }

    static void logHelp(boolean helpDialog) {
        String[] helpTextBuild = {
                "",
                String.format("Verwendung: java -jar %s.jar [Optionen...] %s_Hauptdatei [%s_Dateien...] [%s Argumente...]", Main.LANGUAGE_NAME, Main.LANGUAGE_NAME, Main.LANGUAGE_NAME, fFlag(Flag.ARGS, "|")),
                String.format("            java -jar %s.jar %s", Main.LANGUAGE_NAME, fFlag(Flag.SETTINGS, "|")),
                String.format("           (%s.jar kann auch anders heißen)", Main.LANGUAGE_NAME),
                String.format("Die 'Argumente...' werden an das %s-Programm weitergeleitet, wenn es ausgeführt wird.", Main.LANGUAGE_NAME),
                String.format("Wird '%s' verwendet, sind Einstellungen einsehbar und änderbar.", fFlag(Flag.SETTINGS, "|")),
                "",
                String.format("Die Optionen umfassen Folgendes:"),
                String.format("    %s   Zeigt Hilfe in der Konsole an", fFlag(Flag.HELP, "  ", true)),
                String.format("    %s   Aktiviert den 'Viel-Text-Modus' = Log-Ausgabe", fFlag(Flag.VERBOSE, "  ", true)),
                String.format("    %s   Wandelt %s-Dateien in Java-Dateien um", fFlag(Flag.CONVERT, "  ", true), Main.LANGUAGE_NAME),
                String.format("    %s   Wandelt um und kompiliert", fFlag(Flag.COMPILE, "  ", true)),
                String.format("    %s   Wandelt um, kompiliert und führt aus (STANDARD)", fFlag(Flag.RUN, "  ", true)),
                String.format("    %s   Erstellte Java-Dateien werden nicht gelöscht", fFlag(Flag.KEEP_JAVA, "  ", true)),
                String.format("    %s   Kompiliert bereits umgewandelte Dateien", fFlag(Flag.JUST_COMPILE, "  ", true)),
                String.format("    %s   Führt Java-Klassen aus", fFlag(Flag.JUST_RUN, "  ", true)),
                "",
        };
        String helpText = String.join("\n", helpTextBuild);

        System.out.println(helpText);
        if (helpDialog)
            showHelpDialog(helpText);
    }

    private static String fFlag(Flag f, String sep) {
        return fFlag(f, sep, false);
    }
    private static String fFlag(Flag f, String sep, boolean fillArgLength) {
        String s = f.S.isEmpty() ? "" : Flag.shortFlag + f.S;
        String l = f.L.isEmpty() ? "" : Flag.longFlag + f.L;
        String ret = s + (s.isEmpty() || l.isEmpty() ? "" : sep) + l;
        if (fillArgLength) {
            ret = " ".repeat(s.isEmpty() ? (Flag.shortFlag.length() + Flag.shortArgLength + sep.length()) : 0)
                    + ret
                    + " ".repeat((l.isEmpty() ? (sep.length() + Flag.longFlag.length()) : 0) + Flag.longArgMaxLength - f.L.length());
        }
        return ret;
    }

    private static void showHelpDialog(String out) {
        while (out.startsWith("\n"))
            out = out.substring(1);
        out = "<html>" + out.replaceAll("\\n", "<br>").replaceAll(" ", "&nbsp;");
        JLabel l = new JLabel(out);
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
        JOptionPane.showMessageDialog(null, l, "Hilfe", JOptionPane.INFORMATION_MESSAGE);
    }
}
