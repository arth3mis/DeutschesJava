package convert;

import filesystem.Filer;
import main.Logger;
import main.Main;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

public class Converter {

    /**
     * TEST METHOD for Converter
     */
    public static void main(String[] args) {
        File[] djavaFiles = Arrays.stream(args)
                .map(File::new)
                .filter(File::exists)
                .toList().toArray(new File[0]);

        Converter c = new Converter(djavaFiles);
        c.translateToJavaFiles();
        File[] javaFiles = c.getFiles();

        System.out.println(Arrays.toString(javaFiles));
    }

    private static final String TRANSLATION_DIR = "translation";
    private static final String[] TRANSLATION_FILES = {
            "0_java.lang",
            "0_main_translation",
            "java",
            "java.util.function",
            "java.util",
            "javax.swing",
            "javax",
    };
    private static final String TRANSLATION_EXT = ".txt";

    private Translation rootTranslation = new Translation();
    private Translation packageContext;
    private final File[] files;
    private HashMap<File, TextChain> fileChains = new HashMap<>();

    // old version
    private HashMap<String, String> oldTranslationHashMap;


    public Converter(File[] djavaFiles) {
        files = djavaFiles;

        // Loop through every dJava File and create the chains
        Logger.log("Dateien werden in Sinneinheiten unterteilt...");
        for (File dJavaFile : djavaFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(dJavaFile));
                TextChain tc = new TextChain.Generator(br).generate();
                // Importing TextChain
                fileChains.put(dJavaFile, tc);

                fileChains.get(dJavaFile).print();
                Logger.debug("\n\n");
            } catch (IOException e) {
                Logger.error("Fehler beim Lesen der Djava-Datei: %s", dJavaFile.getAbsolutePath());
                // main file lost?
                if (fileChains.isEmpty())
                    Main.mainFileIntact = false;
            }
        }

        //      Name  , isLoaded
        HashMap<String, Boolean> packageTranslationFiles = new HashMap<>();
        packageContext = rootTranslation;
        // Import import files
        // Note (Arthur): using File objects and dynamic translation file searching doesn't work in jar file
        for (String trFileName : TRANSLATION_FILES) {
            if (trFileName.startsWith("0_")) {
                // Add to Root translation
                loadPackageTranslationFile(trFileName);
            } else {
                // Note the Package Name
                packageTranslationFiles.put(trFileName, false);
            }
        }

        rootTranslation.print("");
        Logger.debug("\n");

        // collect all tokens that are top-level package names
        // 0_main_translation.txt must have been loaded before this!!!
        String[] packages = rootTranslation.getPackageTranslations().keySet().toArray(new String[0]);
        TextChain[] imports = fileChains.values().stream()
                .flatMap(t -> t.findAllInChains(packages).stream())
                .toArray(TextChain[]::new);

        Arrays.stream(imports).forEach(t -> {
            TextChain t1 = t, t2 = t.find(";");  // todo watch out, can contain "unwanted" code
            String sb = "";
            while (t1 != t2) {
                sb += t1.germanWord;
                t1 = t1.nextChain;
            }
            Logger.debug(sb);
        });

        // for each import, load all needed package translations
        // note: an object named "djava" calling a field (e.g. "djava.sub.value = 2;") has the same syntax.
        //       Thus, a warning for "wrong import statements" or similar is not easy to implement
        for (TextChain t : imports) {
            StringBuilder packageName = new StringBuilder();
            Translation currentPackage = rootTranslation;
            // check if it is really an import (not e.g. "djava = 42;")
            if (t.find(new String[]{".",";"}).germanWord.equals(";"))
                continue;
            // check and load sub-packages
            while (t != null) {
                if (t.germanWord.equals(";"))
                    break;
                else if (t.germanWord.equals(".")) {
                    packageName.append(".");
                } else if (!t.isWhitespace()) {
                    Map<String, Translation> pt = currentPackage.getPackageTranslations();
                    // no sub-packages available?
                    if (pt == null)
                        break;
                    // add translation and load package
                    else if (pt.containsKey(t.germanWord)) {
                        currentPackage = pt.get(t.germanWord);
                        t.translate(currentPackage.getTranslationText());
                        packageName.append(t.translation);
                        String s = packageName.toString();
                        // package does not exist? (can mean: error; reached class identifier; was no import at all; ...)
                        if (!packageTranslationFiles.containsKey(s))
                            break;
                        // package file not already loaded?
                        if (!packageTranslationFiles.get(s)) {
                            packageContext = currentPackage;
                            loadPackageTranslationFile(s);
                            packageTranslationFiles.put(s, true);
                        }
                    }
                }
                t = t.nextChain;
            }
        }
        packageContext = rootTranslation;  // no need yet, just for status quo


        // setup imports in all translations
            // check which translation files are needed
            // store in "rootTranslation" object
    }

    private void loadPackageTranslationFile(String trFileName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                getClass().getResourceAsStream(TRANSLATION_DIR + File.separator + trFileName + TRANSLATION_EXT))))) {
            readPackageTranslationFile(br, rootTranslation);
        } catch (IOException | NullPointerException e) {
            Logger.error("Fehler beim Lesen der Übersetzungsdatei: %s", trFileName);
        } /*catch (NullPointerException e) {
            Logger.warning("Fehler beim Lesen der Übersetzungsdatei: %s\n\tDetails: Fehlerhafte Klammersetzung!",
                    trFileName.getAbsolutePath());
        }*/
    }

    /**
     * Recursive Method to read a translation File
     * @param br
     * @param staticContextTranslation
     * @throws IOException
     * @throws NullPointerException
     */
    private void readPackageTranslationFile(BufferedReader br, Translation staticContextTranslation) throws IOException {
        String line;
        Translation lastTranslation = null;

        // Read lines & Return if context goes up a layer
        while ((line = br.readLine()) != null) {
            line = line.replaceAll(" ", "");
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;
            if (line.startsWith("}")) return;
            if (line.startsWith("{")) {
                if (lastTranslation == null) throw new NullPointerException();
                readPackageTranslationFile(br, lastTranslation);
                continue;
            }

            // delete trailing comment
            line = line.substring(0, line.contains("#") ? line.indexOf("#") : line.length());

            HashMap<String, Translation> context;
            // Check if is static and delete $
            if (line.startsWith("$")) {
                context = staticContextTranslation.getStaticTranslations();
                line = line.substring(1);
            } else if (line.startsWith("_")) {
                if (packageContext.getPackageTranslations() == null)
                    packageContext.initPackageTranslations();
                context = packageContext.getPackageTranslations();
                line = line.substring(1);
            } else
                context = rootTranslation.getStaticTranslations();

            // Add translation if in File
            if (line.contains(";")) {
                String[] splitLine = line.split(";");
                Translation value = new Translation(splitLine[0]);

                Arrays.stream(splitLine[1].split(",")).forEach(key -> context.put(key, value));

                lastTranslation = value;

            } else {
                lastTranslation = new Translation(line);
                context.put(line, lastTranslation);
            }
        }
    }


    /**
     * called by Main class to start the translation
     */
    public void translateToJavaFiles() {
        for (int i = 0; i < files.length; i++) {
            // TODO if first file (=main class) fails, set flag: Main.mainFileIntact = false;
            files[i] = Filer.refactorExtension(translateToJavaFile(files[i]), Main.JAVA_EXTENSION);;
        }
    }

    @NotNull private File translateToJavaFile(File djavaFile) {

        // Setup text chain
            // setup sub chains on every ()

        // Translate Text chains and put it into StringBuilder

        return djavaFile;
    }

    /**
     * called by Main class to retrieve java file
     * @return translated java file, null for unsuccessful translation
     */
    public File[] getFiles() {

        return files;
    }













    private String read(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            Logger.error("Djava-Datei konnte nicht gelesen werden: %s", e.getMessage());
            return null;
        }
    }

    private String replace(String djavaCode) {
        StringBuilder code = new StringBuilder(djavaCode);
        
        //TODO:
        // use String instead of StringBuilder? Or some array (or make a class like 'ReplaceCode') that splits code into parts that need replacing and parts that don't (to keep order)
        // String content needs to be excluded

        for (Map.Entry<String, String> entry : oldTranslationHashMap.entrySet()) {
            int ind = code.indexOf(entry.getKey());
            for (; ind != -1; ind = code.indexOf(entry.getKey(), ind + 1)) {
                code.replace(ind, ind + entry.getKey().length(), entry.getValue());
            }
        }

        return code.toString();
    }

    /**
     * @param djavaFiles existing djava files
     * @return successfully converted java files
     */
    public File[] makeJavaFiles(File[] djavaFiles) {
        List<File> successFiles = new ArrayList<>();
        for (File djavaFile : djavaFiles) {
            // get java file (change extension)
            File javaFile = Filer.refactorExtension(djavaFile, Main.JAVA_EXTENSION);
            // create and fill java file with translated code
            try {
                if (javaFile.exists())
                    if (!javaFile.delete())
                        throw new IOException();
                if (!javaFile.createNewFile())
                    throw new IOException();

                // read and translate
                String djavaCode = read(djavaFile);
                String javaCode = replace(djavaCode);

                BufferedWriter bw = new BufferedWriter(new FileWriter(javaFile));
                bw.write(javaCode);
                bw.close();
                successFiles.add(javaFile);
            } catch (IOException e) {
                Logger.error("Java-Datei %s konnte nicht erstellt werden: %s", javaFile, e.getMessage());
                // main file lost?
                if (successFiles.isEmpty())
                    Main.mainFileIntact = false;
            }
        }
        if (successFiles.size() == djavaFiles.length)
            Logger.log("Java-Dateien erfolgreich erstellt.");
        else
            Logger.warning("Java-Dateien erstellt, %d/%d nicht erfolgreich.",
                    djavaFiles.length - successFiles.size(), djavaFiles.length);
        return successFiles.toArray(new File[0]);
    }

    public void loadOldTranslation() {
        oldTranslationHashMap = new HashMap<>();

        //todo:
        // make "sub-translations" for namespaces (marked by 4 space indent in txt files), so that no conflicts happen
        //      idea: class that has direct translations of this "level" and keys that lead to another namespace
        //      e.g. "class Translation { HashMap<String, String> tr; HashMap<String, Translation> sub; }
        // find a way to translate "main" only for main method

        File f = new File(Main.SOURCE_PATH, getClass().getPackageName() + File.separator + TRANSLATION_DIR);
        //System.out.println(f.getAbsolutePath() + " --- is dir: " + f.isDirectory());

        // TODO: go through directory of f and get all files (sort later which ones to load)
        String[] trFiles = null;
        Logger.warning("Übersetzungsdateien laden - Implementierung temporär ausgesetzt");
        if (trFiles == null) return;

        for (String file : trFiles) {
            InputStream is = getClass().getResourceAsStream(TRANSLATION_DIR + file);  // uses this package (convert) as root folder
            if (is == null) continue;

            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String s;
                while ((s = br.readLine()) != null) {
                    s = s.replaceAll("\\s", "");  // remove whitespaces within the line todo don't remove beginning to keep track of namespaces; remove beginning if # follows
                    if (!s.isEmpty() && !s.startsWith("#")) {
                        String[] s2 = s.split(";");

                        // add variations? todo
                        if (s2[1].startsWith("^") || s2[1].endsWith("-") || s2[1].endsWith("+")) {
                            ArrayList<String> s3 = new ArrayList<>();
                            if (s2[1].endsWith("-")) {
                                String[] sAdd = {"er", "e", "es"};
                                if (s2[1].endsWith("--"))
                                    sAdd = new String[]{sAdd[0], sAdd[1], sAdd[2], ""};

                                for (int i = 0; i < 3; i++) {
                                    if (s2[1].startsWith("^")) {
                                        s3.add(s2[1].substring(1, s2[1].length() - 1) + sAdd[i]);
                                        s3.add(s2[1].substring(1, 2).toUpperCase() + s2[1].substring(2, s2[1].length() - 1) + sAdd[i]);
                                    } else
                                        s3.add(s2[1].substring(0, s2[1].length() - (s2[1].endsWith("--")?2:1)) + sAdd[i]);
                                }
                            } else if (s2[1].startsWith("^")) {
                                s3.add(s2[1].substring(1));
                                s3.add(s2[1].substring(1, 2).toUpperCase() + s2[1].substring(2));
                            } else if (s2[1].endsWith("+")) {
                                s3.add(s2[1].substring(0, s2[1].length() - 1));
                                s3.add(s2[1].substring(0, s2[1].length() - 1) + (s2[1].endsWith("e+") ? "n" : "en"));
                            }
                            for (String s4 : s3)
                                oldTranslationHashMap.put(s4, s2[0]);
                        } else {
                            oldTranslationHashMap.put(s2[1], s2[0]);
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Fehler beim Lesen der Übersetzungs-Datei: %s", file);
            }
        }
        Logger.log("Übersetzungen geladen.");
    }
}
