package main;

import convert.Interpreter;
import run.Runner;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {

    // -v -complete "D:\Benutzer\Arthur\Arthurs medien\Documents\Intellij\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"
    // -v -a "D:\Users\Art\Coding\Intellij\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"
    // new template:
    // -v -u "D:\Users\Art\Coding\IdeaProjects\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"

    public static final String LANGUAGE_NAME = "DJava";
    public static final String EXTENSION_NAME = "djava";

    // program flags
    public enum Flag {
        HELP("?", "hilfe"),
        VERBOSE("v", ""),
        CONVERT("u", "umwandeln"),
        COMPILE("k", "kompilieren"),
        RUN("r", "rennen"),
        KEEP_JAVA("j", "behaltejava"),
        JUST_COMPILE("K", "nurkompilieren"),
        JUST_RUN("R", "nurrennen"),
        ARGS("a", ""),
        SETTINGS("s", ""),
        ;

        public static final String shortFlag = "-";
        public static final String longFlag = "--";

        public final String S;
        public final String L;

        public boolean set;

        Flag(String sArg, String lArg) {
            S = sArg;
            L = lArg;
            set = false;
        }

        public static long countSet() {
            return Arrays.stream(Flag.values()).map(f -> f.set).filter(b -> b).count();
        }
    }

    public static void main(String[] args) {
        boolean helpDialog = false;

        // THIS IS TESTING AREA
        //
        if (evaluateArgs(args) != null) {
            // String[0][0] -> help log (+ dialog)
            // String[1][0] -> settings
            System.out.println("true flags: "+ Flag.countSet());
            new Interpreter().loadTranslation();
            return;
        }

        try {
            if (args == null || args.length == 0) {
                args = new String[]{"-?"};
                helpDialog = true;
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
                Logger.logToSystemOut = true;
                String[] a = new String[args.length-1];
                System.arraycopy(args, 1, a, 0, a.length);
                args = a;
            }
            /*if (args[0].equals("-?"))
                help = true;
            else*/ if (args.length == 1 && args[0].startsWith("-"))
                System.exit(1);
            Logger.log("\n");
            if (new Interpreter().loadTranslation())
                ;
            /*if (args[0].startsWith("-")) {
                String[] args2 = new String[args.length - 1];
                System.arraycopy(args, 1, args2, 0, args2.length);
                args2 = makeAbsolutePaths(args2);
                switch (args[0]) {
                    case "-?":
                        Logger.logHelp(helpDialog);
                        break;
                    case "-u":
                    case "-konvertieren":
                        Logger.log(Interpreter.makeJavaFile(args2));
                        break;
                    case "-k":
                    case "-kompilieren":
                        Logger.log(Interpreter.makeJavaFile(args2));
                        Logger.log(Interpreter.compile(args2));
                        Logger.log(Interpreter.deleteJavaFile(args2));
                        break;
                    case "-r":
                    case "-rennen":
                        Logger.log(Interpreter.makeJavaFile(args2));
                        Logger.log(Interpreter.compile(args2));
                        Logger.log(Interpreter.deleteJavaFile(args2));
                        Logger.log(Interpreter.run(args2[0]));
                        break;
                    case "-a":
                    case "-komplett":
                        Logger.log(Interpreter.makeJavaFile(args2));
                        Logger.log(Interpreter.compile(args2));
                        Logger.log(Interpreter.run(args2[0]));
                        break;
                    case "-K":
                    case "-nurkompilieren":
                        Logger.log(Interpreter.compile(args2));
                        break;
                    case "-R":
                    case "-nurrennen":
                        Logger.log(Interpreter.run(args2[0]));
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
            }*/
        } catch (Exception e) {
            System.exit(1);
        }
    }

    private static String[][] evaluateArgs(String[] args) {
        // return array: dimension 0 are DJava files, dimension 1 are run arguments
        String[][] ret = new String[2][];
        List<String> files = new ArrayList<>();

        // empty args? return String[0][0] to trigger help dialog
        if (args.length == 0)
            return new String[0][0];

        // check for flags
        int runArgsPos = -1;

        List<String> flagListS = Arrays.stream(Flag.values()).map(f -> Flag.shortFlag + f.S).toList();
        List<String> flagListL = Arrays.stream(Flag.values()).map(f -> Flag.longFlag + f.L).toList();

        int k = 0;
        for (String s : args) {
            Flag f = null;
            if (flagListS.contains(s)) {
                (f = Flag.values()[flagListS.indexOf(s)]).set = true;
            } else if (flagListL.contains(s)) {
                (f = Flag.values()[flagListL.indexOf(s)]).set = true;
            } else if (runArgsPos == -1) {  // don't add run arguments
                files.add(s);
            }
            // save position of '-a' for run argument extraction
            if (f == Flag.ARGS)
                runArgsPos = k;
            k++;
        }

        // settings? return String[1][0] to trigger settings menu
        if (Flag.SETTINGS.set)
            return new String[1][0];

        // put all non-flags (DJava files) in ret[0]
        ret[0] = files.toArray(new String[0]);

        //  put run arguments in ret[1]; they must come after -a
        if (Flag.ARGS.set && runArgsPos > -1) {
            ret[1] = Arrays.copyOfRange(args, runArgsPos + 1, args.length);
        }

        return ret;
    }

    static String[] makeAbsolutePaths(String[] paths) {
        String[] ap = new String[paths.length];
        String currentLocation = System.getProperty("user.dir");  // this is the current location of the calling process (cmd, terminal, IDE, ...)
        System.out.println("CURRENT LOCATION: "+currentLocation);
        for (int i = 0; i < ap.length; i++) {
            if (paths[i].startsWith("-"))
                continue;
            File f = new File(paths[i]);
            if (!f.exists() || !f.isAbsolute()) {
                ap[i] = new File(currentLocation, paths[i]).toString();
                Logger.log("Pfad erstellt: " + ap[i]);
            } else {
                ap[i] = paths[i];
            }
        }
        return ap;
    }
    public static String compile(String... filePaths) {
        try {
            File[] files = new File[filePaths.length];
            for (int i = 0; i < files.length; i++) {
                String fn = new File(filePaths[i]).getName();
                files[i] = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + "java");
            }

            if (ToolProvider.getSystemJavaCompiler() != null) {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                Logger.log("System-Kompilierer gefunden");
                //Main.log("compiler: " + compiler.toString());
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                //Main.log("fileManager: " + fileManager.toString());

                ArrayList<File> af = new ArrayList<>(List.of(files[0].getParentFile()));
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, af);

                Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjects(files);

                if (compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call())
                    return "Kompilierung erfolgreich";
                else
                    return "Kompilierung fehlgeschlagen";
            } else {
                Logger.log("Kein System-Kompilierer gefunden, versuche manuelles Kompilieren mit Befehlen " +
                        "(funktioniert nur, wenn ein Kompilierer im PFAD steht)");
                // compile by command
                try {
                    StringBuilder s = new StringBuilder();
                    for (File file : files) {
                        s.append(" \"").append(file.toString()).append("\"");
                    }
                    //Main.log(s.toString());
                    Process p = Runtime.getRuntime().exec("javac" + s, null, new File(System.getProperty("user.dir")));
                    try {
                        while (p.isAlive())
                            Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                    return "Kompilierung durch Befehle beendet mit Rückgabewert: " + p.exitValue();
                } catch (IOException e) {
                    return "Kompilierung durch Befehle fehlgeschlagen: " + e.getMessage();
                }
            }
        } catch (IOException e) {
            return "Kompilierung fehlgeschlagen: " + e.getMessage();
        } catch (NullPointerException e) {
            return "Während der Kompilierung ist eine NULL aufgetreten";
        }
    }

    public static String deleteJavaFile(String... filePaths) {
        StringBuilder s = new StringBuilder("Java-Dateien gelöscht, bis auf: ");
        for (int i = 0; i < filePaths.length; i++) {
            String fn = new File(filePaths[i]).getName();
            File f = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + "java");
            if (!f.delete())
                s.append(i);
        }
        return s.toString();
    }

    public static String run(String mainFilePath) {
        String s = mainFilePath.substring(0, mainFilePath.length() - 5) + "class";
        String l = "Starte Ausführung mit Argumenten: \"" + s + "\"\n";
        Runner runner = Runner.newRunner();
        if (runner == null)
            return l + "Ausführung nicht möglich, Betriebssystem nicht unterstützt (" + OS.getOsName() + ")";
        return (l + "Rückgabe der Ausführung: " + runner.start(new String[]{s}));
    }

}
