package main;

import compile.Compiler;
import filesystem.Filer;
import run.Runner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {

    public static final int VERSION = 22;

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
        JUST_COMPILE("K", "nurkompilieren"),
        JUST_RUN("R", "nurrennen"),
        SPECIAL_RUN("s", "spezialrennen"),
        IGNORE_EXT("x", "unendung"),
        INCLUDE_ALL("", "alle"),
        KEEP_JAVA("j", "behaltejava"),
        DELETE_CLASS("l", "löschklassen"),
        ARGS("a", ""),
        SETTINGS("e", ""),
        TEST("t", ""),// debug only
        ;

        public static final String shortFlag = "-";
        public static final String longFlag = "--";

        public static final int shortArgLength = 1;
        public static final int longArgMaxLength =
                Arrays.stream(Flag.values()).map(f -> f.L).max(Comparator.comparingInt(String::length)).get().length();

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
    private static String customCompiler = "";
    private static String customRunner = "";

    public static final String pathSaveFileName = "pfade.txt";
    public static final String compilerSave = "K=";
    public static final String runnerSave = "R=";

    private static File[] djavaFiles;
    private static String[] runArgs;

    public static final String OUTPUT_SEP = "_".repeat(70);


    public static void main(String[] args) {
        loadCustomPaths();

        short eval = evaluateArgs(args);

        // display info in verbose mode
        Logger.log("%s", OUTPUT_SEP);
        Logger.log("%s (.%s) Version %d", LANGUAGE_NAME, EXTENSION_NAME, VERSION);
        Logger.log("Einstellungs-Datei: %s", new File(Filer.getAppConfigFolder(), pathSaveFileName).toString());
        Logger.log("%s\n", OUTPUT_SEP);

        // display help dialog?
        if (eval == 1) {
            Logger.logHelp(true);
            return;
        }
        // settings?
        else if (eval == 2) {
            startSettings();
            return;
        }

        // standard call
        //

        // help?
        if (Flag.HELP.set) {
            Logger.logHelp(false);
            // user entered more arguments?
            if (args.length > 1)
                Logger.warning("Nach der Hilfe werden keine weiteren Aktionen ausgeführt!");
            // exit here
            return;
        }

        // File array that are used to pass data to subsequent actions (or signal errors)
        File[] javaFiles = null;   // java extension
        File[] classFiles = null;  // no extension

        // standard operation (run)?
        boolean standard =
                !(Flag.CONVERT.set || Flag.COMPILE.set || Flag.RUN.set || Flag.JUST_COMPILE.set || Flag.JUST_RUN.set);
        // resolve flags to actions
        boolean convert   = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.CONVERT.set;
        boolean compile   = standard || Flag.RUN.set || Flag.COMPILE.set || Flag.JUST_COMPILE.set;
        boolean run       = standard || Flag.RUN.set || Flag.JUST_RUN.set;

        // actions
        //

        // no input files
        if (djavaFiles.length == 0) {
            Logger.log("Aktionen werden übersprungen.");
            return;
        }

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

        // delete java files? (only when conversion and compilation were successful)
        if (!Flag.KEEP_JAVA.set
                && convert && javaFiles.length > 0
                && compile && classFiles.length > 0) {
            Logger.log("Java-Dateien löschen...");
            if (!Filer.deleteFiles(javaFiles))
                Logger.warning("Nicht alle Java-Dateien konnten gelöscht werden.");
            else
                Logger.log("Alle Java-Dateien gelöscht.");
        }

        // run class with set java binary/exe, alternatively try global java command
        boolean runSuccess = false;
        if (run) {
            // no compilation happened?
            if (classFiles == null)
                classFiles = Filer.refactorExtension(javaFiles != null ? javaFiles : djavaFiles, "");
            // don't run if conversion/compilation failed
            if (classFiles.length == 0 || !mainFileIntact)
                Logger.warning("Rennen wird übersprungen.");
            else {
                runSuccess = run(classFiles[0], runArgs);
            }
        }

        // delete class files? (only when compilation and running were successful)
        if (Flag.DELETE_CLASS.set
                && compile && run && runSuccess) {
            // do not allow deletion for windows batch runs, because class files are needed asynchronous
            if (Flag.SPECIAL_RUN.set && OS.isWindows())
                Logger.warning("Klassen-Dateien werden aufgrund der Option '%s' nicht gelöscht",
                        Logger.fFlag(Flag.SPECIAL_RUN, "|"));
            else {
                Logger.log("Klassen-Dateien löschen...");
                if (!Filer.deleteFiles(Filer.refactorExtension(classFiles, "class")))
                    Logger.warning("Nicht alle Klassen-Dateien konnten gelöscht werden.");
                else
                    Logger.log("Alle Klassen-Dateien gelöscht.");
            }
        }
    }

    private static void loadCustomPaths() {
        File saveFile = new File(Filer.getAppConfigFolder(), pathSaveFileName);
        if (!saveFile.exists())
            return;
        // extract paths
        try {
            String content = Files.readString(saveFile.toPath());
            int index;
            if ((index = content.indexOf(compilerSave)) >= 0)
                customCompiler = content.substring(index + compilerSave.length(), content.indexOf("\n", index))
                        .replace("\"", "");
            if ((index = content.indexOf(runnerSave)) >= 0)
                customRunner = content.substring(index + runnerSave.length(), content.indexOf("\n", index))
                        .replace("\"", "");
        } catch (IOException ignored) {
        }
    }

    private static void saveCustomPaths() {
        File saveFile = new File(Filer.getAppConfigFolder(), "pfade.txt");
        if (!saveFile.exists()) {
            try {
                if (!saveFile.getParentFile().mkdirs() && !saveFile.delete() && !saveFile.createNewFile())
                    throw new IOException("Datei/Elternordner konnten nicht gelöscht/neu erstellt werden.");
            } catch (IOException e) {
                Logger.error("Einstellungen können nicht gespeichert werden: %s", e.getMessage());
            }
        }
        // save paths
        String content =
                compilerSave + "\"" + customCompiler + "\"\n" +
                runnerSave + "\"" + customRunner + "\"\n";
        try {
            FileWriter w = new FileWriter(saveFile);
            w.write(content);
            w.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * @param args program launch arguments
     * @return 0: standard; 1: help dialog; 2: settings
     */
    private static short evaluateArgs(String[] args) {
        // empty args? help dialog
        if (args.length == 0)
            return 1;

        // check for flags, files and run arguments
        List<String> flagListS = Arrays.stream(Flag.values()).map(f -> Flag.shortFlag + f.S).toList();
        List<String> flagListL = Arrays.stream(Flag.values()).map(f -> Flag.longFlag + f.L).toList();
        List<String> userFileNames = new ArrayList<>();
        int runArgsPos = -1;

        int k = 0;
        for (String s : args) {
            Flag f = null;
            if (flagListS.contains(s)) {
                (f = Flag.values()[flagListS.indexOf(s)]).set = true;
            } else if (flagListL.contains(s)) {
                (f = Flag.values()[flagListL.indexOf(s)]).set = true;
            } else if (runArgsPos == -1) {  // don't add run arguments
                userFileNames.add(s);
            }
            // save position of '-a' for run argument extraction
            if (f == Flag.ARGS)
                runArgsPos = k;
            k++;
        }
        Logger.log("%d Argumente wurden ausgelesen.", args.length);

        // settings?
        if (Flag.SETTINGS.set)
            return 2;

        // warnings for flags/flag combos
        if (Flag.SPECIAL_RUN.set && !OS.isWindows())
            Logger.warning("Die Option '%s' ist für %s nicht verfügbar.",
                    Logger.fFlag(Flag.SPECIAL_RUN, "|"), OS.getOsName());

        // evaluate djava files
        //
        // create files from user-given file names, remove non-existing, make them absolute
        // (relative paths also work, but absolute is now standard for comparison reasons)
        List<File> files = userFileNames.stream()
                .map(File::new)
                .filter(File::isFile)
                .map(File::getAbsoluteFile)
                .toList();

        // remove duplicates (and make mutable list at the same time)
        // absolute paths are necessary here, because distinct() uses equals,
        // which compares file paths lexicographically
        files = new ArrayList<>(files.stream().distinct().toList());

        // find all files in directory tree? (non-djava files are handled afterwards)
        if (Flag.INCLUDE_ALL.set) {
            Logger.log("Suche alle %s-Dateien in Ordner und Unterordnern...", LANGUAGE_NAME);

            try {
                // walk tree, create file, make absolute (already is, but be certain), exclude directories
                List<File> includeFiles = Files.walk(Filer.getCurrentDir().toPath())
                        .map(Path::toFile)
                        .map(File::getAbsoluteFile)
                        .filter(File::isFile)
                        .toList();
                includeFiles = new ArrayList<>(includeFiles);  // make file list mutable
                // check duplicates with already present files
                List<String> names = files.stream().map(File::getAbsolutePath).toList();
                includeFiles.removeIf(file -> names.contains(file.getPath()));

                files.addAll(includeFiles);
                Logger.log("%d Dateien hinzugefügt.", includeFiles.size());
            } catch (IOException | IllegalArgumentException e) {
                Logger.error("Fehler beim rekursiven Suchen nach Dateien: %s", e.getMessage());
            }
        }

        // remove non-djava files?
        if (!Flag.IGNORE_EXT.set) {
            int oldSize = files.size();
            if (files.removeIf(file -> !Filer.checkExtension(file, EXTENSION_NAME)))
                Logger.log("%d Dateien entfernt, die nicht auf '.%s' enden.",
                        oldSize - files.size(), EXTENSION_NAME);
        }

        // put files in global field
        djavaFiles = files.toArray(new File[0]);

        //  put run arguments in global field; they must come after -a
        if (Flag.ARGS.set && runArgsPos > -1) {
            runArgs = Arrays.copyOfRange(args, runArgsPos + 1, args.length);
        } else
            runArgs = new String[0];

        // skip file evaluation if help is set
        if (Flag.HELP.set)
            return 0;

        // no valid files found?
        if (djavaFiles.length < 1) {
            Logger.error("Keine validen %s-Dateien gefunden.", LANGUAGE_NAME);
        } else {
            // first valid file is not the first user-given one?
            if (!userFileNames.isEmpty()
                    && !djavaFiles[0].getAbsolutePath().equals(new File(userFileNames.get(0)).getAbsolutePath())) {
                Logger.warning("Hauptklasse: %s", Filer.getCurrentDir().toPath()
                        .relativize(djavaFiles[0].toPath())
                        .toString());
            }
            Logger.log("%d Dateien gefunden.", djavaFiles.length);
        }

        return 0;
    }

    private static void startSettings() {
        enum Choices { k,K, r,R, x,X, wrong }
        String choice = "";
        while (!choice.equalsIgnoreCase("x")) {
            choice = Logger.request("""
                    
                    
                    -- Einstellungen --
                    [k] Kompilierer-Befehl (javac) setzen (aktuell: %s)
                    [r] Renner-Befehl (java) setzen (aktuell: %s)
                    [x] Beenden
                    
                    Auswahl""",
                    customCompiler.isEmpty() ? "-" : customCompiler,
                    customRunner.isEmpty() ? "-" : customRunner);
            Choices ch;
            try {
                ch = Choices.valueOf(choice);
            } catch (IllegalArgumentException e) {
                ch = Choices.wrong;
            }
            switch (ch) {
                case k, K -> setCommand(0, "Kompilierer");
                case r, R -> setCommand(1, "Renner");
                case x, X -> Logger.log("Einstellungen beendet.");
                default -> Logger.error("Falsche Eingabe!");
            }
        }
    }

    private static void setCommand(int type, String name) {
        String s = Logger.request("Gib einen Befehl für den %s an (ausführbare Datei, Umgebungsvariable, ...)", name)
                .replace("\"", "");
        switch (type) {
            case 0 -> customCompiler = s;
            case 1 -> customRunner = s;
        }
        saveCustomPaths();
    }

    private static File[] interpret(File[] djavaFiles) {
        //Converter c = new Converter(djavaFiles);
        //c.translateToJavaFiles();
        //return c.getFiles();

        // todo temp (and very scuffed god damn)
        List<File> jf = new ArrayList<>(List.of(Filer.refactorExtension(djavaFiles, JAVA_EXTENSION)));
        List<File> unJf = new ArrayList<>();
        for (int i = 0; i < djavaFiles.length; i++) {
            File f = jf.get(i);
            try {
                if (!f.delete() && !f.createNewFile()) throw new IOException();
                FileWriter w = new FileWriter(f);
                w.write(Files.readString(djavaFiles[i].toPath()));
                w.close();
            } catch (IOException e) {
                unJf.add(f);
            }
        }
        for (File r : unJf)
            jf.remove(r);
        return jf.toArray(new File[0]);
    }

    private static boolean compile(File[] javaFiles) {
        Compiler compiler = new Compiler(customCompiler);
        return compiler.start(javaFiles);
    }

    private static boolean run(File mainClassFile, String[] args) {
        Runner runner = Runner.newInstance(mainClassFile, args, customRunner);
        if (runner == null) {
            Logger.error("Rennen wird auf diesem Betriebssystem nicht unterstützt.");
            return false;
        }
        return runner.start();
    }
}
