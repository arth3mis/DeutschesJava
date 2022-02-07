package convert;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import convert.translation.TextChain;
import filesystem.Filer;
import main.Logger;
import main.Main;

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

    private Translation rootTranslation;
    private final File[] files;
    private HashMap<File, TextChain> fileChains = new HashMap<>();

    // old version
    private HashMap<String, String> oldTranslationHashMap;


    public Converter(File[] djavaFiles) {
        files = djavaFiles;

        HashSet<String> neededPackages = new HashSet<>();

        // Loop through every dJava File and create the chains
        for (File dJavaFile : djavaFiles) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(dJavaFile));

                // Communicate Debugging
                System.out.println("\n\nStarte Lesen...\n");
                TextChain textChainStart = new TextChain.Generator(br).generate();


                textChainStart.print();
                System.out.println("\n");

            } catch (IOException e) {
                Logger.error("Fehler beim Lesen der Djava-Datei: %s", dJavaFile.getAbsolutePath());
            }
        }



        /*
        HashMap<String, File> translationFiles = new HashMap<>();
        File translationFolder = new File(Main.SOURCE_PATH, getClass().getPackageName() + File.separator + TRANSLATION_DIR);
        //System.out.println(translationFolder.getAbsolutePath() + " --- is dir: " + translationFolder.isDirectory());

        for (File translationFile : translationFolder.listFiles()) {

        }
        */




        // setup imports in all translations
            // check which translation files are needed
            // store in "rootTranslation" object
    }

    //TODO: see above
    int createChain(int i, BufferedReader bf, TextChain textChain) {
        return 0;
    }




    /**
     * called by Main class to start the translation
     * @return nothing hehe
     */
    public void translateToJavaFiles() {
        for (int i = 0; i < files.length; i++) {
            // TODO if first file (=main class) fails, set flag: Main.mainFileIntact = false;
            //files[i] = Filer.refactorExtension(translateToJavaFile(files[i]), Main.JAVA_EXTENSION);;
        }
    }

    private File translateToJavaFile(File djavaFile) {

        // Setup text chain
            // setup sub chains on every ()

        // Translate Text chains and put it into StringBuilder

        return null;
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

    public void loadTranslation() {
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
