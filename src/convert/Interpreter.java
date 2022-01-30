package convert;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

import main.Logger;

public class Interpreter {

    private final String TRANSLATION_DIR = "translation/";
    private final String[] trFiles = {
            "0_main_translation.txt",
            "1_java_lang.txt",
            "2_javax_swing.txt"
    };
    private HashMap<String, String> translation;


    private String read(File file) {
        try {
            return Files.lines(file.toPath()).collect(Collectors.joining("\n"));
        } catch (IOException e) {
            Logger.log("Djava-Datei konnte nicht gelesen werden: " + e.getMessage());
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

    public boolean makeJavaFiles(File ... files) {
        boolean allSuccess = true;
        for (File f : files) {
            String fName = f.getName();
            f = new File(f.getParentFile().getAbsoluteFile(), fName.substring(0, fName.length() - 5) + "java");  // replace "djava" with "java"; todo better way?
            try {
                if (f.exists())
                    if (!f.delete())
                        throw new IOException();
                if (!f.createNewFile())
                    throw new IOException();

                String djavaCode = read(f);
                String javaCode = replace(djavaCode);

                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(javaCode);
                bw.close();
            } catch (IOException e) {
                Logger.log("Java-Datei konnte nicht erstellt werden: " + e.getMessage());
                allSuccess = false;
            }
        }
        Logger.log("Java-Datei erstellt");
        return allSuccess;
    }

    public boolean loadTranslation() {
        translation = new HashMap<>();

        //todo:
        // make "sub-translations" for namespaces (marked by 4 space indent in txt files), so that no conflicts happen
        //      idea: class that has direct translations of this "level" and keys that lead to another namespace
        //      e.g. "class Translation { HashMap<String, String> tr; HashMap<String, Translation> sub; }
        // find a way to translate "main" only for main method

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
                Logger.log("Fehler beim Lesen der Übersetzungs-Datei: " + file);
                return false;
            }
        }
        Logger.log("Übersetzungen geladen");
        return true;
    }
}
