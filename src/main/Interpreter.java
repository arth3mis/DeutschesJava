package main;

import javax.tools.*;
import java.awt.*;
import java.io.*;
import java.util.*;

public class Interpreter {

    public static final String STD_FILE_EXTENSION = ".djava";
    public static final String J_FILE_EXTENSION = ".java";

    public static final String translationDir = "translation/";

    public static final String SRC_1_DIR = "djava/";
    public static final String SRC_2_DIR = "java/";
    public static final String OUT_DIR = "out/";

    static String projectDir = "example_saves/example_project/";
    static String fileName = "Example_class";

    static StringBuilder fileIn;
    static StringBuilder fileOut;

    static HashMap<String, String> translation;

    public static void main(String[] args) throws IOException {
        loadTranslation();

        read();
        replace();

        makeJavaFile();

        File file1 = new File(projectDir + SRC_2_DIR + fileName + J_FILE_EXTENSION);

        /*
            Iterator i = translation.entrySet().iterator();
            while (i.hasNext()) {
                HashMap.Entry he = (HashMap.Entry) i.next();
                System.out.println(he.getKey() + " = " + he.getValue());
            }
        */
        //System.out.println(translation.get("neu"));

        /*
            Graphics2D g2 = (Graphics2D) g;
            g2.drawString("this is something I want people to <p color=\"#00FF00\">NOTICE</p>", x, y);
        */

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        ArrayList<File> af = new ArrayList<>();
        af.addAll(Arrays.asList(new File(projectDir + OUT_DIR)));
        fileManager.setLocation(StandardLocation.CLASS_OUTPUT, af);

        Iterable<? extends JavaFileObject> compilationUnits1 =
                fileManager.getJavaFileObjects(file1);
        compiler.getTask(null, fileManager, null, null, null, compilationUnits1).call();

        try {
            String absolutePath = new File(projectDir + OUT_DIR).getAbsolutePath();
            String command = "java -cp \"" + absolutePath + "\" " + fileName;
            System.out.println(command);

            Process prc = Runtime.getRuntime().exec(command);
        } catch (IOException e) {
            System.out.println("run error");
        }
    }

    static void read() {
        try (BufferedReader br = new BufferedReader(new FileReader(projectDir + SRC_1_DIR + fileName + STD_FILE_EXTENSION))) {
            fileIn = new StringBuilder();
            Iterator i = br.lines().iterator();
            while (i.hasNext())
                fileIn.append(i.next()).append("\n");
        } catch (IOException e) {
            System.out.println("ERROR while reading djava file!");
        }
    }

    static void replace() {
        fileOut = new StringBuilder(fileIn.toString());
        Iterator i = translation.entrySet().iterator();
        while (i.hasNext()) {
            HashMap.Entry keyword = (HashMap.Entry) i.next();
            int ind = fileOut.indexOf((String) keyword.getKey());
            for (; ind != -1; ind = fileOut.indexOf((String) keyword.getKey(), ind+1)) {
                fileOut.replace(ind, ind+((String) keyword.getKey()).length(), (String) keyword.getValue());
            }
        }
        //System.out.println(fileOut.toString());
    }

    static void makeJavaFile() {
        File f = new File(projectDir + SRC_2_DIR + fileName + J_FILE_EXTENSION);
        try {
            if (f.exists())
                if (!f.delete())
                    throw new IOException();
            if (!f.createNewFile())
                throw new IOException();
            BufferedWriter bw = new BufferedWriter(new FileWriter(f));
            bw.write(fileOut.toString());
            bw.close();
        } catch (IOException e) {
            System.out.println("ERROR on java file creation!");
        }
    }

    static void loadTranslation() {         //TODO: String content needs to be excluded!
        translation = new HashMap<>();

        File f = new File(translationDir);
        if (!f.exists()) {
            System.out.println("CRITICAL ERROR: Translation directory \""+f.toString()+"\" does not exist!");
            return;
        }
        String[] subFNames = f.list();
        if (subFNames == null) {
            System.out.println("CRITICAL WARNING: subFNames is NULL, no translations will be loaded!");
            return;
        }
        for (String s : subFNames) {
            System.out.println(s);
        }

        for (String subFName : subFNames) {
            try (BufferedReader br = new BufferedReader(new FileReader(translationDir + subFName))) {
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
                                        System.out.println("here");
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
                System.out.println("ERROR while reading translation file: " + subFName);
            }
        }
        System.out.println();
    }
}
