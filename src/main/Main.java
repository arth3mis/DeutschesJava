package main;

import compile.Compiler;
import filesystem.Filer;
import run.Runner;

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

    public static final String WILDCARD = "*"; // todo maybe "--alle" Flag to include subfolder recursive search?

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
    private static String compilerPath = "";
    private static String runnerPath = "";

    public static final String pathSaveFileName = "pfade.txt";
    public static final String compilerSave = "K=";
    public static final String runnerSave = "R=";


    public static void main(String[] args) {
        loadCustomPaths();

        String[][] eval = evaluateArgs(args);

        Logger.log(Arrays.toString(args));
        if (args != null)return;

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

            // turn djava file names into files, check if the files exist
            File[] djavaFiles = Arrays.stream(eval[0])
                    .map(File::new)
                    .filter(File::isFile)
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

            // delete java files? (only when conversion and compilation happened)
            if (convert && compile && !Flag.KEEP_JAVA.set) {
                Logger.log("Java-Dateien löschen...");
                if (!Filer.deleteFiles(javaFiles))
                    Logger.warning("Nicht alle Java-Dateien konnten gelöscht werden.");
                else
                    Logger.log("Alle Java-Dateien gelöscht.");
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
                    run(classFiles[0], eval[1]);
                }
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
                compilerPath = content.substring(index + compilerSave.length(), content.indexOf("\n", index))
                        .replace("\"", "");
            if ((index = content.indexOf(runnerSave)) >= 0)
                runnerPath = content.substring(index + runnerSave.length(), content.indexOf("\n", index))
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
                compilerSave + "\"" + compilerPath + "\"\n" +
                runnerSave + "\"" + runnerPath + "\"\n";
        try {
            FileWriter w = new FileWriter(saveFile);
            w.write(content);
            w.close();
        } catch (IOException ignored) {
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

        // replace wildcard with all djava files in current directory (user.dir) todo not working with '*'
        if (fileNames.contains(WILDCARD)) {
            Logger.log("Suche alle %s-Dateien im aktuellen Ordner...", LANGUAGE_NAME);
            // remove wildcard from list
            while (fileNames.remove(WILDCARD));

            // find all djava files present in working directory
            File workingDir = new File(System.getProperty("user.dir"));
            String[] newDjavaFiles = workingDir.list(
                    (dir, name) -> name.lastIndexOf('.') > 0 &&
                           name.substring(name.lastIndexOf('.') + 1).equalsIgnoreCase(EXTENSION_NAME));
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
        // path not empty?
        if (!path.isEmpty()) {
            // test existence of file
            File f = new File(path);
            if (!f.isFile() || !f.isAbsolute()) {
                Logger.error("Ungültiger Pfad.");
                return;
            }
        }
        switch (type) {
            case 0 -> compilerPath = path;
            case 1 -> runnerPath = path;
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
        Compiler compiler = new Compiler(compilerPath);
        return compiler.start(javaFiles);
    }

    private static void run(File mainClassFile, String[] args) {
        Runner runner = Runner.newInstance(runnerPath);
        // not supported?
        if (runner == null) {
            Logger.error("Rennen wird auf diesem Betriebssystem nicht unterstützt.");
        } else {
            runner.start(mainClassFile, args);
        }
    }
}
