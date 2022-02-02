package main;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

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
                "Verwendung: java -jar /*L_NAME*/.jar [/*.S*/Optionen...] /*L_NAME*/_Datei [/*L_NAME*/_Dateien...] [/*ARGS;|)*/ Argumente...]",
                "            java -jar /*L_NAME*/.jar /*SETTINGS;|)*/",
                "           (/*L_NAME*/.jar kann auch anders heißen)",
                "Die 'Argumente...' werden an das DJava-Programm weitergeleitet, wenn es ausgeführt wird.",
                "Wird '/*S_FLAG_9*/' verwendet, sind Einstellungen einsehbar und änderbar.",
                "",
                "Die Optionen umfassen Folgendes:",
                "    /*HELP*/   Zeigt Hilfe in der Konsole an",
                "    /*VERBOSE*/   Aktiviert den 'Viel-Text-Modus' = Log-Ausgabe",
                "    -u  --umwandeln        Wandelt DJava-Dateien in Java-Dateien um",
                "    -k  --kompilieren      Wandelt um und kompiliert",
                "    -r  --rennen           Wandelt um, kompiliert und führt aus (STANDARD)",
                "    -j  --behaltejava      Erstellte Java-Dateien werden nicht gelöscht",
                "    -K  --nurkompilieren   Kompiliert bereits umgewandelte Dateien (Es muss trotzdem die .djava-Endung angegeben werden)",
                "    -R  --nurrennen        Führt Java-Klasse aus (Es muss trotzdem die .djava-Endung angegeben werden)",
        };
        String helpText = String.join("\n", helpTextBuild);

        // replace placeholders
        // translation
        //  /*HELP;|)*/   -->   "(-s|--longflag)"
        //  /*HELP; | */  -->   "-s | --longflag"
        //  /*HELP;  */   -->   "-s  --longflag"
        //  /*HELP;  )*/  -->   "(-s  --longflag)"

        String replace = Flag.valueOf("HELP").S;  // method for replacing

        System.out.println(helpText);
        if (helpDialog)
            showHelpDialog(helpText);
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
