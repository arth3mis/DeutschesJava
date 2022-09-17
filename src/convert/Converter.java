package convert;

import convert.translation.TranslationFolder;
import filesystem.Filer;
import main.Logger;
import main.Main;
import org.jetbrains.annotations.NotNull;

import java.io.*;
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

    private static final String TRANSLATION_EXT = ".txt";
    private static final String[] TRANSLATION_FILES = {
            "0_java.lang",
            "0_main_translation",
            "java",
            "java.util.function",
            "java.util",
            "javax.swing",
            "javax",
    };

    private Translation rootTranslation = new Translation();
    private Translation packageContext;
    private final File[] files;
    private HashMap<File, TextChain> fileChains = new HashMap<>();

    // old version
    private HashMap<String, String> oldTranslationHashMap;


    public Converter(File[] djavaFiles) {
        files = djavaFiles;

        // Loop through every dJava File and create text chains
        Logger.log("%s-Dateien in Sinneinheiten unterteilen...", Main.LANGUAGE_NAME);
        for (File djavaFile : djavaFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(djavaFile));
                TextChain tc = new TextChain.Generator(br).generate();
                fileChains.put(djavaFile, tc);

                fileChains.get(djavaFile).print();
            } catch (IOException e) {
                Logger.error("Fehler beim Lesen der %s-Datei: %s", Main.LANGUAGE_NAME, djavaFile.getAbsolutePath());
                // main file lost?
                if (fileChains.isEmpty())
                    Main.mainFileIntact = false;
            }
        }
    }

    /**
     * called by Main class to start the translation
     */
    public void translateToJavaFiles() {
        loadTranslationFiles();


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
     * @return translated java files, null for unsuccessful translation
     */
    public File[] getFiles() {
        return files;
    }


    /**
     * checks djava TextChains for package names and loads respective files
     * (package names in chains are translated in the process)
     */
    private void loadTranslationFiles() {
        Logger.log("Übersetzungsdateien laden...");

        HashMap<String, Boolean> packageTranslationFiles = new HashMap<>();  // name, hasBeenLoaded
        // Import import files
        // Note (Arthur): using File objects and dynamic translation file searching doesn't work in jar file
        for (String trFileName : TRANSLATION_FILES) {
            if (trFileName.startsWith("0_")) {
                // Add to Root translation
                loadPackageTranslationFile(trFileName, rootTranslation);
            } else {
                // Note the Package Name
                packageTranslationFiles.put(trFileName, false);
            }
        }

        // if there are no top-level package names specified (such as java/javax), exit method here
        if (rootTranslation.getPackageTranslations() == null)
            return;

        // collect all tokens that are top-level package names
        String[] packages = rootTranslation.getPackageTranslations().keySet().toArray(new String[0]);
        TextChain[] imports = fileChains.values().stream()
                .flatMap(t -> t.findAllInChain(packages).stream())
                .toArray(TextChain[]::new);

        // print all found package references
        /*Arrays.stream(imports).forEach(t -> {
            TextChain t1 = t, t2 = t.find(";");
            String sb = "";
            while (t1 != t2) {
                sb += t1.germanWord;
                t1 = t1.nextChain;
            }
            Logger.debug(sb);
        });*/

        // for each import, load all needed package translations
        // note: an object named "djava" calling a field (e.g. "djava.sub.value = 2;") has the same syntax.
        //       Thus, a warning for "wrong import statements" or similar is not easy to implement
        Logger.log("Benötigte Pakete hinzufügen...");
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
                        // package does not exist? (cause: reached class identifier, missing translation, was no import in the first place, ...)
                        if (!packageTranslationFiles.containsKey(s))
                            break;
                        // package file not already loaded?
                        if (!packageTranslationFiles.get(s)) {
                            loadPackageTranslationFile(s, currentPackage);
                            packageTranslationFiles.put(s, true);
                        }
                    }
                }
                t = t.nextChain;
            }
        }

        Logger.log("Übersetzungsdateien geladen.");
    }

    /**
     * loads translation txt file
     * @param trFileName file name: no extension, must not be a path (see {@link TranslationFolder})
     * @param packageContext reference for sub-package translations
     */
    private void loadPackageTranslationFile(String trFileName, Translation packageContext) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                TranslationFolder.class.getResourceAsStream(trFileName + TRANSLATION_EXT)
        )))) {
            this.packageContext = packageContext;
            readPackageTranslationFile(br, rootTranslation);
            this.packageContext = rootTranslation;
        } catch (IOException | NullPointerException e) {
            Logger.error("Fehler beim Lesen der Übersetzungsdatei: %s", trFileName);
        }
    }

    /** recursive translation file loading (don't call directly!) */
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
}
