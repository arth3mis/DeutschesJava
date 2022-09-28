package convert.translation;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * path reference class to detect txt files when inside a jar archive.
 * Must be located in the same folder as the translation txt files
 */
public class TranslationFolder {

    public static final String TRANSLATION_EXT = ".txt";
    public static final String[] TRANSLATION_FILES = {         // TODO always add new txt files!
            "0_main_translation",
            "java.io",
            "java.lang",
            "java",
            "java.util.function",
            "java.util.random",
            "java.util.stream",
            "java.util",
            "javax.swing",
            "javax",
    };


    /** searches txt files. <ul><li>args[0]: search term<li>args[1]: ignore case</ul> */
    public static void main(String[] args) {
        // insert search term here
        String searchTerm = "current";



        boolean ignoreCase = true;

        if (args.length >= 2) {
            searchTerm = args[0];
            ignoreCase = Boolean.parseBoolean(args[1]);
        }

        String replaceTerm = null;  // todo implement rewrite txt from 'lines' list
        boolean confirmEachReplace = true;

        // confirm replace option
        if (replaceTerm != null) {
            if (JOptionPane.showConfirmDialog(null, "replace is active, is this really wanted?") != JOptionPane.YES_OPTION)
                System.exit(0);
        }

        Pattern rgx = Pattern.compile("("+searchTerm+")", ignoreCase ? Pattern.CASE_INSENSITIVE : 0);

        // find all occurences in txt files
        File fo = new File("src/convert/translation");
        File[] txts = fo.listFiles((dir, name) -> name.endsWith(TRANSLATION_EXT));
        BufferedReader[] brs;
        // in jar?
        if (txts == null || txts.length == 0) {
            txts = Arrays.stream(TRANSLATION_FILES)
                    .map(s -> s += TRANSLATION_EXT)
                    .map(File::new)
                    .toArray(File[]::new);
            brs = Arrays.stream(txts)
                    .map(File::getName)
                    .map(TranslationFolder.class::getResourceAsStream)
                    .filter(Objects::nonNull)
                    .map(InputStreamReader::new)
                    .map(BufferedReader::new)
                    .toArray(BufferedReader[]::new);
        }
        // in IDE?
        else {
                brs = Arrays.stream(txts)
                        .map(f -> {
                            try {
                                return new FileReader(f);
                            } catch (FileNotFoundException e) {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .map(BufferedReader::new)
                        .toArray(BufferedReader[]::new);
        }

        for (int i = 0; i < brs.length; i++) {
            boolean found = false;
            List<String> lines = new ArrayList<>();
            Stack<String> context = new Stack<>();  // todo print all ->name<-{ search_hit; ... }, in english is probably better
            try {
                int ln = 0;
                String s;
                while ((s = brs[i].readLine()) != null) {
                    lines.add(s);
                    ln++;
                    if (!s.startsWith("#") && rgx.matcher(s).find()) {
                        if (!found) {
                            String file = txts[i].getName().replace("0_", "");
                            System.out.printf("\n\n%s %s\n\n", file, "-".repeat(70 - file.length()));
                            found = true;
                        }
                        System.out.printf("Z.%3d: '%s'\n", ln, rgx.matcher(s).replaceAll("[$1]"));
                        if (replaceTerm != null) {
                            int c = confirmEachReplace ? JOptionPane.showConfirmDialog(null, "replace in this line?") : JOptionPane.YES_OPTION;
                            if (c == JOptionPane.CANCEL_OPTION)
                                replaceTerm = null;
                            else if (c == JOptionPane.YES_OPTION) {
                                lines.remove(lines.size() - 1);
                                lines.add(rgx.matcher(s).replaceAll(replaceTerm));
                            }
                        }
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
