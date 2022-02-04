package convert;

import java.io.*;
import java.nio.file.Files;
import java.util.*;

import filesystem.Filer;
import main.Logger;
import main.Main;

public class Converter {

    /*

    NOTIZ für JANNIS: Commits haben Versionen, die werden stumpf durchnummeriert, siehe bisherige commits und jar-Dateien

     */

    private static final String TRANSLATION_DIR = "translation";
    private static Translation baseTranslation;

    private final File djavaFile;
    // old version
    private HashMap<String, String> translation;


    public Converter(File djavaFile) {
        this.djavaFile = djavaFile;
        // copy static base translation
        // read file
        // check which translation files are needed
        // load translation
    }

    /**
     * called by Main class to retrieve java file
     * @return translated java file, null for unsuccessful translation
     */
    public File translateToJava() {
        return Filer.refactorExtension(djavaFile, Main.JAVA_EXTENSION);
        //return null;
    }

    private String read(File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            Logger.error("Djava-Datei konnte nicht gelesen werden: " + e.getMessage());
            return null;
        }
    }

    private String replace(String djavaCode) {
        StringBuilder code = new StringBuilder(djavaCode);
        
        //TODO:
        // use String instead of StringBuilder? Or some array (or make a class like 'ReplaceCode') that splits code into parts that need replacing and parts that don't (to keep order)
        // String content needs to be excluded

        for (Map.Entry<String, String> entry : translation.entrySet()) {
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
                Logger.error("Java-Datei " + javaFile + " konnte nicht erstellt werden: " + e.getMessage());
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
        translation = new HashMap<>();

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
                                translation.put(s4, s2[0]);
                        } else {
                            translation.put(s2[1], s2[0]);
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Fehler beim Lesen der Übersetzungs-Datei: " + file);
            }
        }
        Logger.log("Übersetzungen geladen.");
    }
}
