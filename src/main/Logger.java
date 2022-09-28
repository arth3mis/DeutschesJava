package main;

import javax.swing.*;
import java.awt.*;
import java.util.Scanner;

import main.Main.Flag;

import static main.Main.*;

public class Logger {

    static boolean suppressWarnings = false;  // no user setting yet
    static boolean debugMessages = false;

    private static final Scanner scanner = new Scanner(System.in);

    public static void log(String s, Object... args) {
        if (Flag.VERBOSE.set) {
            System.out.printf(s+"\n", args);
        }
    }

    public static void info(String s, Object... args) {
        System.out.printf(s+"\n", args);
    }

    public static void warning(String s, Object... args) {
        if (!suppressWarnings) {
            System.out.printf(s+"\n", args);
        }
    }

    public static void error(String s, Object... args) {
        System.out.printf(/*"Fehler: "+*/s+"\n", args);
    }

    public static String request(String s, Object... args) {
        System.out.printf(s+": ", args);
        return scanner.nextLine();
    }

    public static boolean isDebug() {
        return debugMessages;
    }
    public static void debug(String s, Object... args) {
        if (debugMessages) {
            System.out.printf(s+"\n", args);
        }
    }

    static void logHelp(boolean helpDialog) {
        String[] helpTextBuild = {
                "",
                "Verwendung: java -jar %s.jar [Optionen...] %s_Hauptdatei [%s_Dateien...] [%s Argumente...]"            .formatted(LANGUAGE_NAME, LANGUAGE_NAME, LANGUAGE_NAME, fFlag(Flag.ARGS, "|")),
                "            java -jar %s.jar %s|%s"                                                                    .formatted(LANGUAGE_NAME, fFlag(Flag.SETTINGS, "|"), fFlag(Flag.GENERATE_SYNTAX, "|")),
                "            java -jar %s.jar %s [?]Suchbegriff"                                                        .formatted(LANGUAGE_NAME, fFlag(Flag.SEARCH_TRANSLATION, "|")),
                "           (%s.jar kann auch anders heißen)"                                                           .formatted(LANGUAGE_NAME),
                "Die 'Argumente...' werden an das %s-Programm weitergeleitet, wenn es ausgeführt wird."                 .formatted(LANGUAGE_NAME),
                "Ohne Argumente wird die Hilfe in der Konsole und in einem grafischen Dialog angezeigt.",
                "",
                "Alternative Verwendungen:",
                "    %s   Einstellungen einsehen und ändern"                                                            .formatted(fFlag(Flag.SETTINGS, "  ", true)),
                "    %s   Generiere Syntax-Hervorhebung und Auto-Vervollständigungen (Sublime Text)"                    .formatted(fFlag(Flag.GENERATE_SYNTAX, "  ", true)),
                "    %s   Suche nach Übersetzung ('?': ignoriere Groß-/Kleinschreibung)"                                .formatted(fFlag(Flag.SEARCH_TRANSLATION, "  ", true)),
                "",
                "Die Optionen umfassen Folgendes:",
                "    %s   Zeigt Hilfe in der Konsole an"                                                                .formatted(fFlag(Flag.HELP, "  ", true)),
                "    %s   Aktiviert den 'Viel-Text-Modus' = Log-Ausgabe"                                                .formatted(fFlag(Flag.VERBOSE, "  ", true)),
                "    %s   Wandelt %s-Dateien in Java-Dateien um"                                                        .formatted(fFlag(Flag.CONVERT, "  ", true), LANGUAGE_NAME),
                "    %s   Wandelt um und kompiliert"                                                                    .formatted(fFlag(Flag.COMPILE, "  ", true)),
                "    %s   Wandelt um, kompiliert und führt aus (STANDARD)"                                              .formatted(fFlag(Flag.RUN, "  ", true)),
                "    %s   Kompiliert Java-Dateien"                                                                      .formatted(fFlag(Flag.JUST_COMPILE, "  ", true)),
                "    %s   Führt Java-Klassen aus"                                                                       .formatted(fFlag(Flag.JUST_RUN, "  ", true)),
                "    %s   Spezielle Ausführung (Windows: eigenes Prozess-Fenster)"                                      .formatted(fFlag(Flag.SPECIAL_RUN, "  ", true)),
                "    %s   Auch Dateien, die nicht auf '.%s' enden, werden akzeptiert"                                   .formatted(fFlag(Flag.IGNORE_EXT, "  ", true), EXTENSION_NAME),
                "    %s   Fügt alle %s-Dateien aus diesem Ordner und Unterordnern hinzu"                                .formatted(fFlag(Flag.INCLUDE_ALL, "  ", true), LANGUAGE_NAME),
                "    %s   Erstellte Java-Dateien werden nach Kompilierung nicht gelöscht"                               .formatted(fFlag(Flag.KEEP_JAVA, "  ", true)),
                "    %s   Erstellte Klassen-Dateien werden nach Ausführung gelöscht"                                    .formatted(fFlag(Flag.DELETE_CLASS, "  ", true)),
                "",
                "Statt Dateinamen kann auch '%s' angegeben werden (= alle Dateien im aktuellen Ordner)."                .formatted(Main.WILDCARD),
                "",
                "Beim Rennen wird die erste Datei als Hauptklasse verwendet, diese sollte manuell angegeben werden.",
                "Die Kombination '%s' und '%s' sollte vorsichtig verwendet werden."                                     .formatted(fFlag(Flag.INCLUDE_ALL, "|"), fFlag(Flag.IGNORE_EXT, "|")),
                "",
        };
        String helpText = String.join("\n", helpTextBuild);

        String title = "%s - Deutsches Java von AJ - V%d (© Arthur Freye && Jannis Müller %d)".formatted(LANGUAGE_NAME, VERSION, YEAR);

        System.out.println(title + "\n" + helpText);
        if (helpDialog)
            showHelpDialog(helpText, title);
    }

    public static String fFlag(Flag f, String sep) {
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

    private static void showHelpDialog(String out, String title) {
        // avoid errors on headless systems
        if (GraphicsEnvironment.isHeadless())
            return;
        while (out.startsWith("\n"))
            out = out.substring(1);
        out = "<html>" + out.replaceAll("\\n", "<br>").replaceAll(" ", "&nbsp;");
        JLabel l = new JLabel(out);
        l.setFont(new Font("Monospaced", Font.BOLD, 12));
        JOptionPane.showMessageDialog(null, l, title, JOptionPane.INFORMATION_MESSAGE);
    }
}
