package main;

import javax.tools.*;
import java.io.*;
import java.util.*;
import cfr.Runner;

public class Interpreter {

    private static final String translationDir = "translation/";

    private static final String[] trFiles = {
            "0_main_translation.txt",
            "1_java_lang.txt",
            "2_javax_swing.txt"
    };

    static StringBuilder fileIn;
    static StringBuilder fileOut;

    static HashMap<String, String> translation;

    static String compile(String... filePaths) {
        try {
            File[] files = new File[filePaths.length];
            for (int i = 0; i < files.length; i++) {
                String fn = new File(filePaths[i]).getName();
                files[i] = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + "java");
            }

            if (ToolProvider.getSystemJavaCompiler() != null) {
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                Main.log("system compiler found");
                //Main.log("compiler: " + compiler.toString());
                StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
                //Main.log("fileManager: " + fileManager.toString());

                ArrayList<File> af = new ArrayList<>();
                af.addAll(Arrays.asList(files[0].getParentFile()));
                fileManager.setLocation(StandardLocation.CLASS_OUTPUT, af);

                Iterable<? extends JavaFileObject> compilationUnits1 = fileManager.getJavaFileObjects(files);

                if (compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call())
                    return "compilation successful";
                else
                    return "compilation failed";
            } else {
                Main.log("no system compiler found, trying manual compilation with command");
                // compile by command
                try {
                    StringBuilder s = new StringBuilder();
                    for (int i = 0; i < files.length; i++) {
                        s.append(" \"").append(files[i].toString()).append("\"");
                    }
                    //Main.log(s.toString());
                    Process p = Runtime.getRuntime().exec("javac"+s.toString(), null, new File(System.getProperty("user.dir")));
                    while (p.isAlive());
                    return "command compilation ended with value: " + p.exitValue();
                } catch (IOException e) {
                    return "command compilation failed: " + e.getMessage();
                }
            }
        } catch (IOException e) {
            return "compilation failed: " + e.getMessage();
        } catch (NullPointerException e) {
            return "null value during compilation";
        }
    }

    static String run(String mainFilePath) {
        String s = mainFilePath.substring(0, mainFilePath.length() - 5) + "class";
        String l = "running Runner.start with args: \"" + s + "\"\n";
        return (l + "runner returned: " + Runner.start(new String[]{s}));
    }

    private static void read(String filePath) {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            fileIn = new StringBuilder();
            Iterator i = br.lines().iterator();
            while (i.hasNext())
                fileIn.append(i.next()).append("\n");
        } catch (IOException e) {
            System.out.println("djava file could not be read: " + e.getMessage());
        }
    }

    private static void replace() {
        fileOut = new StringBuilder(fileIn.toString());
        Iterator i = translation.entrySet().iterator();
        while (i.hasNext()) {
            HashMap.Entry keyword = (HashMap.Entry) i.next();
            int ind = fileOut.indexOf((String) keyword.getKey());
            for (; ind != -1; ind = fileOut.indexOf((String) keyword.getKey(), ind+1)) {
                fileOut.replace(ind, ind+((String) keyword.getKey()).length(), (String) keyword.getValue());
            }
        }
    }

    static String makeJavaFile(String... filePaths) {
        for (int i = 0; i < filePaths.length; i++) {
            String fn = new File(filePaths[i]).getName();
            File f = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + "java");
            try {
                if (f.exists())
                    if (!f.delete())
                        throw new IOException();
                if (!f.createNewFile())
                    throw new IOException();

                read(filePaths[i]);
                replace();

                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                bw.write(fileOut.toString());
                bw.close();
            } catch (IOException e) {
                return "java file could not be created: " + e.getMessage();
            }
        }
        return "created java file";
    }

    static String deleteJavaFile(String... filePaths) {
        String s = "java files deleted, except: ";
        for (int i = 0; i < filePaths.length; i++) {
            String fn = new File(filePaths[i]).getName();
            File f = new File(new File(filePaths[i]).getParentFile().getAbsoluteFile(), fn.substring(0, fn.length()-5) + "java");
            if (!f.delete())
                s += ""+i;
        }
        return s;
    }

    static String loadTranslation() {         //TODO: String content needs to be excluded!
        translation = new HashMap<>();

        for (String file : trFiles) {
            InputStream is = Main.class.getResourceAsStream(translationDir + file);
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String s;
                while ((s = br.readLine()) != null) {
                    s = s.replaceAll(" ", "");
                    if (!s.isEmpty() && !s.startsWith("#")) {
                        String[] s2 = s.split(";");

                        if (s2[0].startsWith("^") || s2[0].endsWith("-") || s2[0].endsWith("+")) {
                            ArrayList<String> s3 = new ArrayList<>();
                            if (s2[0].endsWith("-")) {
                                String[] sAdd = {"er", "e", "es"};
                                if (s2[0].endsWith("--"))
                                    sAdd = new String[]{sAdd[0], sAdd[1], sAdd[2], ""};

                                for (int i = 0; i < 3; i++) {
                                    if (s2[0].startsWith("^")) {
                                        s3.add(s2[0].substring(1, s2[0].length() - 1) + sAdd[i]);
                                        s3.add(s2[0].substring(1, 2).toUpperCase() + s2[0].substring(2, s2[0].length() - 1) + sAdd[i]);
                                    } else
                                        s3.add(s2[0].substring(0, s2[0].length() - (s2[0].endsWith("--")?2:1)) + sAdd[i]);
                                }
                            } else if (s2[0].startsWith("^")) {
                                s3.add(s2[0].substring(1));
                                s3.add(s2[0].substring(1, 2).toUpperCase() + s2[0].substring(2));
                            } else if (s2[0].endsWith("+")) {
                                s3.add(s2[0].substring(0, s2[0].length() - 1));
                                s3.add(s2[0].substring(0, s2[0].length() - 1) + (s2[0].endsWith("e+") ? "n" : "en"));
                            }
                            for (String s4 : s3)
                                translation.put(s4, s2[1]);
                        } else {
                            translation.put(s2[0], s2[1]);
                        }
                    }
                }
            } catch (IOException e) {
                return "ERROR while reading translation file: " + file;
            }
        }
        return "translations loaded";
    }
}
