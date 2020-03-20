package main;

import javax.swing.*;
import java.io.File;
import java.util.Arrays;

public class Main {

    // -v -complete "D:\Benutzer\Arthur\Arthurs medien\Documents\Intellij\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"

    static boolean log = false;
    static boolean help = false;
    static boolean helppane = false;
    static String  hpText = "";

    public static void main(String[] args) {
        try {
            if (args == null || args.length == 0) {
                args = new String[]{"-?"};
                helppane = true;
            }
            if (args.length > 1) {
                for (int i = 1; i < args.length; i++) {
                    if (args[i].equals("-?")) {
                        System.out.println("'-?' muss das erste Element sein");
                        System.exit(1);
                    }
                }
            }
            if (args[0].equalsIgnoreCase("-v")) {
                if (args.length == 1)
                    System.exit(1);
                log = true;
                String[] a = new String[args.length-1];
                System.arraycopy(args, 1, a, 0, a.length);
                args = a;
            }
            if (args[0].equals("-?"))
                help = true;
            else if (args.length == 1 && args[0].startsWith("-"))
                System.exit(1);
            log("\n");
            log(Interpreter.loadTranslation());
            if (args[0].startsWith("-")) {
                String[] args2 = new String[args.length - 1];
                System.arraycopy(args, 1, args2, 0, args2.length);
                args2 = makeAbsolutePaths(args2);
                switch (args[0]) {
                    case "-?":
                        hlog("",
                                "Verwendung: java -jar DeutschesJava.jar [-Optionen] Djava_Datei [Djava_Dateien...]",
                                "           (DeutschesJava.jar kann auch anders heissen)",
                                "wobei Optionen Folgendes umfasst:",
                                "    -v                Aktiviert den \"Viel-Text-Modus\" = Log-Ausgabe (muss an erster Stelle stehen!)",
                                "    -konvertieren     Wandelt Djava-Dateien in Java-Dateien um",
                                "    -kompilieren      Wandelt um und Kompiliert, Java-Dateien werden gelöscht",
                                "    -rennen           Wandelt um, kompiliert und führt aus, Java-Dateien werden gelöscht",
                                "    -komplett         Wandelt um, kompiliert und führt aus, Java-Dateien bleiben erhalten",
                                "    -nurkompilieren   Kompiliert bereits umgewandelte Dateien (Es muss trotzdem die .djava-Datei angegeben werden)",
                                "    -nurrennen        Führt Java-Klasse aus (Es muss trotzdem die .djava-Datei angegeben werden)");
                        if (helppane)
                            showHelpPane();
                        break;
                    case "-konvertieren":
                        log(Interpreter.makeJavaFile(args2));
                        break;
                    case "-kompilieren":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.deleteJavaFile(args2));
                        break;
                    case "-rennen":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.deleteJavaFile(args2));
                        log(Interpreter.run(args2[0]));
                        break;
                    case "-komplett":
                        log(Interpreter.makeJavaFile(args2));
                        log(Interpreter.compile(args2));
                        log(Interpreter.run(args2[0]));
                        break;
                    case "-nurkompilieren":
                        log(Interpreter.compile(args2));
                        break;
                    case "-nurrennen":
                        log(Interpreter.run(args2[0]));
                        break;
                    default:
                        System.exit(1);
                }
            } else {
                args = makeAbsolutePaths(args);
                Interpreter.makeJavaFile(args);
                Interpreter.compile(args);
                Interpreter.deleteJavaFile(args);
                Interpreter.run(args[0]);
            }
        } catch (Exception e) {
            System.exit(1);
        }
    }

    static String[] makeAbsolutePaths(String[] paths) {
        String[] ap = new String[paths.length];
        String currentLocation = System.getProperty("user.dir");
        for (int i = 0; i < ap.length; i++) {
            if (paths[i].startsWith("-"))
                continue;
            File f = new File(paths[i]);
            if (!f.exists() || !f.isAbsolute()) {
                ap[i] = new File(currentLocation, paths[i]).toString();
                log("created abs path: " + ap[i]);
            } else {
                ap[i] = paths[i];
            }
        }
        return ap;
    }

    static void log(String s) {
        if (log) {
            System.out.println(s);
        }
    }

    static void hlog(String ... h) {
        if (help) {
            StringBuilder b = new StringBuilder(hpText);
            for (String s : h) {
                System.out.println(s);
                b.append(s).append("\n");
            }
            hpText = b.toString();
        }
    }

    static void showHelpPane() {
        while (hpText.startsWith("\n"))
            hpText = hpText.substring(1);
        hpText = hpText.replaceAll("\\t", "            ");
        JOptionPane.showMessageDialog(null, hpText, "Usage help", JOptionPane.INFORMATION_MESSAGE);
    }

}
