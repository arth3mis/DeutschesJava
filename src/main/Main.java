package main;

import compile.Compiler;
import convert.Converter;
import convert.sublime.SublimeFolder;
import convert.translation.TranslationFolder;
import filesystem.Filer;
import run.Runner;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {

    public static final int VERSION = 31;
    // todo "search djava for english words and list them" feature (-p --prüfen maybe?)

    public static final int YEAR = 2022;
    public static final String linkGitHub = "https://github.com/arth3mis/DeutschesJava";
    public static final String thisFileOnGitHub = "https://github.com/arth3mis/DeutschesJava/blob/master/src/main/Main.java";

    public static final String LANGUAGE_NAME = "DJava";
    public static final String EXTENSION_NAME = "djava";
    public static final String JAVA_EXTENSION = "java";

    public static final String WILDCARD = "*";

    // program flags
    public enum Flag {
        HELP("h", "hilfe"),
        VERBOSE("v", ""),
        CONVERT("u", "umwandeln"),
        COMPILE("k", "kompilieren"),
        RUN("r", "rennen"),
        JUST_COMPILE("K", "nurkompilieren"),
        JUST_RUN("R", "nurrennen"),
        SPECIAL_RUN("z", "spezialrennen"),
        IGNORE_EXT("x", "endungx"),
        INCLUDE_ALL("", "alle"),
        KEEP_JAVA("j", "behaltejava"),
        DELETE_CLASS("l", "löschklassen"),
        ARGS("a", ""),
        SETTINGS("e", ""),
        GENERATE_SYNTAX("g", ""),
        SEARCH_TRANSLATION("s", "suche"),
        TEST("t", ""),// debug only
        ;

        public static final String shortFlag = "-";
        public static final String longFlag = "--";

        public static final int shortArgLength = 1;
        public static final int longArgMaxLength =
                Arrays.stream(Flag.values())
                        .map(f -> f.L)
                        .max(Comparator.comparingInt(String::length))
                        .orElseThrow()
                        .length();

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
    public static boolean disableCheckVersions = false;
    public static long lastVersionCheckTime = 0;

    public static final String pathSaveFileName = "pfade.txt";
    public static final String compilerSave = "K=";
    public static final String runnerSave = "R=";
    public static final String versionsSaveFileName = "versionen.txt";
    public static final String disableCheckSave = "X=";
    public static final String lastTimeSave = "T=";

    private static File[] djavaFiles;
    private static String[] runArgs;

    public static final String OUTPUT_SEP = "_".repeat(70);


    public static void main(String[] args) {
        assert Arrays.stream(Flag.values()).mapToInt(f -> f.S.length()).max().orElse(0) == Flag.shortArgLength;

        Logger.debug(Charset.defaultCharset().name());

        loadCustomPaths();

        checkForNewVersion();

        short eval = evaluateArgs(args);

        // display info in verbose mode
        Logger.log("%s", OUTPUT_SEP);
        Logger.log("%s (.%s) Version %d", LANGUAGE_NAME, EXTENSION_NAME, VERSION);
        Logger.log("Einstellungs-Ordner: %s", Filer.getDJavaConfigFolder());
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
        // search translation?
        else if (eval == 3) {
            TranslationFolder.main(runArgs);
            return;
        }
        // generate syntax file?
        else if (eval == 4) {
            SublimeFolder.generateSublime();
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
        File[] classFiles = null;  // no extension (.class in file names, none here as they are not needed in java command call)

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

        // delete class files? (only when compilation and running (or JUST_RUN running) were successful)
        if (Flag.DELETE_CLASS.set
                && (compile && run && runSuccess || Flag.JUST_RUN.set && runSuccess)) {
            // do not allow deletion for windows batch runs, because class files are needed asynchronous
            if (Flag.SPECIAL_RUN.set && OS.isWindows()) {
                Logger.warning("Klassen-Dateien werden aufgrund der Option '%s' nicht gelöscht",
                        Logger.fFlag(Flag.SPECIAL_RUN, "|"));
            } else {
                Logger.log("Klassen-Dateien löschen...");
                if (!Filer.deleteFiles(Filer.refactorExtension(classFiles, "class")) ||
                        !Filer.deleteInnerClassFiles(Filer.refactorExtension(classFiles, "class")))
                    Logger.warning("Nicht alle Klassen-Dateien konnten gelöscht werden.");
                else
                    Logger.log("Alle Klassen-Dateien gelöscht.");
            }
        }
    }

    private static void checkForNewVersion() {
        // 24h=86_400_000; 6h=21_600_000;
        final long CHECK_INTERVAL_MS = 21_600_000;
        long currentCheck = System.currentTimeMillis();

        // check after passed time interval
        if (currentCheck - lastVersionCheckTime > CHECK_INTERVAL_MS) {
            // save check time
            lastVersionCheckTime = currentCheck;
            saveCustomPaths();

            // suppressed checks?
            if (disableCheckVersions) {
                Logger.info("Zum Aktivieren der automatischen Aktualisierungs-Prüfungen: nutze Option '%s'\n", Logger.fFlag(Flag.SETTINGS, "|"));
                return;
            }

            // try to reach GitHub
            try {
                URL url = new URL(thisFileOnGitHub);

                // get page content
                HttpURLConnection huc = (HttpURLConnection) url.openConnection();
                HttpURLConnection.setFollowRedirects(false);
                huc.setConnectTimeout(500);
                huc.setRequestMethod("GET");
                huc.connect();

                BufferedReader br = new BufferedReader(new InputStreamReader(huc.getInputStream()));
                String result = br.lines().collect(Collectors.joining("\n"));

                Pattern target = Pattern.compile("VERSION.+>(\\d+)<.+;");
                Matcher m = target.matcher(result);
                if (m.find()) {
                    int newestVersion = Integer.parseInt(m.group(1));

                    if (newestVersion > VERSION) {
                        Logger.info("Eine neue Version (%d) des Programms ist verfügbar!", newestVersion);
                        Logger.info("Hier herunterladen: %s", linkGitHub);
                        Logger.info("Zum Deaktivieren dieser Nachricht: nutze Option '%s'", Logger.fFlag(Flag.SETTINGS, "|"));
                        Logger.info("%s\n", OUTPUT_SEP);
                    }
                }

                // alt 1
//                Scanner sc = new Scanner(url.openStream());
//                StringBuilder sb = new StringBuilder();
//                while (sc.hasNext()) {
//                    sb.append(sc.next());
//                }
//                String result = sb.toString();

                // alt 2
//                URLConnection conn = url.openConnection();
//                InputStream in = conn.getInputStream();
//                String encoding = conn.getContentEncoding();  // ** WRONG: should use "con.getContentType()" instead but it returns something like "text/html; charset=UTF-8" so this value must be parsed to extract the actual encoding
//                encoding = encoding == null ? "UTF-8" : encoding;
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                byte[] buf = new byte[8192];
//                int len = 0;
//                while ((len = in.read(buf)) != -1) {
//                    baos.write(buf, 0, len);
//                }
//                String body = baos.toString(encoding);
            } catch (IOException | NumberFormatException ignored) {
            }
        }
    }

    private static void loadCustomPaths() {
        File saveFolder = Filer.getDJavaConfigFolder();

        File saveFile = new File(saveFolder, pathSaveFileName);
        if (saveFile.exists()) {
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

        saveFile = new File(saveFolder, versionsSaveFileName);
        if (saveFile.exists()) {
            // extract paths
            try {
                String content = Files.readString(saveFile.toPath());
                int index;
                if ((index = content.indexOf(disableCheckSave)) >= 0)
                    disableCheckVersions = Boolean.parseBoolean(content.substring(index + disableCheckSave.length(), content.indexOf("\n", index)));
                if ((index = content.indexOf(lastTimeSave)) >= 0)
                    lastVersionCheckTime = Long.parseLong(content.substring(index + lastTimeSave.length(), content.indexOf("\n", index)));
            } catch (IOException ignored) {
            }
        }
    }

    private static void saveCustomPaths() {
        File saveFolder = Filer.getDJavaConfigFolder();
        if (!saveFolder.isDirectory() && !saveFolder.mkdirs()) {
            Logger.error("Einstellungs-Ordner kann nicht erstellt werden.");
            return;
        }

        Map<File, String> filesAndContents = Map.of(
                new File(saveFolder, pathSaveFileName),
                compilerSave + "\"" + customCompiler + "\"\n" + runnerSave + "\"" + customRunner + "\"\n",

                new File(saveFolder, versionsSaveFileName),
                disableCheckSave + disableCheckVersions + "\n" + lastTimeSave + lastVersionCheckTime + "\n"
        );

        for (var fc : filesAndContents.entrySet()) {
            try {
                if (fc.getKey().isFile() && !fc.getKey().delete() || !fc.getKey().createNewFile())
                    throw new IOException("Datei konnte nicht neu erstellt werden.");
                FileWriter w = new FileWriter(fc.getKey());
                w.write(fc.getValue());
                w.close();
            } catch (IOException e) {
                Logger.error("Einstellungen können nicht gespeichert werden: %s", e.getMessage());
            }
        }
    }

    /**
     * @param args program launch arguments
     * @return flag value based on user choice <ul>
     *      <li> 0: standard
     *      <li> 1: help dialog
     *      <li> 2: settings
     *      <li> 3: search translations
     *      <li> 4: generate syntax
     *      </ul>
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

        // allow multiple short args after one short flag
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        int counter = 0;
        while (counter < argList.size()) {
            String s = argList.get(counter);
            if (s.startsWith(Flag.shortFlag) && !s.startsWith(Flag.longFlag)) {
                argList.remove(counter);
                for (int i = Flag.shortFlag.length(); i < s.length(); i += Flag.shortArgLength) {
                    argList.add(counter++, Flag.shortFlag + s.charAt(i));
                }
                counter--;
            }
            counter++;
        }
        args = argList.toArray(new String[0]);

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
            // save position of '-a|-s' for run/search argument extraction
            if (f == Flag.ARGS || f == Flag.SEARCH_TRANSLATION)
                runArgsPos = k;
            k++;
        }
        Logger.log("%d Argumente wurden ausgelesen.", args.length);

        // settings?
        if (Flag.SETTINGS.set)
            return 2;

        // search translations?
        if (Flag.SEARCH_TRANSLATION.set) {
            String combine = String.join("", Arrays.copyOfRange(args, runArgsPos + 1, args.length));
            runArgs = new String[]{
                    combine.substring(combine.startsWith("?") ? 1 : 0),
                    Boolean.toString(combine.startsWith("?"))};
            return 3;
        }

        // generate syntax file?
        if (Flag.GENERATE_SYNTAX.set)
            return 4;

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
                .map(File::getAbsoluteFile)
                .toList();

        // assume .djava for files without extension
        // (if all extensions are allowed, assumption is kind of defeating the purpose)
        if (!Flag.IGNORE_EXT.set) {
            files = files.stream()
                    .map(file -> {
                        if (Filer.checkExtension(file, "")) {
                            File f = Filer.refactorExtension(file, EXTENSION_NAME);
                            if (f.isFile())
                                return f;
                        }
                        return file;
                    })
                    .toList();
        }

        // remove duplicates (and make mutable list at the same time)
        // absolute paths are necessary here, because distinct() uses equals,
        // which compares file paths lexicographically
        files = new ArrayList<>(files.stream()
                .filter(File::isFile)
                .distinct()
                .toList());

        // find all files in directory tree? (non-djava files are handled afterwards)
        if (Flag.INCLUDE_ALL.set) {
            Logger.log("Suche alle %s-Dateien in Ordner und Unterordnern...", LANGUAGE_NAME);

            try (var fw = Files.walk(Filer.getCurrentDir().toPath())) {
                // walk tree, create file, make absolute (already is, but be certain), exclude directories
                List<File> includeFiles = fw
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
                Logger.warning("%d Dateien entfernt, die nicht auf '.%s' enden.",
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
                Logger.log("Hauptdatei: %s", Filer.getCurrentDir().toPath()
                        .relativize(djavaFiles[0].toPath())
                        .toString());
            }
            Logger.log("%d Dateien gefunden.", djavaFiles.length);
        }

        return 0;
    }

    private static void startSettings() {
        enum Choices { k,K, r,R, v,V, x,X, wrong }
        String choice = "";
        // print save path once
        Logger.info("Speicherpfad: %s", Filer.getDJavaConfigFolder().getAbsolutePath());
        // IO loop
        while (!choice.equalsIgnoreCase("x")) {
            choice = Logger.request("""
                    
                    
                    -- Einstellungen --
                    [k] Kompilierer-Befehl (javac) setzen (aktuell: %s)
                    [r] Renner-Befehl (java) setzen (aktuell: %s)
                    [v] Regelmäßiges Überprüfen auf neuere Version: %s
                    [x] Beenden
                    
                    Auswahl""",
                    customCompiler.isEmpty() ? "-" : customCompiler,
                    customRunner.isEmpty() ? "-" : customRunner,
                    disableCheckVersions ? "Nein" : "Ja"
            );
            Choices ch;
            try {
                ch = Choices.valueOf(choice);
            } catch (IllegalArgumentException e) {
                ch = Choices.wrong;
            }
            switch (ch) {
                case k, K -> setCommand(0, "Kompilierer");
                case r, R -> setCommand(1, "Renner");
                case v, V -> {
                    disableCheckVersions = !disableCheckVersions;
                    saveCustomPaths();
                }
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
        Converter c = new Converter(djavaFiles);
        c.translateToJavaFiles();
        return c.getFiles();
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
