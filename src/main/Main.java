package main;

import compile.Compiler;
import convert.Converter;
import filesystem.Filer;
import run.Runner;

import javax.tools.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Main {

    // -v -complete "D:\Benutzer\Arthur\Arthurs medien\Documents\Intellij\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"
    // -v -a "D:\Users\Art\Coding\Intellij\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"
    // new template:
    // -v -u "D:\Users\Art\Coding\IdeaProjects\DeutschesJava\out\artifacts\DeutschesJava_jar\x.djava"

    public static final String LANGUAGE_NAME = "DJava";
    public static final String EXTENSION_NAME = "djava";
    public static final String JAVA_EXTENSION = "java";

    public static final String SOURCE_PATH = "src";

    public static final String WILDCARD = "*";

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
        SETTINGS("e", ""),
        ;

        public static final String shortFlag = "-";
        public static final String longFlag = "--";

        public static final int shortArgLength = 1;
        public static final int longArgMaxLength = Arrays.stream(Flag.values()).map(f -> f.L).max(Comparator.comparingInt(String::length)).get().length();

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
        String[][] eval = evaluateArgs(args);

        // verbose mode?
        if (Flag.VERBOSE.set)
            Logger.logToSystemOut = true;

        // display help dialog?
        if (eval.length == 0) {
            Logger.logHelp(true);
        }
        // settings?
        else if (eval.length == 1) {
            startSettings();
        }
        // standard call?
        else if (eval.length == 2) {
            // help?
            if (Flag.HELP.set)
                Logger.logHelp(false);

            // turn djava file names into files
            // check if they exist and have an extension (important for Interpreter.makeJavaFiles())
            // todo put in own function (File[] djavaFiles = parseFiles()?) to make main() clear and simple
            File[] djavaFiles = Arrays.stream(eval[0])
                    .map(File::new)
                    .filter(File::exists)
                    .filter(file -> file.getName().contains("."))
                    .toList().toArray(new File[0]);

            if (djavaFiles.length < 1) {
                Logger.error("Keine validen %s-Dateien gefunden.", LANGUAGE_NAME);
                return;
            } else {
                Logger.log("%d/%d %s-Dateien gefunden.", djavaFiles.length, eval[0].length, LANGUAGE_NAME);
            }

            // TODO TEST SYSTEM COMPILER VERSION BY RUNNING compiled java file manually with old jre/jdk
            //makeAbsolutePaths(new String[]{});
            //File f = new File("x.djava").getAbsoluteFile();
            //System.out.println("File: " + f + " - exists: " + f.exists());

            File[] javaFiles = null;
            File[] classFiles = null;
            // todo maybe obsolete declaration?
            Compiler compiler = null;
            Runner runner = null;

            // standard operation (run)?
            boolean standard = !(Flag.CONVERT.set || Flag.COMPILE.set || Flag.RUN.set || Flag.JUST_COMPILE.set || Flag.JUST_RUN.set);
            // resolve flags to actions
            boolean interpret = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.CONVERT.set;
            boolean compile   = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.JUST_COMPILE.set;
            boolean run       = standard || Flag.RUN.set || Flag.JUST_RUN.set;

            // actions
            //
            // convert djava to java
            if (interpret) {
                // todo put in own function to make main() clear and simple
                Converter c = new Converter(null);
                c.loadTranslation();
                javaFiles = c.makeJavaFiles(djavaFiles);
            }
            // compile java with set javac binary/exe, alternatively try system compiler
            if (compile) {
                compiler = Compiler.newInstance();
                if (javaFiles == null)
                    javaFiles = Filer.refactorExtension(djavaFiles, JAVA_EXTENSION);
                // don't compile if previous action failed
                if (javaFiles.length == 0)
                    Logger.warning("Kompilierung wird übersprungen.");
                else if (compiler == null) {
                    Logger.error("Kompilieren ist auf diesem Betriebssystem nicht unterstützt.");
                    classFiles = new File[0];
                } else
                    classFiles = compile(javaFiles);  // todo maybe make compile boolean and if true make classfiles (same purpose and cost)
            }
            // run class with set java binary/exe, alternatively try global java command
            if (run) {
                runner = Runner.newInstance();
                if (classFiles == null)
                    classFiles = Filer.refactorExtension(javaFiles != null ? javaFiles : djavaFiles, "");
                // don't run if previous action failed or run is not supported
                if (classFiles.length == 0)
                    Logger.warning("Ausführung wird übersprungen.");
                else if (runner == null)
                    Logger.error("Rennen ist auf diesem Betriebssystem nicht unterstützt.");
                else
                    run("");
            }

        }
        if (eval != null)
            return;

        try {
            if (args == null || args.length == 0) {
                args = new String[]{"-?"};
                //helpDialog = true;
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
                //Logger.logToSystemOut = true;
                String[] a = new String[args.length-1];
                System.arraycopy(args, 1, a, 0, a.length);
                args = a;
            }
            /*if (args[0].equals("-?"))
                help = true;
            else*/ if (args.length == 1 && args[0].startsWith("-"))
                System.exit(1);
            Logger.log("\n");
            //if (new Interpreter().loadTranslation())
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

    /**
     * @param args program launch arguments
     * @return new String[0][0] for help dialog; String[2] with {{DJava files}, {run arguments}}
     */
    private static String[][] evaluateArgs(String[] args) {
        String[][] ret = new String[2][];
        List<String> fileNames = new ArrayList<>();

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
                fileNames.add(s);
            }
            // save position of '-a' for run argument extraction
            if (f == Flag.ARGS)
                runArgsPos = k;
            k++;
        }

        // settings? return String[1][0] to trigger settings menu
        if (Flag.SETTINGS.set)
            return new String[1][];

        // replace wildcard with all djava files in working directory
        if (fileNames.contains(WILDCARD)) {
            // remove wildcard from list
            while (fileNames.remove(WILDCARD));

            // find all djava files present in current folder
            File workingDir = new File(System.getProperty("user.dir"));
            String[] newDjavaFiles = workingDir.list(
                    (dir, name) -> name.substring(name.lastIndexOf('.') + 1).equalsIgnoreCase(EXTENSION_NAME));
            // add all file names that are not already in the list
            if (newDjavaFiles != null)
                fileNames.addAll(Arrays.stream(newDjavaFiles).filter(name -> !fileNames.contains(name)).toList());
        }

        // put all non-flags (DJava files) in ret[0]
        ret[0] = fileNames.toArray(new String[0]);

        //  put run arguments in ret[1]; they must come after -a
        if (Flag.ARGS.set && runArgsPos > -1) {
            ret[1] = Arrays.copyOfRange(args, runArgsPos + 1, args.length);
        }

        return ret;
    }

    private static void startSettings() {
        Logger.warning("Einstellungen - Noch nicht implementiert");
    }

    static String[] makeAbsolutePaths(String[] paths) {
        String[] ap = new String[paths.length];
        String currentLocation = System.getProperty("user.dir");  // this is the current location of the calling process (cmd, terminal, IDE, ...)
        // ABOVE method is obsolete, since new File().getAbsoluteFile() works the same (uses calling process wd as root)
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
    public static File[] compile(File[] javaFiles) {
        if (javaFiles == null || javaFiles.length == 0)
            return new File[0];
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler != null) {
                Logger.log("System-Kompilierer gefunden.");
                //Main.log("compiler: " + compiler.toString());
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                //Main.log("fileManager: " + fileManager.toString());

                File pathRef = javaFiles[0].getAbsoluteFile().getParentFile();
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(pathRef));

                Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjects(javaFiles);

                if (compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call()) {
                    Logger.log("Kompilieren erfolgreich.");
                    return Filer.refactorExtension(javaFiles, "");
                } else {
                    Logger.error("\n------------------------------------------\nKompilieren fehlgeschlagen.");
                    return new File[0];
                }
            } else {
                Logger.warning("Kein System-Kompilierer gefunden, versuche manuelles Kompilieren mit Befehlen " +
                        "(funktioniert nur, wenn ein Kompilierer im PFAD steht)");
                // compile by command
                try {
                    StringBuilder s = new StringBuilder();
                    for (File file : javaFiles) {
                        s.append("\"").append(file.toString()).append("\"");
                    }
                    //Main.log(s.toString());
                    Process p = Runtime.getRuntime().exec("javac " + s, null/*, new File(System.getProperty("user.dir"))*/);  // user.dir will already be standard
                    try {
                        while (p.isAlive())
                            Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                    Logger.log("Kompilierung durch Befehle beendet mit Rückgabewert: ", p.exitValue());
                    // success?
                    if (p.exitValue() == 0)
                        return Filer.refactorExtension(javaFiles, "");
                } catch (IOException e) {
                    Logger.error("Kompilierung durch Befehle fehlgeschlagen: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            Logger.error("Kompilierung fehlgeschlagen: " + e.getMessage());
        } catch (NullPointerException e) {
            Logger.error("Während der Kompilierung ist eine NULL aufgetreten");
        }
        return new File[0];
    }

    public static String deleteJavaFile(String... filePaths) {
        StringBuilder s = new StringBuilder("Java-Dateien gelöscht, bis auf: ");
        for (int i = 0; i < filePaths.length; i++) {
            String fn = new File(filePaths[i]).getName();
            File f = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + JAVA_EXTENSION);
            if (!f.delete())
                s.append(i);
        }
        return s.toString();
    }

    public static String run(String mainFilePath) {
        String s = mainFilePath.substring(0, mainFilePath.length() - 5) + "class";
        String l = "Starte Ausführung mit Argumenten: \"" + s + "\"\n";
        Runner runner = Runner.newInstance();
        if (runner == null)
            return l + "Ausführung nicht möglich, Betriebssystem nicht unterstützt (" + OS.getOsName() + ")";
        return (l + "Rückgabe der Ausführung: " + runner.start(new String[]{s}));
    }

}
