package convert;

import convert.translation.TranslationFolder;
import filesystem.Filer;
import main.Logger;
import main.Main;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static convert.translation.TranslationFolder.*;

public class Converter {

    private final Translation rootTranslation = new Translation();
    private String[] importNames, staticNames;
    private final HashMap<String, Translation> staticGivers = new HashMap<>();
    private final HashMap<String, List<Translation>> staticSearchers = new HashMap<>();

    private File[] files;
    private final HashMap<File, TextChain> fileChains = new HashMap<>();


    public Converter(File[] djavaFiles) {
        files = djavaFiles;

        if (djavaFiles != null && djavaFiles.length > 0)
            Logger.log("%s-Dateien in Sinneinheiten unterteilen...", Main.LANGUAGE_NAME);
        else return;

        // Loop through every dJava File and create text chains
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

    private void pass() {}

    private void translateToJavaFile(TextChain tc) {
        // go through every chain link
        Stack<TextChain> parent = new Stack<>();
        Translation lastWord = null;
        boolean searchStatic = false, searchPackage = false;

        while (tc != null) {
            if (tc.isWhitespace()) {
                pass();
            } else if (tc.isAccessOrMethodRef()) {
                searchStatic = true;
            } else if (tc.translation == null) {
                // check package translations
                if (searchPackage) {
                    if (lastWord.getPackageTranslations() == null) {
                        searchPackage = false;
                    } else {
                        Translation t = lastWord.getPackageTranslations().get(tc.getGermanWord());
                        if (t != null) {
                            tc.translate(t);
                            lastWord = t;
                        } else
                            searchPackage = false;
                    }
                }
                // check static translations first
                if (tc.translation == null && searchStatic && lastWord != null) {
                    Translation t = lastWord.getStaticTranslations().get(tc.getGermanWord());
                    if (t != null) {
                        tc.translate(t);
                        lastWord = t;
                    }
                }
                // not translated yet? check root translation
                if (tc.translation == null) {
                    Translation t = rootTranslation.getStaticTranslations().get(tc.getGermanWord());
                    // top-level package?
                    if (t == null && (t = rootTranslation.getPackageTranslations().get(tc.getGermanWord())) != null)
                        searchPackage = true;
                    if (t != null) {
                        tc.translate(t);
                        lastWord = t;
                    }
                }
                searchStatic = false;
            } else {  // translation already exists (should not be the case since package crawl was deprecated)
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
     * do not call in normal conversion procedure.
     * Warning: Only each first list element has static/package translations!
     */
    public Translation.Reverse loadAndGetReversedTranslation() {
        loadTranslationFiles();

        return Translation.Reverse.create(rootTranslation, null);
    }

    /**
     * checks djava TextChains for package names and loads respective files
     * (package names in chains are translated in the process)
     */
    private void loadTranslationFiles() {
//        HashMap<String, Boolean> packageTranslationFiles = new HashMap<>();  // name, hasBeenLoaded

        // Import import files
        // for package init, load files sorted by contained dots
        var files = Arrays.stream(TRANSLATION_FILES).sorted(
                Comparator.comparingInt(s -> s.split("\\.").length)).toList();
        for (String trFileName : files) {
            loadPackageTranslationFile(trFileName);
//            if (trFileName.startsWith("0_")) {
//                // Add to Root translation
//            } else {
//                // Note the Package Name
//                packageTranslationFiles.put(trFileName, false);
//            }
        }

        // if there are no top-level package names specified (such as java/javax), exit method here
        if (rootTranslation.getPackageTranslations() == null)
            return;

        // TODO all pointless? see x.djava implicit example -> need all package translations
        //      -> not all pointless, see static import resolving below

        // collect all tokens that are top-level package names
        String[] packages = rootTranslation.getPackageTranslations().keySet().toArray(new String[0]);
//        TextChain[] imports = fileChains.values().stream()
//                .flatMap(t -> t.findAllInChain(packages).stream())
//                .toArray(TextChain[]::new);
//
//        // for each import, load all needed package translations
//        // note: an object named "djava" calling a field (e.g. "djava.sub.value = 2;") has the same syntax.
//        //       Thus, a warning for "wrong import statements" or similar is not easy to implement
//        for (TextChain tc : imports) {
//            StringBuilder packageName = new StringBuilder();
//            Translation currentPackage = rootTranslation;
//
//            // check if it is really an import (not e.g. "djava = 42;")
//            if (tc.find(new String[]{".",";"}).germanWord.equals(";"))
//                continue;
//
//            // check and load sub-packages
//            while (tc != null) {
//                if (tc.germanWord.equals(";"))
//                    break;
//                else if (tc.germanWord.equals(".")) {
//                    packageName.append(".");
//                } else if (!tc.isWhitespace()) {
//                    Map<String, Translation> pt = currentPackage.getPackageTranslations();
//                    // no sub-packages available?
//                    if (pt == null)
//                        break;
//
//                    // add translation and load package
//                    else if (pt.containsKey(tc.getGermanWord())) {
//                        currentPackage = pt.get(tc.getGermanWord());
//                        tc.translate(currentPackage);
//                        packageName.append(tc.translation.getTranslationText());
//                        String s = packageName.toString();
//                        // package does not exist? (cause: reached class identifier, missing translation, was no import in the first place, ...)
//                        if (!packageTranslationFiles.containsKey(s))
//                            break;
//                        // package file not already loaded?
//                        if (!packageTranslationFiles.get(s)) {
//                            loadPackageTranslationFile(s);
//                            packageTranslationFiles.put(s, true);
//                        }
//                    }
//                }
//                tc = tc.nextChain;
//            }
//        }

        // load all other files because of implicit access to other types (see above and x.djava HashMap)
//        packageTranslationFiles.forEach((s, b) -> {
//            loadPackageTranslationFile(s);
//        });

        // copy static translations to extending classes
        //
        var conflicts = staticSearchers.values().stream()
                .flatMap(List::stream)
                .map(Translation::getTranslationText)
                .filter(staticSearchers::containsKey)
                .toList();
        Map<String, Integer> blocked = conflicts.stream().distinct().collect(Collectors
                .toMap(s -> s, s -> Collections.frequency(conflicts, s)));

        while (staticSearchers.values().stream().anyMatch(Objects::nonNull)) {
            for (var e : staticSearchers.entrySet()) {
                // skip if giver is also still a searcher (or value has been processed)
                if (blocked.get(e.getKey()) != null || e.getValue() == null)
                    continue;
                // copy if giver exists
                if (staticGivers.containsKey(e.getKey())) {
                    e.getValue().forEach(tr -> {
                        var st = staticGivers.get(e.getKey()).getStaticTranslations();
                        // note: containing itself is possible, see inner interfaces (like RandomGenerator.StreamableGenerator.StreamableGenerator.StreamableGenerator)
                        if (!st.isEmpty()) {
                            tr.getStaticTranslations().putAll(st);
                            // add to staticGivers for 2nd degree inheritance
                            staticGivers.putIfAbsent(tr.getTranslationText(), tr);
                        }
                    });
                }
                // remove all values from block list
                e.getValue().stream().map(Translation::getTranslationText).filter(blocked::containsKey).forEach(s -> {
                    blocked.put(s, blocked.get(s) - 1);
                    if (blocked.get(s) <= 0)
                        blocked.remove(s);
                });
                // mark as done
                e.setValue(null);
            }
        }

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

        // remove all template/placeholder keys (those with "++[name]")
        List<String> l = rootTranslation.getStaticTranslations().keySet().stream()
                .filter(s -> s.startsWith("++")).toList();
        for (String s : l)
            rootTranslation.getStaticTranslations().remove(s);
    }

    /**
     * loads translation txt file
     * @param trFileName file name: no extension, must not be a path (see {@link TranslationFolder})
     */
    private void loadPackageTranslationFile(String trFileName) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                TranslationFolder.class.getResourceAsStream(trFileName + TRANSLATION_EXT)
        )))) {
            // find package context
            String[] packs = trFileName.split("\\.");
            Translation packageContext = rootTranslation;
            for (String s : packs) {
                if (packageContext.getPackageTranslations() == null)
                    break;
                var tr = packageContext.getPackageTranslations().values().stream().filter(t -> t.getTranslationText().equals(s)).findAny();
                if (tr.isPresent())
                    packageContext = tr.get();
                else break;
            }
            // start reading
            readPackageTranslationFile(br, rootTranslation, packageContext);
        } catch (IOException | NullPointerException e) {
            Logger.error("Fehler beim Lesen der Übersetzungsdatei %s: %s", trFileName, e.getMessage());
        }
    }

    /** recursive translation file loading (don't call directly!) */
    private void readPackageTranslationFile(BufferedReader br, Translation staticContextTranslation, Translation packageContext) throws IOException {
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
                readPackageTranslationFile(br, lastTranslation, packageContext);
                continue;
            }
            if (line.startsWith(":") || line.startsWith("<") || line.startsWith("=")) {  // extends/implements/same methods as
                if (lastTranslation == null) throw new NullPointerException();
                // mark for copying static translations
                final Translation t = lastTranslation;
                Arrays.stream(line.substring(1).split(",")).forEach(s -> {
                    if (staticSearchers.containsKey(s))
                        staticSearchers.get(s).add(t);
                    else
                        staticSearchers.put(s, new ArrayList<>(List.of(t)));
                });
                continue;
            }

            // delete trailing comment
            line = line.substring(0, line.contains("#") ? line.indexOf("#") : line.length());

            Map<String, Translation> context;
            // Check if is static and delete $
            if (line.startsWith("$")) {
                context = staticContextTranslation.getStaticTranslations();
                line = line.substring(1);
            }
            // check if is package and delete _
            else if (line.startsWith("_")) {
                if (packageContext.getPackageTranslations() == null)
                    packageContext.initPackageTranslations();
                context = packageContext.getPackageTranslations();
                line = line.substring(1);
            } else
                context = rootTranslation.getStaticTranslations();

            // Add translation if in file
            Translation value;
            String[] german;
            if (line.contains(";")) {
                String[] splitLine = line.split(";");
                value = new Translation(splitLine[0]);
                german = splitLine[1].split(",");

                Arrays.stream(german).forEach(key -> context.put(key, value));

                // set priority to first translation (used in sublime completions)
                if (german.length > 1)
                    value.priorityKey = german[0];
            } else {
                // german and english are the same
                value = new Translation(line);
                german = new String[]{line};

                context.put(line, value);
            }
            // save for copying static translations
            newStaticGivers.put(value.getTranslationText(), value);

            // save translations for "import" and "static"
            if (value.getTranslationText().equals("import"))
                importNames = german;
            else if (value.getTranslationText().equals("static"))
                staticNames = german;

            lastTranslation = value;
        }

        // add only contexts that have static translation
        newStaticGivers.entrySet().parallelStream()
                .filter(e -> !e.getValue().getStaticTranslations().isEmpty())
                .forEach(e -> staticGivers.put(e.getKey(), e.getValue()));
    }
}
