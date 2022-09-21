package convert;

import convert.translation.TranslationFolder;
import filesystem.Filer;
import main.Logger;
import main.Main;

import java.io.*;
import java.util.*;

public class Converter {

    public static final String TRANSLATION_EXT = ".txt";
    public static final String[] TRANSLATION_FILES = {         // TODO always add new txt files!
            "0_java.lang",
            "0_main_translation",
            "java",
            "java.util.function",
            "java.util.stream",
            "java.util",
            "javax.swing",
            "javax",
    };

    private final Translation rootTranslation = new Translation();
    private Translation packageContext;
    private String[] importNames, staticNames;
    private final HashMap<String, Translation> staticGivers = new HashMap<>();
    private final HashMap<String, Translation> staticSearchers = new HashMap<>();

    private File[] files;
    private final HashMap<File, TextChain> fileChains = new HashMap<>();


    public Converter(File[] djavaFiles) {
        files = djavaFiles;

        // Loop through every dJava File and create text chains
        Logger.log("%s-Dateien in Sinneinheiten unterteilen...", Main.LANGUAGE_NAME);
        for (File djavaFile : djavaFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(djavaFile));
                TextChain tc = new TextChain.Generator(br).generate();
                fileChains.put(djavaFile, tc);
//                fileChains.get(djavaFile).print();
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
        Logger.log("Übersetzungsdateien laden...");
        loadTranslationFiles();
        Logger.log("Übersetzungsdateien geladen.");

        fileChains.values().forEach(this::translateToJavaFile);
        Logger.log("Übersetzung abgeschlossen.");

        File[] javaFiles = Filer.refactorExtension(files, Main.JAVA_EXTENSION);
        // create and write files
        for (int i = 0; i < files.length; i++) {
            try {
                if (!javaFiles[i].exists() && !javaFiles[i].createNewFile())
                    throw new IOException("");
                BufferedWriter bw = new BufferedWriter(new FileWriter(javaFiles[i]));
                bw.write(fileChains.get(files[i]).collectTranslation().toString());
                bw.close();
            } catch (IOException e) {
                Logger.error("Fehler beim Erstellen der Datei %s: %s", javaFiles[i].getAbsolutePath(), e.getMessage());
            }
        }
        files = javaFiles;
        Logger.log("Umwandlung abgeschlossen.");
    }

    private void translateToJavaFile(TextChain tc) {
        // go through every chain link
        Stack<TextChain> parent = new Stack<>();
        Translation lastWord = null;
        boolean searchStatic = false;

        while (tc != null) {
            if (tc.isAccessOrMethodRef()) {
                searchStatic = true;
            } else if (tc.translation == null) {
                // check static translations first
                if (searchStatic && lastWord != null) {
                    Translation t = lastWord.getStaticTranslations().get(tc.getGermanWord());
                    if (t != null) {
                        tc.translate(t);
                        lastWord = t;
                    }
                }
                // not translated? check root translation
                if (tc.translation == null) {
                    Translation t = rootTranslation.getStaticTranslations().get(tc.getGermanWord());
                    if (t != null) {
                        tc.translate(t);
                        lastWord = t;
                    }
                }
            } else {  // translation exists
                lastWord = tc.translation;
            }
            if (tc.subChain != null) {
                parent.push(tc);
                tc = tc.subChain;
            } else if (tc.nextChain == null && !parent.empty()) {
                tc = parent.pop().nextChain;
            } else {
                tc = tc.nextChain;
            }
        }
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

        // TODO all pointless? see x.djava implicit example -> need all package translations
        //      -> not all pointless, see static import resolving below

        // collect all tokens that are top-level package names
        String[] packages = rootTranslation.getPackageTranslations().keySet().toArray(new String[0]);
        TextChain[] imports = fileChains.values().stream()
                .flatMap(t -> t.findAllInChain(packages).stream())
                .toArray(TextChain[]::new);

        // for each import, load all needed package translations
        // note: an object named "djava" calling a field (e.g. "djava.sub.value = 2;") has the same syntax.
        //       Thus, a warning for "wrong import statements" or similar is not easy to implement
        for (TextChain tc : imports) {
            StringBuilder packageName = new StringBuilder();
            Translation currentPackage = rootTranslation;

            // check if it is really an import (not e.g. "djava = 42;")
            if (tc.find(new String[]{".",";"}).germanWord.equals(";"))
                continue;

            // check and load sub-packages
            while (tc != null) {
                if (tc.germanWord.equals(";"))
                    break;
                else if (tc.germanWord.equals(".")) {
                    packageName.append(".");
                } else if (!tc.isWhitespace()) {
                    Map<String, Translation> pt = currentPackage.getPackageTranslations();
                    // no sub-packages available?
                    if (pt == null)
                        break;

                    // add translation and load package
                    else if (pt.containsKey(tc.getGermanWord())) {
                        currentPackage = pt.get(tc.getGermanWord());
                        tc.translate(currentPackage);
                        packageName.append(tc.translation.getTranslationText());
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
                tc = tc.nextChain;
            }
        }

        // load all other files because of implicit access to other types (see above and x.djava HashMap)
        packageTranslationFiles.forEach((s, b) -> {
            if (!b) loadPackageTranslationFile(s, rootTranslation);
        });

        // copy static translations to extending classes
        staticSearchers.forEach((s, tr) -> {
            if (staticGivers.containsKey(s)) {
                tr.getStaticTranslations().putAll(staticGivers.get(s).getStaticTranslations());
            }
        });

        // collect all "import static" statements
        TextChain[] staticImports = fileChains.values().stream()
                .flatMap(t -> t.findAllInChain(importNames).stream())
                // check static keyword
                .filter(t -> {
                    while (t.nextChain.isWhitespace())
                        t = t.nextChain;
                    return t.nextChain.find(staticNames) == t.nextChain;
                })
                // move to package name
                .map(t -> t.find(packages, new String[]{";"}))
                // if no valid package name was found
                .filter(Objects::nonNull)
                .toArray(TextChain[]::new);

        // add needed static imports to root translation        TODO per-file translation? (is it really worth the hassle?)
        // for the following context search, encapsulated package translations are good
        for (TextChain tc : staticImports) {
            String lastKey = "";
            Translation context = rootTranslation;
            boolean packageIteration = true, translationFound = true;

            while (tc != null && context != null) {
                if (tc.germanWord.equals(";")) {
                    if (translationFound)
                        rootTranslation.getStaticTranslations().put(lastKey, context);
                    break;
                } else if (tc.germanWord.equals("*")) {
                    // add all static items to root translation
                    context.getStaticTranslations().forEach(rootTranslation.getStaticTranslations()::put);
                } else if (!tc.germanWord.equals(".") && !tc.isWhitespace()) {
                    if (packageIteration) {
                        Translation t = context.getPackageTranslations() != null ? context.getPackageTranslations().get(tc.getGermanWord()) : null;
                        if (t != null)
                            context = t;
                        else {
                            packageIteration = false;
                            // get class/... translation from root
                            lastKey = tc.getGermanWord();
                            context = rootTranslation.getStaticTranslations().get(tc.getGermanWord());
                        }
                    } else {
                        Translation t = context.getStaticTranslations().get(tc.getGermanWord());
                        if (t != null) {
                            lastKey = tc.getGermanWord();
                            context = t;
                        } else
                            translationFound = false;
                    }
                }
                tc = tc.nextChain;
            }
        }
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
            Logger.error("Fehler beim Lesen der Übersetzungsdatei %s: %s", trFileName, e.getMessage());
        }
    }

    /** recursive translation file loading (don't call directly!) */
    private void readPackageTranslationFile(BufferedReader br, Translation staticContextTranslation) throws IOException {
        String line;
        Translation lastTranslation = null;
        Map<String, Translation> newStaticGivers = new HashMap<>();

        // Read lines & Return if context goes up a layer
        while ((line = br.readLine()) != null) {
            line = line.replaceAll("\\s", "");
            if (line.isEmpty()) continue;
            if (line.startsWith("#")) continue;
            if (line.startsWith("}")) return;
            if (line.startsWith("{")) {
                if (lastTranslation == null) throw new NullPointerException();
                readPackageTranslationFile(br, lastTranslation);
                continue;
            }
            if (line.startsWith(":") || line.startsWith("<")) {  // extends/implements
                if (lastTranslation == null) throw new NullPointerException();
                // mark for copying static translations
                final Translation t = lastTranslation;
                Arrays.stream(line.substring(1).split(",")).forEach(s -> staticSearchers.put(s, t));
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

            // Add translation if in file
            if (line.contains(";")) {
                String[] splitLine = line.split(";");
                Translation value = new Translation(splitLine[0]);

                Arrays.stream(splitLine[1].split(",")).forEach(key -> context.put(key, value));

                // save translations for "import" and "static"
                if (value.getTranslationText().equals("import"))
                    importNames = splitLine[1].split(",");
                else if (value.getTranslationText().equals("static"))
                    staticNames = splitLine[1].split(",");
                // save for copying static translations
                newStaticGivers.put(value.getTranslationText(), value);

                lastTranslation = value;
            } else {
                lastTranslation = new Translation(line);
                context.put(line, lastTranslation);
            }
        }

        // add only contexts that have static translation
        newStaticGivers.entrySet().parallelStream()
                .filter(e -> !e.getValue().getStaticTranslations().isEmpty())
                .forEach(e -> staticGivers.put(e.getKey(), e.getValue()));
    }
}
