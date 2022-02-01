package main;

import javax.swing.*;
import java.awt.*;
import main.Main.Flag;

public class Logger {

    static boolean logToSystemOut = true;

    public static void log(String s) {
        if (logToSystemOut) {
            System.out.println(s);
        }
    }

    static void logHelp(boolean helpDialog) {
        String[] helpText = {
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
        StringBuilder b = new StringBuilder();
        for (String s : helpText) {
            System.out.println(s);
            b.append(s).append("\n");
        }
        String t = b.toString();

        // replace placeholders
        // translation
        //  /*HELP;|)*/   -->   "(-s|--longflag)"
        //  /*HELP; | */  -->   "-s | --longflag"
        //  /*HELP;  */   -->   "-s  --longflag"
        //  /*HELP;  )*/  -->   "(-s  --longflag)"

        String replace = Flag.valueOf("HELP").S;

        log(t);
        if (helpDialog)
            showHelpDialog(t);
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
