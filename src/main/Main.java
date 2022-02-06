package main;

import compile.Compiler;
import convert.Converter;
import filesystem.Filer;
import run.Runner;

import javax.tools.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
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
    }

    public static boolean mainFileIntact = true;
    static String compilerPath = "";
    static String runnerPath = "";

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
            File[] djavaFiles = Arrays.stream(eval[0])
                    .map(File::new)
                    .filter(File::exists)
                    .toList().toArray(new File[0]);

            if (djavaFiles.length < 1) {
                Logger.error("Keine validen %s-Dateien gefunden.", LANGUAGE_NAME);
                return;
            } else {
                if (!djavaFiles[0].getPath().equals(eval[0][0]))
                    mainFileIntact = false;
                if (djavaFiles.length != eval[0].length)
                    Logger.warning("%d/%d %s-Dateien gefunden.", djavaFiles.length, eval[0].length, LANGUAGE_NAME);
                else
                    Logger.log("%d/%d %s-Dateien gefunden.", djavaFiles.length, eval[0].length, LANGUAGE_NAME);
            }

            // File array that are used to pass data to subsequent actions (or signal errors)
            File[] javaFiles = null;
            File[] classFiles = null;

            // standard operation (run)?
            boolean standard = !(Flag.CONVERT.set || Flag.COMPILE.set || Flag.RUN.set || Flag.JUST_COMPILE.set || Flag.JUST_RUN.set);
            // resolve flags to actions
            boolean convert = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.CONVERT.set;
            boolean compile   = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.JUST_COMPILE.set;
            boolean run       = standard || Flag.RUN.set || Flag.JUST_RUN.set;

            // actions
            //

            // convert djava to java
            if (convert) {
                javaFiles = interpret(djavaFiles);
            }

            // compile java with set javac binary/exe, alternatively try system compiler
            if (compile) {
                // no conversion happened?
                if (javaFiles == null)
                    javaFiles = Filer.refactorExtension(djavaFiles, JAVA_EXTENSION);
                // don't compile if converting failed
                if (javaFiles.length == 0)
                    Logger.warning("Kompilierung wird übersprungen.");
                else {
                    // successful compilation?
                    if (compile(javaFiles))
                        classFiles = Filer.refactorExtension(javaFiles, "");
                    else
                        classFiles = new File[0];
                }
            }

            // delete java files? (only when conversion happened)
            if (convert && !Flag.KEEP_JAVA.set) {
                //Filer.deleteFiles(javaFiles); todo
            }

            // run class with set java binary/exe, alternatively try global java command
            if (run) {
                // no compilation happened?
                if (classFiles == null)
                    classFiles = Filer.refactorExtension(javaFiles != null ? javaFiles : djavaFiles, "");
                // don't run if conversion/compilation failed
                if (classFiles.length == 0 || !mainFileIntact)
                    Logger.warning("Rennen wird übersprungen.");
                else {
                    run(classFiles[0]);
                }
            }

        }
    }

    private static void loadCustomPaths() {
        // todo
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
        enum Choices { k,K, r,R, x,X, wrong }
        String choice = "";
        while (!choice.equalsIgnoreCase("x")) {
            choice = Logger.request("""
                    
                    -- Einstellungen --
                    [k] Kompilierer-Pfad (javac) setzen (aktuell: %s)
                    [r] Renner-Pfad (java) setzen (aktuell: %s)
                    [x] Beenden
                    
                    Auswahl""",
                    compilerPath.isEmpty() ? "-" : compilerPath,
                    runnerPath.isEmpty() ? "-" : runnerPath);
            Choices ch;
            try {
                ch = Choices.valueOf(choice);
            } catch (IllegalArgumentException e) {
                ch = Choices.wrong;
            }
            switch (ch) {
                case k, K -> setPath(0, "Kompilierer");
                case r, R -> setPath(1, "Renner");
                case x, X -> Logger.log("Einstellungen beendet.");
                default -> Logger.error("Falsche Eingabe!");
            }
        }
    }

    private static void setPath(int type, String pathName) {
        String path = Logger.request("Gib einen (absoluten) Pfad für die %s-Datei an (keinen Ordnerpfad)", pathName)
                .replace("\"", "");
        // no input?
        if (path.isEmpty())
            return;
        // test existence of file
        File f = new File(path);
        if (f.isFile() && f.isAbsolute()) {
            switch (type) {
                case 0 -> compilerPath = path;
                case 1 -> runnerPath = path;
            }
        } else
            Logger.error("Ungültiger Pfad.");
    }

    private static File[] interpret(File[] djavaFiles) {
        //Converter c = new Converter(djavaFiles);
        //c.translateToJavaFiles();
        //return c.getFiles();

        // todo temp
        List<File> jf = new ArrayList<>(List.of(Filer.refactorExtension(djavaFiles, JAVA_EXTENSION)));
        for (File f : jf) {
            try {
                f.delete();
                if (f.createNewFile()) throw new IOException();
                FileWriter w = new FileWriter(f);
                w.write(Files.readString(f.toPath()));
            } catch (IOException e) {
                jf.remove(f);
            }
        }
        return jf.toArray(new File[0]);
    }

    private static boolean compile(File[] javaFiles) {
        Compiler compiler = Compiler.newInstance(compilerPath);
        // not supported?
        if (compiler == null) {
            Logger.error("Kompilieren wird auf diesem Betriebssystem nicht unterstützt.");
            return false;
        }
        return compiler.start(javaFiles);
    }

    private static void run(File mainClassFile) {
        Runner runner = Runner.newInstance(runnerPath);
        // not supported?
        if (runner == null) {
            Logger.error("Rennen wird auf diesem Betriebssystem nicht unterstützt.");
        } else {
            runner.start(mainClassFile);
        }
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
    public static File[] compile(File[] javaFiles, boolean deprecated) {
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

    /*public static String run(File mainClassFile) {
        Runner runner = Runner.newInstance(runnerPath);
        String l = "";
        if (runner == null)
            return l + "Ausführung nicht möglich, Betriebssystem nicht unterstützt (" + OS.getOsName() + ")";
        return null;// (l + "Rückgabe der Ausführung: " + runner.start(new String[]{s}));
    }*/

}
